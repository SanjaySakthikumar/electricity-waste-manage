
package com.energy.audit.controller;

import com.energy.audit.model.Recommendation;
import com.energy.audit.service.RecommendationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RecommendationController {

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping("/recommendations")
    public String showRecommendations(Model model) {
        model.addAttribute("recommendation", new Recommendation());
        model.addAttribute("recommendations", service.findAll());
        model.addAttribute("totalSavingsKwh",
                service.getEstimatedMonthlySavingsKwh());
        model.addAttribute("totalSavingsRupees",
                service.getEstimatedMonthlySavingsRupees());
        model.addAttribute("totalCost",
                service.getTotalImplementationCost());
        model.addAttribute("paybackMonths",
                service.getEstimatedPaybackMonths());

        return "recommendations";
    }

    @PostMapping("/recommendations")
    public String saveRecommendation(
            @ModelAttribute Recommendation recommendation,
            RedirectAttributes redirectAttributes) {

        if (recommendation.getTitle() == null
                || recommendation.getTitle().isBlank()
                || !Double.isFinite(
                    recommendation.getExpectedMonthlySavingsKwh())
                || recommendation.getExpectedMonthlySavingsKwh() < 0
                || !Double.isFinite(
                    recommendation.getImplementationCost())
                || recommendation.getImplementationCost() < 0) {

            redirectAttributes.addFlashAttribute(
                    "error", "Enter valid recommendation details.");
            return "redirect:/recommendations";
        }

        service.save(recommendation);

        redirectAttributes.addFlashAttribute(
                "message", "Recommendation saved successfully.");

        return "redirect:/recommendations";
    }
}
