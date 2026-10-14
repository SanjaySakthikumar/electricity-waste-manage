
package com.energy.audit.controller;

import com.energy.audit.model.BuildingProfile;
import com.energy.audit.repository.BuildingProfileRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class BuildingController {

    private final BuildingProfileRepository buildingProfileRepository;

    public BuildingController(
            BuildingProfileRepository buildingProfileRepository) {
        this.buildingProfileRepository = buildingProfileRepository;
    }

    @GetMapping("/building")
    public String showBuilding(Model model) {

        BuildingProfile profile =
                buildingProfileRepository
                        .findTopByOrderByIdDesc()
                        .orElseGet(BuildingProfile::new);

        model.addAttribute("buildingProfile", profile);

        return "building";
    }

    @PostMapping("/building")
    public String saveBuilding(
            @ModelAttribute BuildingProfile buildingProfile) {

        if (buildingProfile.getBuildingName() == null
                || buildingProfile.getBuildingName().isBlank()
                || !Double.isFinite(buildingProfile.getFloorAreaSqm())
                || buildingProfile.getFloorAreaSqm() <= 0) {

            return "redirect:/building?error";
        }

        // Update the latest profile instead of creating a new one
        // on every form submission.
        BuildingProfile existing =
                buildingProfileRepository
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

        return "redirect:/building?saved";
    }
}
