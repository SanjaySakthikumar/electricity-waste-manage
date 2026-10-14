package com.energy.audit.controller;

import com.energy.audit.model.BuildingProfile;
import com.energy.audit.repository.BuildingProfileRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BuildingController {

    private final BuildingProfileRepository buildingProfileRepository;

    public BuildingController(
            BuildingProfileRepository buildingProfileRepository) {
        this.buildingProfileRepository = buildingProfileRepository;
    }

    @GetMapping("/building")
    public String showBuilding(Model model) {

        BuildingProfile profile = buildingProfileRepository
                .findTopByOrderByIdDesc()
                .orElseGet(BuildingProfile::new);

        model.addAttribute("buildingProfile", profile);

        return "building";
    }

    @PostMapping("/building")
    public String saveBuilding(
            @ModelAttribute BuildingProfile buildingProfile,
            RedirectAttributes redirectAttributes) {

        // Validate the submitted building details
        if (buildingProfile.getBuildingName() == null
                || buildingProfile.getBuildingName().isBlank()
                || !Double.isFinite(buildingProfile.getFloorAreaSqm())
                || buildingProfile.getFloorAreaSqm() <= 0) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Please enter a valid building name and floor area greater than zero."
            );

            return "redirect:/building";
        }

        // Update the existing profile if one already exists
        BuildingProfile existing = buildingProfileRepository
                .findTopByOrderByIdDesc()
                .orElse(null);

        if (existing != null) {
            existing.setBuildingName(
                    buildingProfile.getBuildingName().trim());

            existing.setFloorAreaSqm(
                    buildingProfile.getFloorAreaSqm());

            buildingProfileRepository.save(existing);

        } else {
            buildingProfile.setBuildingName(
                    buildingProfile.getBuildingName().trim());

            buildingProfileRepository.save(buildingProfile);
        }

        // Return to the dashboard after successful submission
        redirectAttributes.addFlashAttribute(
                "message",
                "Building details saved successfully!"
        );

        return "redirect:/";
    }
}