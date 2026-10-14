
package com.energy.audit.controller;

import com.energy.audit.model.Appliance;
import com.energy.audit.model.ElectricityBill;
import com.energy.audit.model.BuildingProfile;
import com.energy.audit.repository.BuildingProfileRepository;
import com.energy.audit.service.EnergyAnalysisService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AnalysisController {

    private final EnergyAnalysisService service;
    private final BuildingProfileRepository buildingProfileRepository;

    public AnalysisController(
            EnergyAnalysisService service,
            BuildingProfileRepository buildingProfileRepository) {
        this.service = service;
        this.buildingProfileRepository = buildingProfileRepository;
    }

    @GetMapping("/analysis")
    public String analysis(Model model) {

        List<Appliance> appliances = service.getAppliances();
        ElectricityBill bill = service.getLatestBill();

        BuildingProfile profile = buildingProfileRepository
                .findTopByOrderByIdDesc()
                .orElse(null);

        double estimatedMonthlyKwh =
                service.getEstimatedMonthlyKwh();

        model.addAttribute("appliances", appliances);
        model.addAttribute("estimatedMonthlyKwh",
                estimatedMonthlyKwh);
        model.addAttribute("bill", bill);
        model.addAttribute("buildingProfile", profile);

        if (profile != null) {
            model.addAttribute("energyIntensity",
                    service.getEnergyIntensityKwhPerSqm(
                            profile.getFloorAreaSqm()));
        } else {
            model.addAttribute("energyIntensity", null);
        }

        if (bill != null) {
            double estimatedPeriodKwh =
                    service.getEstimatedPeriodKwh(bill);

            model.addAttribute("estimatedPeriodKwh",
                    estimatedPeriodKwh);

            model.addAttribute("differenceKwh",
                    service.getUnexplainedKwh(bill));

            model.addAttribute("estimatedCost",
                    service.getEstimatedCost(
                            estimatedPeriodKwh, bill));

            model.addAttribute("averageRatePerKwh",
                    service.getAverageRatePerKwh(bill));

            model.addAttribute("differencePercent",
                    service.getConsumptionDifferencePercent(bill));
        }

        return "analysis";
    }
}
