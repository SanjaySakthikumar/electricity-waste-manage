
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonthlyEnergyReportService {

    private final ApplianceRepository applianceRepository;
    private final ElectricityBillRepository billRepository;
    private final MonitoringRecordRepository monitoringRepository;

    @Value("${ecopulse.tariff-per-kwh:8.0}")
    private double tariffPerKwh;

    public MonthlyEnergyReportService(
            ApplianceRepository applianceRepository,
            ElectricityBillRepository billRepository,
            MonitoringRecordRepository monitoringRepository) {
        this.applianceRepository = applianceRepository;
        this.billRepository = billRepository;
        this.monitoringRepository = monitoringRepository;
    }

    public Map<String, Object> generateReport(
            YearMonth month, String selectedRoom) {

        if (month == null) {
            throw new IllegalArgumentException(
                    "Select a month for the energy report.");
        }

        if (selectedRoom == null || selectedRoom.isBlank()) {
            throw new IllegalArgumentException(
                    "Select a room for the energy report.");
        }

        String roomKey = selectedRoom.trim();

        // 1. Retrieve appliances registered in the selected room.
        List<Appliance> appliances = applianceRepository.findAll()
                .stream()
                .filter(a -> a.getRoom() != null)
                .filter(a -> a.getRoom().trim()
                        .equalsIgnoreCase(roomKey))
                .toList();

        // 2. Retrieve monitoring records for the selected room.
        List<MonitoringRecord> records =
                monitoringRepository.findAll()
                        .stream()
                        .filter(r -> r.getRoom() != null)
                        .filter(r -> r.getRoom().trim()
                                .equalsIgnoreCase(roomKey))
                        .toList();

        // 3. Estimate monthly baseline from appliance schedules.
        double estimatedBaselineKwh = appliances.stream()
                .mapToDouble(Appliance::getMonthlyKwh)
                .sum();

        // 4. Determine the previous calendar month.
        YearMonth previousMonth = month.minusMonths(1);

        LocalDate previousStart = previousMonth.atDay(1);
        LocalDate previousEnd = previousMonth.atEndOfMonth();

        // 5. Find a saved bill overlapping the previous month.
        // ElectricityBill currently has no room field, so this bill
        // cannot be guaranteed to belong to the selected room.
        List<ElectricityBill> matchingBills = billRepository.findAll()
                .stream()
                .filter(b -> b.getPeriodStart() != null)
                .filter(b -> b.getPeriodEnd() != null)
                .filter(b -> !b.getPeriodEnd()
                        .isBefore(b.getPeriodStart()))
                .filter(b -> !b.getPeriodStart().isAfter(previousEnd))
                .filter(b -> !b.getPeriodEnd().isBefore(previousStart))
                .toList();

        ElectricityBill previousBill = matchingBills.stream()
                .filter(b -> YearMonth.from(b.getPeriodEnd())
                        .equals(previousMonth))
                .max(Comparator.comparing(
                        ElectricityBill::getPeriodEnd))
                .orElseGet(() -> matchingBills.stream()
                        .max(Comparator.comparing(
                                ElectricityBill::getPeriodEnd))
                        .orElse(null));

        boolean actualPreviousBillAvailable =
                previousBill != null;

        double previousMonthKwh =
                actualPreviousBillAvailable
                        ? previousBill.getBilledKwh()
                        : estimatedBaselineKwh;

        double previousMonthAmount =
                actualPreviousBillAvailable
                        ? previousBill.getBillAmount()
                        : estimatedBaselineKwh * tariffPerKwh;

        // 6. Set current-month time boundaries.
        LocalDateTime monthStart =
                month.atDay(1).atStartOfDay();

        LocalDateTime nextMonthStart =
                month.plusMonths(1).atDay(1).atStartOfDay();

        // 7. Accumulate appliance-wise energy and wastage.
        Map<String, ApplianceWaste> wasteMap =
                new LinkedHashMap<>();

        double currentMonthKwh = 0.0;
        double occupiedKwh = 0.0;
        double unoccupiedKwh = 0.0;
        double totalWasteHours = 0.0;

        int monitoringRecordCount = 0;

        for (MonitoringRecord record : records) {

            if (!isValidRecord(record)) {
                continue;
            }

            // Only count the part of the appliance ON interval
            // that overlaps the selected calendar month.
            LocalDateTime onStart = later(
                    record.getDeviceOnStart(), monthStart);

            LocalDateTime onEnd = earlier(
                    record.getDeviceOnEnd(), nextMonthStart);

            if (!onEnd.isAfter(onStart)) {
                continue;
            }

            monitoringRecordCount++;

            double onHours = hoursBetween(onStart, onEnd);

            // Calculate the overlap with the room-empty interval.
            LocalDateTime emptyStart = later(
                    onStart, record.getRoomEmptyStart());

            LocalDateTime emptyEnd = earlier(
                    onEnd, record.getRoomEmptyEnd());

            double emptyHours = emptyEnd.isAfter(emptyStart)
                    ? hoursBetween(emptyStart, emptyEnd)
                    : 0.0;

            emptyHours = Math.min(
                    onHours, Math.max(0.0, emptyHours));

            double powerWatts = record.getPowerWatts();

            double totalEnergyKwh =
                    powerWatts * onHours / 1000.0;

            double unoccupiedEnergyKwh =
                    powerWatts * emptyHours / 1000.0;

            double occupiedEnergyKwh =
                    totalEnergyKwh - unoccupiedEnergyKwh;

            String applianceName =
                    record.getApplianceName().trim();

            String roomName = record.getRoom().trim();

            String key = applianceName.toLowerCase()
                    + "|" + roomName.toLowerCase();

            ApplianceWaste item = wasteMap.computeIfAbsent(
                    key,
                    ignored -> new ApplianceWaste(
                            applianceName,
                            roomName,
                            powerWatts));

            item.add(
                    onHours,
                    emptyHours,
                    totalEnergyKwh,
                    occupiedEnergyKwh,
                    unoccupiedEnergyKwh);

            currentMonthKwh += totalEnergyKwh;
            occupiedKwh += occupiedEnergyKwh;
            unoccupiedKwh += unoccupiedEnergyKwh;
            totalWasteHours += emptyHours;
        }

        // 8. If there are no valid records, use the schedule
        // estimate and clearly identify it as an estimate.
        boolean monitoringAvailable =
                monitoringRecordCount > 0;

        String currentMonthSource;

        if (!monitoringAvailable) {
            currentMonthKwh = estimatedBaselineKwh;
            occupiedKwh = 0.0;
            unoccupiedKwh = 0.0;
            totalWasteHours = 0.0;

            currentMonthSource = "SCHEDULE_ESTIMATE";
        } else {
            currentMonthSource =
                    "MONITORED_INTERVAL_ESTIMATE";
        }

        // 9. Rank appliances by estimated unoccupied energy.
        List<ApplianceWaste> applianceWaste =
                new ArrayList<>(wasteMap.values());

        applianceWaste.sort(
                Comparator.comparingDouble(
                        ApplianceWaste::getWastedKwh)
                        .reversed());

        // 10. Generate basic rule-based recommendations.
        List<String> recommendations = new ArrayList<>();

        for (ApplianceWaste item : applianceWaste) {
            if (item.getWastedKwh() > 0.001) {
                recommendations.add(
                        item.getApplianceName()
                        + " in " + item.getRoom()
                        + " used "
                        + String.format(
                                "%.3f", item.getWastedKwh())
                        + " kWh during recorded empty-room intervals. "
                        + "Review its operating schedule and determine "
                        + "whether automatic or manual switch-off is appropriate.");
            }
        }

        if (recommendations.isEmpty()) {
            if (monitoringAvailable) {
                recommendations.add(
                        "No positive unoccupied energy was calculated "
                        + "from the available monitoring intervals.");
            } else {
                recommendations.add(
                        "No valid monitoring intervals are available. "
                        + "Current-month energy is estimated from appliance schedules.");
            }
        }

        // 11. Compare current energy with the previous-month reference.
        double energyDifferenceKwh =
                currentMonthKwh - previousMonthKwh;

        Double percentageChange = previousMonthKwh > 0
                ? energyDifferenceKwh / previousMonthKwh * 100.0
                : null;

        // 12. Return the results for the Thymeleaf report.
        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("selectedRoom", roomKey);
        result.put("month", month.toString());
        result.put("previousMonth", previousMonth.toString());

        result.put("previousBill", previousBill);
        result.put("actualPreviousBillAvailable",
                actualPreviousBillAvailable);

        result.put("previousMonthSource",
                actualPreviousBillAvailable
                        ? "ACTUAL_BILL"
                        : "ESTIMATED_BASELINE");

        result.put("previousMonthKwh", previousMonthKwh);
        result.put("previousMonthAmount", previousMonthAmount);
        result.put("estimatedBaselineKwh", estimatedBaselineKwh);

        result.put("currentMonthKwh", currentMonthKwh);
        result.put("currentMonthSource", currentMonthSource);
        result.put("monitoringAvailable", monitoringAvailable);
        result.put("monitoringRecordCount",
                monitoringRecordCount);

        result.put("occupiedKwh", occupiedKwh);
        result.put("unoccupiedKwh", unoccupiedKwh);
        result.put("totalWasteKwh", unoccupiedKwh);
        result.put("totalWasteHours", totalWasteHours);

        result.put("currentMonthAmount",
                currentMonthKwh * tariffPerKwh);

        result.put("potentialWasteCost",
                unoccupiedKwh * tariffPerKwh);

        result.put("tariffPerKwh", tariffPerKwh);

        result.put("energyDifferenceKwh",
                energyDifferenceKwh);

        result.put("percentageChange", percentageChange);

        result.put("applianceCount", appliances.size());
        result.put("applianceWaste", applianceWaste);
        result.put("recommendations", recommendations);

        return result;
    }

    private boolean isValidRecord(MonitoringRecord record) {
        return record.getApplianceName() != null
                && !record.getApplianceName().isBlank()
                && record.getRoom() != null
                && !record.getRoom().isBlank()
                && record.getDeviceOnStart() != null
                && record.getDeviceOnEnd() != null
                && record.getRoomEmptyStart() != null
                && record.getRoomEmptyEnd() != null
                && Double.isFinite(record.getPowerWatts())
                && record.getPowerWatts() > 0
                && record.getDeviceOnEnd()
                        .isAfter(record.getDeviceOnStart())
                && record.getRoomEmptyEnd()
                        .isAfter(record.getRoomEmptyStart());
    }

    private LocalDateTime later(
            LocalDateTime first,
            LocalDateTime second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalDateTime earlier(
            LocalDateTime first,
            LocalDateTime second) {
        return first.isBefore(second) ? first : second;
    }

    private double hoursBetween(
            LocalDateTime start,
            LocalDateTime end) {
        return Duration.between(start, end).toSeconds()
                / 3600.0;
    }

    public static class ApplianceWaste {

        private final String applianceName;
        private final String room;
        private final double powerWatts;

        private double totalOnHours;
        private double wastedHours;
        private double totalKwh;
        private double occupiedKwh;
        private double wastedKwh;

        public ApplianceWaste(
                String applianceName,
                String room,
                double powerWatts) {
            this.applianceName = applianceName;
            this.room = room;
            this.powerWatts = powerWatts;
        }

        public void add(
                double onHours,
                double emptyHours,
                double totalEnergyKwh,
                double occupiedEnergyKwh,
                double unoccupiedEnergyKwh) {

            this.totalOnHours += onHours;
            this.wastedHours += emptyHours;
            this.totalKwh += totalEnergyKwh;
            this.occupiedKwh += occupiedEnergyKwh;
            this.wastedKwh += unoccupiedEnergyKwh;
        }

        public String getApplianceName() {
            return applianceName;
        }

        public String getRoom() {
            return room;
        }

        public double getPowerWatts() {
            return powerWatts;
        }

        public double getTotalOnHours() {
            return totalOnHours;
        }

        public double getWastedHours() {
            return wastedHours;
        }

        public double getTotalKwh() {
            return totalKwh;
        }

        public double getOccupiedKwh() {
            return occupiedKwh;
        }

        public double getWastedKwh() {
            return wastedKwh;
        }
    }
}
