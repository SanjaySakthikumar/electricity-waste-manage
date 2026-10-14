package com.energy.audit.service;

import com.energy.audit.model.Appliance;
import com.energy.audit.model.ElectricityBill;
import com.energy.audit.repository.ApplianceRepository;
import com.energy.audit.repository.ElectricityBillRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class RecommendationService {

    private final ApplianceRepository applianceRepository;
    private final ElectricityBillRepository billRepository;

    @Value("${ecopulse.tariff-per-kwh:8.0}")
    private double configuredTariff;

    public RecommendationService(
            ApplianceRepository applianceRepository,
            ElectricityBillRepository billRepository) {
        this.applianceRepository = applianceRepository;
        this.billRepository = billRepository;
    }

    public List<AutoRecommendation> generateAutomaticRecommendations() {
        List<AutoRecommendation> results = new ArrayList<>();
        double tariff = getTariff();

        for (Appliance appliance : applianceRepository.findAll()) {
            if (appliance == null || appliance.getName() == null
                    || appliance.getName().isBlank()
                    || appliance.getRatedPowerWatts() <= 0
                    || appliance.getQuantity() <= 0
                    || appliance.getHoursPerDay() <= 0
                    || appliance.getHoursPerDay() > 24
                    || appliance.getDaysPerMonth() <= 0
                    || appliance.getDaysPerMonth() > 31) {
                continue;
            }

            String name = appliance.getName().trim();
            double watts = appliance.getRatedPowerWatts();
            int quantity = appliance.getQuantity();
            double hours = appliance.getHoursPerDay();
            int days = appliance.getDaysPerMonth();

            Advice advice = getAdvice(name.toLowerCase(Locale.ROOT), watts);
            double suggestedWatts = Math.min(watts, advice.suggestedWatts);

            double currentKwh = watts * quantity * hours * days / 1000.0;
            double suggestedKwh = suggestedWatts * quantity * hours * days / 1000.0;
            double savingsKwh = Math.max(0, currentKwh - suggestedKwh);
            double savingsRupees = savingsKwh * tariff;
            double cost = advice.costPerUnit * quantity;

            AutoRecommendation item = new AutoRecommendation();
            item.rank = 0;
            item.room = appliance.getRoom() == null ? "Not specified" : appliance.getRoom();
            item.applianceName = name;
            item.ratedPowerWatts = watts;
            item.suggestedApplianceName = advice.suggestedName;
            item.suggestedPowerWatts = suggestedWatts;
            item.quantity = quantity;
            item.hoursPerDay = hours;
            item.daysPerMonth = days;
            item.currentMonthlyKwh = currentKwh;
            item.suggestedMonthlyKwh = suggestedKwh;
            item.estimatedSavingsKwh = savingsKwh;
            item.estimatedSavingsRupees = savingsRupees;
            item.implementationCost = cost;
            item.paybackMonths = savingsRupees > 0 ? cost / savingsRupees : 0;
            item.title = advice.title;
            item.description = advice.description;
            item.practicality = advice.practicality;
            item.priorityScore = savingsRupees * advice.practicality
                    / (1.0 + cost / 1000.0);
            item.rankingReason = String.format(
                    Locale.ROOT,
                    "Estimated savings: %.2f kWh/month (₹%.2f/month). "
                            + "Priority considers estimated savings, practicality and cost.",
                    savingsKwh, savingsRupees);

            results.add(item);
        }

        results.sort(Comparator
                .comparingDouble(AutoRecommendation::getPriorityScore)
                .reversed()
                .thenComparing(AutoRecommendation::getApplianceName,
                        String.CASE_INSENSITIVE_ORDER));

        for (int i = 0; i < results.size(); i++) {
            results.get(i).rank = i + 1;
        }

        return results;
    }

    private double getTariff() {
        ElectricityBill bill = billRepository
                .findTopByOrderByPeriodEndDesc()
                .orElse(null);

        if (bill != null && bill.getBilledKwh() > 0
                && bill.getBillAmount() > 0) {
            double rate = bill.getBillAmount() / bill.getBilledKwh();
            if (Double.isFinite(rate) && rate > 0) {
                return rate;
            }
        }

        return Double.isFinite(configuredTariff) && configuredTariff > 0
                ? configuredTariff : 8.0;
    }

    private Advice getAdvice(String name, double watts) {
        if (contains(name, "tube light", "fluorescent", "cfl", "bulb", "lamp", "light")) {
            return new Advice("Energy-efficient LED lighting",
                    "Review lighting efficiency",
                    "Compare suitable LED replacements with equivalent brightness and compatible fittings.",
                    Math.min(18, watts * 0.60), 180, 5);
        }
        if (contains(name, "fan")) {
            return new Advice("Energy-efficient BLDC fan",
                    "Review fan efficiency",
                    "Compare a similarly sized BLDC fan and verify airflow, rated power and warranty.",
                    Math.min(30, watts * 0.50), 3000, 5);
        }
        if (contains(name, "air conditioner", "aircon", "ac unit", "hvac")) {
            return new Advice("High-efficiency inverter AC",
                    "Review air-conditioning efficiency",
                    "Compare correctly sized high-efficiency models. Actual savings depend on usage and model ratings.",
                    watts * 0.80, 35000, 4);
        }
        if (contains(name, "refrigerator", "fridge")) {
            return new Advice("High-efficiency refrigerator",
                    "Review refrigerator efficiency",
                    "Compare equivalent-capacity models using their annual energy consumption labels.",
                    watts * 0.75, 25000, 4);
        }
        if (contains(name, "television", "smart tv", "tv")) {
            return new Advice("Energy-efficient television",
                    "Review television efficiency",
                    "Compare models of similar screen size and features using rated power.",
                    watts * 0.80, 20000, 3);
        }
        if (contains(name, "water heater", "geyser", "water boiler")) {
            return new Advice("Efficient water heater",
                    "Review water-heating energy use",
                    "Review thermostat settings, insulation and hot-water demand before replacing equipment.",
                    watts * 0.85, 10000, 4);
        }
        if (contains(name, "pump", "motor")) {
            return new Advice("High-efficiency motor or pump",
                    "Review motor or pump efficiency",
                    "Check sizing, operating load and maintenance. Verify required flow and pressure.",
                    watts * 0.85, 12000, 3);
        }

        return new Advice("Efficient equivalent appliance",
                "Assess appliance efficiency",
                "No specific appliance category matched. Check the actual rating and compare equivalent models. The estimated 10% reduction is illustrative only.",
                watts * 0.90, 5000, 2);
    }

    private boolean contains(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static class Advice {
        final String suggestedName;
        final String title;
        final String description;
        final double suggestedWatts;
        final double costPerUnit;
        final int practicality;

        Advice(String suggestedName, String title, String description,
               double suggestedWatts, double costPerUnit, int practicality) {
            this.suggestedName = suggestedName;
            this.title = title;
            this.description = description;
            this.suggestedWatts = suggestedWatts;
            this.costPerUnit = costPerUnit;
            this.practicality = practicality;
        }
    }

    public static class AutoRecommendation {
        private int rank;
        private String room;
        private String applianceName;
        private double ratedPowerWatts;
        private String suggestedApplianceName;
        private double suggestedPowerWatts;
        private int quantity;
        private double hoursPerDay;
        private int daysPerMonth;
        private double currentMonthlyKwh;
        private double suggestedMonthlyKwh;
        private double estimatedSavingsKwh;
        private double estimatedSavingsRupees;
        private double implementationCost;
        private double paybackMonths;
        private String title;
        private String description;
        private int practicality;
        private double priorityScore;
        private String rankingReason;

        public int getRank() { return rank; }
        public String getRoom() { return room; }
        public String getApplianceName() { return applianceName; }
        public double getRatedPowerWatts() { return ratedPowerWatts; }
        public String getSuggestedApplianceName() { return suggestedApplianceName; }
        public double getSuggestedPowerWatts() { return suggestedPowerWatts; }
        public int getQuantity() { return quantity; }
        public double getHoursPerDay() { return hoursPerDay; }
        public int getDaysPerMonth() { return daysPerMonth; }
        public double getCurrentMonthlyKwh() { return currentMonthlyKwh; }
        public double getSuggestedMonthlyKwh() { return suggestedMonthlyKwh; }
        public double getEstimatedSavingsKwh() { return estimatedSavingsKwh; }
        public double getEstimatedSavingsRupees() { return estimatedSavingsRupees; }
        public double getImplementationCost() { return implementationCost; }
        public double getPaybackMonths() { return paybackMonths; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public int getPracticality() { return practicality; }
        public double getPriorityScore() { return priorityScore; }
        public String getRankingReason() { return rankingReason; }
    }
}