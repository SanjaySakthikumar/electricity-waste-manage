
package com.energy.audit.service;

import com.energy.audit.model.Appliance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ApplianceBasedRecommendationService {

    public List<String> generateRecommendations(
            List<Appliance> appliances) {

        List<String> recommendations = new ArrayList<>();

        if (appliances == null || appliances.isEmpty()) {
            recommendations.add(
                    "Register appliances to receive appliance-specific "
                    + "energy-saving recommendations.");
            return recommendations;
        }

        for (Appliance appliance : appliances) {

            if (appliance == null || appliance.getName() == null) {
                continue;
            }

            String name = appliance.getName()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            double watts = appliance.getRatedPowerWatts();
            int quantity = appliance.getQuantity();
            double monthlyKwh = appliance.getMonthlyKwh();

            if (watts <= 0 || quantity <= 0) {
                recommendations.add(
                        appliance.getName()
                        + ": Verify the rated power and quantity "
                        + "before calculating energy savings.");
                continue;
            }

            String recommendation = getRecommendation(
                    name, watts, quantity, monthlyKwh);

            recommendations.add(
                    appliance.getName() + " (" + appliance.getRoom()
                    + ", " + String.format("%.0f", watts) + " W each): "
                    + recommendation
                    + " Estimated scheduled consumption: "
                    + String.format("%.2f", monthlyKwh) + " kWh/month.");
        }

        return recommendations;
    }

    private String getRecommendation(
            String name,
            double watts,
            int quantity,
            double monthlyKwh) {

        // Lighting
        if (containsAny(name, "bulb", "lamp", "light", "tube light")) {
            if (containsAny(name, "led")) {
                return "Already identified as LED lighting. Check its "
                        + "energy rating and choose an efficient LED "
                        + "replacement only if it offers a meaningful "
                        + "improvement.";
            }

            return "Consider replacing conventional lighting with an "
                    + "equivalent-brightness LED. Compare rated watts, "
                    + "lumens and expected lifetime before purchasing.";
        }

        // Fans
        if (containsAny(name, "fan", "ceiling fan", "table fan")) {
            if (containsAny(name, "bldc")) {
                return "Already identified as a BLDC fan. Check its "
                        + "rated power and energy performance before "
                        + "considering an upgrade.";
            }

            return "Compare this fan with a suitably sized, energy-efficient "
                    + "BLDC model. Check airflow, rated watts and speed "
                    + "settings to estimate possible savings.";
        }

        // Air conditioners
        if (containsAny(name, "air conditioner", "airconditioner",
                "split ac", "window ac", " ac")) {
            if (containsAny(name, "inverter")) {
                return "Check the efficiency rating, cooling capacity, "
                        + "filters and refrigerant-system condition. "
                        + "Compare models using their declared energy "
                        + "performance rather than rated watts alone.";
            }

            return "Compare an appropriately sized inverter AC with the "
                    + "existing unit. Review its efficiency rating, "
                    + "cooling capacity and expected annual energy use.";
        }

        // Refrigerators
        if (containsAny(name, "refrigerator", "fridge")) {
            return "Check the energy label, door seals, thermostat "
                    + "setting and ventilation clearance. If replacement "
                    + "is justified, compare models of similar capacity "
                    + "using their declared annual energy consumption.";
        }

        // Water heaters
        if (containsAny(name, "water heater", "geyser", "water boiler")) {
            return "Check the required tank capacity, insulation and "
                    + "temperature setting. Compare appropriately sized "
                    + "efficient models using expected energy consumption "
                    + "and hot-water requirements.";
        }

        // Irons and heating appliances
        if (containsAny(name, "iron", "heater", "heating element")) {
            return "Choose an appliance with appropriate capacity and "
                    + "temperature control. Compare energy performance "
                    + "for the same required heating task before replacing it.";
        }

        // Pumps
        if (containsAny(name, "water pump", "pump", "motor")) {
            return "Check that the pump or motor is correctly sized for "
                    + "the required flow and head. Compare efficiency "
                    + "ratings and investigate unnecessary running or "
                    + "mechanical faults.";
        }

        // Computers and office equipment
        if (containsAny(name, "computer", "desktop", "monitor",
                "printer", "laptop")) {
            return "Enable available energy-saving settings and compare "
                    + "the rated power of equivalent efficient models. "
                    + "Avoid replacing a working device unless the "
                    + "expected savings justify the cost.";
        }

        // TVs and consumer electronics
        if (containsAny(name, "television", " tv", "smart tv",
                "speaker", "set top box")) {
            return "Compare equivalent models using their energy ratings "
                    + "and standby consumption. Disable unnecessary "
                    + "standby operation where practical.";
        }

        // General recommendation
        if (monthlyKwh >= 50) {
            return "This appliance has relatively high scheduled energy "
                    + "consumption. Check its energy label, rated power, "
                    + "quantity and required performance. Compare an "
                    + "efficient equivalent before considering replacement.";
        }

        if (monthlyKwh >= 10) {
            return "Compare the appliance's energy rating with equivalent "
                    + "models. Consider an upgrade only if the expected "
                    + "energy and cost savings justify the purchase price.";
        }

        return "Check the appliance's rated power and energy label. "
                + "A replacement may not be worthwhile unless a more "
                + "efficient equivalent offers meaningful savings.";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
