
package com.energy.audit.service;

import com.energy.audit.model.Appliance;
import com.energy.audit.model.ElectricityBill;
import com.energy.audit.model.MonitoringRecord;
import com.energy.audit.repository.ApplianceRepository;
import com.energy.audit.repository.ElectricityBillRepository;
import com.energy.audit.repository.MonitoringRecordRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MonthlyEnergyReportService {

    private final ApplianceRepository applianceRepository;
    private final ElectricityBillRepository electricityBillRepository;
    private final MonitoringRecordRepository monitoringRecordRepository;

    @Value("${ecopulse.tariff-per-kwh:8.0}")
    private double tariffPerKwh;

    public MonthlyEnergyReportService(
            ApplianceRepository applianceRepository,
            ElectricityBillRepository electricityBillRepository,
            MonitoringRecordRepository monitoringRecordRepository) {

        this.applianceRepository = applianceRepository;
        this.electricityBillRepository = electricityBillRepository;
        this.monitoringRecordRepository = monitoringRecordRepository;
    }

    public MonthlyReport generateReport(YearMonth month, String selectedRoom) {

        Objects.requireNonNull(month, "Report month is required");

        String roomFilter = selectedRoom == null
                ? ""
                : selectedRoom.trim();

        List<Appliance> appliances = applianceRepository.findAll()
                .stream()
                .filter(a -> roomFilter.isEmpty()
                        || equalsIgnoreCase(a.getRoom(), roomFilter))
                .collect(Collectors.toList());

        List<ElectricityBill> bills = electricityBillRepository.findAll();

        List<MonitoringRecord> monitoringRecords =
                monitoringRecordRepository.findAll()
                        .stream()
                        .filter(r -> roomFilter.isEmpty()
                                || equalsIgnoreCase(r.getRoom(), roomFilter))
                        .collect(Collectors.toList());

        YearMonth previousMonth = month.minusMonths(1);

        /*
         * Select a bill whose billing-period end date falls
         * in the previous month.
         */
        ElectricityBill baselineBill = bills.stream()
                .filter(b -> b.getPeriodEnd() != null)
                .filter(b -> YearMonth.from(b.getPeriodEnd())
                        .equals(previousMonth))
                .max(Comparator.comparing(ElectricityBill::getPeriodEnd))
                .orElse(null);

        /*
         * Select a bill ending in the report month, if available.
         */
        ElectricityBill currentBill = bills.stream()
                .filter(b -> b.getPeriodEnd() != null)
                .filter(b -> YearMonth.from(b.getPeriodEnd())
                        .equals(month))
                .max(Comparator.comparing(ElectricityBill::getPeriodEnd))
                .orElse(null);

        MonthlyReport report = new MonthlyReport();

        report.month = month.toString();
        report.previousMonth = previousMonth.toString();
        report.room = roomFilter.isEmpty() ? "All Rooms" : roomFilter;
        report.tariffPerKwh = tariffPerKwh;

        report.applianceCount = appliances.size();
        report.monitoringRecordCount = monitoringRecords.size();

        /*
         * Baseline: actual bill when available.
         * Otherwise, estimate from appliance schedules.
         */
        if (baselineBill != null) {
            report.baselineKwh = baselineBill.getBilledKwh();
            report.baselineAmount = baselineBill.getBillAmount();
            report.baselineSource = "Actual saved electricity bill";
            report.baselineAvailable = true;
        } else {
            report.baselineKwh = appliances.stream()
                    .mapToDouble(Appliance::getMonthlyKwh)
                    .sum();

            report.baselineAmount =
                    report.baselineKwh * tariffPerKwh;

            report.baselineSource =
                    "Estimated from appliance schedules";

            report.baselineAvailable = !appliances.isEmpty();
        }

        /*
         * Monitoring interval energy is estimated from:
         * power (W) × interval duration (hours) / 1000.
         *
         * Intervals are clipped to the selected calendar month.
         */
        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        LocalDateTime monthEnd =
                month.plusMonths(1).atDay(1).atStartOfDay();

        List<ApplianceLine> lines = new ArrayList<>();

        double totalMonitoredKwh = 0.0;
        double totalWasteKwh = 0.0;

        for (MonitoringRecord record : monitoringRecords) {

            if (record.getDeviceOnStart() == null
                    || record.getDeviceOnEnd() == null
                    || record.getPowerWatts() <= 0) {
                continue;
            }

            LocalDateTime start = record.getDeviceOnStart();
            LocalDateTime end = record.getDeviceOnEnd();

            if (!end.isAfter(start)) {
                continue;
            }

            LocalDateTime clippedStart =
                    start.isAfter(monthStart) ? start : monthStart;

            LocalDateTime clippedEnd =
                    end.isBefore(monthEnd) ? end : monthEnd;

            if (!clippedEnd.isAfter(clippedStart)) {
                continue;
            }

            double hours = Duration.between(
                    clippedStart, clippedEnd
            ).toMinutes() / 60.0;

            double energyKwh =
                    record.getPowerWatts() * hours / 1000.0;

            /*
             * The model's existing waste calculation is used
             * when the recorded interval belongs to this month.
             *
             * If the interval crosses month boundaries, the
             * original whole-interval waste value is scaled
             * by the fraction falling in the selected month.
             */
            double fullHours = Duration.between(start, end)
                    .toMinutes() / 60.0;

            double fraction = fullHours > 0
                    ? Math.min(1.0, hours / fullHours)
                    : 0.0;

            double wasteKwh = Math.max(
                    0.0,
                    Math.min(
                            energyKwh,
                            record.getPotentialWasteKwh() * fraction
                    )
            );

            totalMonitoredKwh += energyKwh;
            totalWasteKwh += wasteKwh;

            ApplianceLine line = new ApplianceLine();
            line.applianceName = record.getApplianceName();
            line.room = record.getRoom();
            line.powerWatts = record.getPowerWatts();
            line.hours = hours;
            line.energyKwh = energyKwh;
            line.wasteKwh = wasteKwh;
            line.estimatedCost = energyKwh * tariffPerKwh;

            lines.add(line);
        }

        report.currentMonitoredKwh = totalMonitoredKwh;
        report.potentialWasteKwh = totalWasteKwh;

        report.estimatedOccupiedEnergyKwh = Math.max(
                0.0,
                totalMonitoredKwh - totalWasteKwh
        );

        report.currentMonitoredAmount =
                totalMonitoredKwh * tariffPerKwh;

        report.potentialWasteAmount =
                totalWasteKwh * tariffPerKwh;

        report.currentBillAvailable = currentBill != null;

        if (currentBill != null) {
            report.currentBillKwh = currentBill.getBilledKwh();
            report.currentBillAmount = currentBill.getBillAmount();
        }

        /*
         * Compare like with like:
         * - A current actual bill is compared with the baseline bill.
         * - Otherwise, monitored energy is shown separately.
         */
        if (baselineBill != null && currentBill != null) {
            report.comparisonKwh =
                    baselineBill.getBilledKwh()
                            - currentBill.getBilledKwh();

            report.comparisonAmount =
                    baselineBill.getBillAmount()
                            - currentBill.getBillAmount();

            report.comparisonLabel =
                    "Actual bill-to-bill comparison";

            report.comparisonAvailable = true;

        } else if (baselineBill == null && !appliances.isEmpty()
                && !monitoringRecords.isEmpty()) {

            report.comparisonKwh =
                    report.baselineKwh - report.currentMonitoredKwh;

            report.comparisonAmount =
                    report.comparisonKwh * tariffPerKwh;

            report.comparisonLabel =
                    "Estimated baseline versus monitored energy";

            report.comparisonAvailable = true;

        } else {
            report.comparisonLabel =
                    "Insufficient comparable data";

            report.comparisonAvailable = false;
        }

        report.applianceLines = lines;

        report.applianceLines.sort(
                Comparator.comparingDouble(
                        ApplianceLine::getWasteKwh
                ).reversed()
        );

        if (!report.baselineAvailable) {
            report.findings.add(
                    "No previous-month bill or appliance baseline is available."
            );
        }

        if (monitoringRecords.isEmpty()) {
            report.findings.add(
                    "No monitoring records match this room selection."
            );
        }

        if (!report.currentBillAvailable) {
            report.findings.add(
                    "No saved electricity bill ending in the selected month was found."
            );
        }

        if (report.potentialWasteKwh > 0) {
            report.findings.add(
                    String.format(
                            Locale.ROOT,
                            "Recorded intervals indicate %.2f kWh of potential energy wastage.",
                            report.potentialWasteKwh
                    )
            );
        }

        if (report.comparisonAvailable) {
            if (report.comparisonKwh > 0) {
                report.findings.add(
                        String.format(
                                Locale.ROOT,
                                "Energy is %.2f kWh lower than the comparison baseline.",
                                report.comparisonKwh
                        )
                );
            } else if (report.comparisonKwh < 0) {
                report.findings.add(
                        String.format(
                                Locale.ROOT,
                                "Energy is %.2f kWh higher than the comparison baseline.",
                                Math.abs(report.comparisonKwh)
                        )
                );
            } else {
                report.findings.add(
                        "Energy consumption matches the comparison baseline."
                );
            }
        }

        if (report.findings.isEmpty()) {
            report.findings.add(
                    "Report generated from the available saved inputs."
            );
        }

        return report;
    }

    private boolean equalsIgnoreCase(String first, String second) {
        return first != null
                && second != null
                && first.trim().equalsIgnoreCase(second.trim());
    }

    public static class MonthlyReport {

        private String month;
        private String previousMonth;
        private String room;

        private String baselineSource;
        private String comparisonLabel;

        private double tariffPerKwh;

        private double baselineKwh;
        private double baselineAmount;

        private double currentMonitoredKwh;
        private double currentMonitoredAmount;

        private double estimatedOccupiedEnergyKwh;
        private double potentialWasteKwh;
        private double potentialWasteAmount;

        private double currentBillKwh;
        private double currentBillAmount;

        private double comparisonKwh;
        private double comparisonAmount;

        private int applianceCount;
        private int monitoringRecordCount;

        private boolean baselineAvailable;
        private boolean currentBillAvailable;
        private boolean comparisonAvailable;

        private List<ApplianceLine> applianceLines = new ArrayList<>();
        private List<String> findings = new ArrayList<>();

        public String getMonth() { return month; }
        public String getPreviousMonth() { return previousMonth; }
        public String getRoom() { return room; }
        public String getBaselineSource() { return baselineSource; }
        public String getComparisonLabel() { return comparisonLabel; }

        public double getTariffPerKwh() { return tariffPerKwh; }
        public double getBaselineKwh() { return baselineKwh; }
        public double getBaselineAmount() { return baselineAmount; }
        public double getCurrentMonitoredKwh() { return currentMonitoredKwh; }
        public double getCurrentMonitoredAmount() { return currentMonitoredAmount; }
        public double getEstimatedOccupiedEnergyKwh() { return estimatedOccupiedEnergyKwh; }
        public double getPotentialWasteKwh() { return potentialWasteKwh; }
        public double getPotentialWasteAmount() { return potentialWasteAmount; }
        public double getCurrentBillKwh() { return currentBillKwh; }
        public double getCurrentBillAmount() { return currentBillAmount; }
        public double getComparisonKwh() { return comparisonKwh; }
        public double getComparisonAmount() { return comparisonAmount; }

        public int getApplianceCount() { return applianceCount; }
        public int getMonitoringRecordCount() { return monitoringRecordCount; }

        public boolean isBaselineAvailable() { return baselineAvailable; }
        public boolean isCurrentBillAvailable() { return currentBillAvailable; }
        public boolean isComparisonAvailable() { return comparisonAvailable; }

        public List<ApplianceLine> getApplianceLines() { return applianceLines; }
        public List<String> getFindings() { return findings; }
    }

    public static class ApplianceLine {

        private String applianceName;
        private String room;
        private double powerWatts;
        private double hours;
        private double energyKwh;
        private double wasteKwh;
        private double estimatedCost;

        public String getApplianceName() { return applianceName; }
        public String getRoom() { return room; }
        public double getPowerWatts() { return powerWatts; }
        public double getHours() { return hours; }
        public double getEnergyKwh() { return energyKwh; }
        public double getWasteKwh() { return wasteKwh; }
        public double getEstimatedCost() { return estimatedCost; }
    }
}
