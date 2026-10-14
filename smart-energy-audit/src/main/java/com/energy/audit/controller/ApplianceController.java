
package com.energy.audit.controller;

import com.energy.audit.model.Appliance;
import com.energy.audit.repository.ApplianceRepository;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@Controller
public class ApplianceController {

    private final ApplianceRepository applianceRepository;

    public ApplianceController(ApplianceRepository applianceRepository) {
        this.applianceRepository = applianceRepository;
    }

    @GetMapping("/appliances")
    public String showAppliances(Model model) {
        preparePage(model, new Appliance());
        return "appliances";
    }

    @PostMapping("/appliances")
    public String addAppliance(
            @Valid @ModelAttribute("appliance") Appliance appliance,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            preparePage(model, appliance);
            return "appliances";
        }

        if (appliance.getHoursPerDay() > 24) {
            bindingResult.rejectValue(
                    "hoursPerDay",
                    "hoursPerDay.invalid",
                    "Operating hours cannot exceed 24 per day."
            );

            preparePage(model, appliance);
            return "appliances";
        }

        if (appliance.getDaysPerMonth() > 31) {
            bindingResult.rejectValue(
                    "daysPerMonth",
                    "daysPerMonth.invalid",
                    "Operating days cannot exceed 31 per month."
            );

            preparePage(model, appliance);
            return "appliances";
        }

        applianceRepository.save(appliance);

        return "redirect:/appliances";
    }

    @PostMapping("/appliances/delete/{id}")
    public String deleteAppliance(@PathVariable Long id) {
        if (applianceRepository.existsById(id)) {
            applianceRepository.deleteById(id);
        }

        return "redirect:/appliances";
    }

    private void preparePage(Model model, Appliance appliance) {
        List<Appliance> appliances = applianceRepository.findAll();

        double estimatedMonthlyKwh = appliances.stream()
                .mapToDouble(Appliance::getMonthlyKwh)
                .sum();

        model.addAttribute("appliances", appliances);
        model.addAttribute("appliance", appliance);
        model.addAttribute("estimatedMonthlyKwh", estimatedMonthlyKwh);
    }
}
