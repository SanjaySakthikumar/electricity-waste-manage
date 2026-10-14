package com.energy.audit.controller;

import com.energy.audit.service.RecommendationService;
import com.energy.audit.service.RecommendationService.AutoRecommendation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    public String showRecommendations(Model model) {
        List<AutoRecommendation> items =
                recommendationService.generateAutomaticRecommendations();

        double totalSavingsKwh = items.stream()
                .mapToDouble(AutoRecommendation::getEstimatedSavingsKwh)
                .sum();

        double totalSavingsRupees = items.stream()
                .mapToDouble(AutoRecommendation::getEstimatedSavingsRupees)
                .sum();

        double totalCost = items.stream()
                .mapToDouble(AutoRecommendation::getImplementationCost)
                .sum();

        double paybackMonths = totalSavingsRupees > 0
                ? totalCost / totalSavingsRupees : 0;

        model.addAttribute("autoRecommendations", items);
        model.addAttribute("totalSavingsKwh", totalSavingsKwh);
        model.addAttribute("totalSavingsRupees", totalSavingsRupees);
        model.addAttribute("totalCost", totalCost);
        model.addAttribute("paybackMonths", paybackMonths);

        return "recommendations";
    }
}