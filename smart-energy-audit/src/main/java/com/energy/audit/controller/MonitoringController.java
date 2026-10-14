package com.energy.audit.controller;

import com.energy.audit.model.Appliance;
import com.energy.audit.model.MonitoringRecord;
import com.energy.audit.repository.ApplianceRepository;
import com.energy.audit.repository.MonitoringRecordRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class MonitoringController {

    private final MonitoringRecordRepository monitoringRecordRepository;
    private final ApplianceRepository applianceRepository;

    public MonitoringController(
            MonitoringRecordRepository monitoringRecordRepository,
            ApplianceRepository applianceRepository) {

        this.monitoringRecordRepository = monitoringRecordRepository;
        this.applianceRepository = applianceRepository;
    }

    @GetMapping("/monitoring")
    public String showMonitoring(
            @RequestParam(required = false) String room,
            Model model) {

        List<Appliance> allAppliances = applianceRepository.findAll();

        // Get unique room names from appliances already registered.
        List<String> rooms = allAppliances.stream()
                .map(Appliance::getRoom)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        // List only appliances belonging to the selected room.
        List<Appliance> roomAppliances = allAppliances.stream()
                .filter(appliance -> room != null
                        && appliance.getRoom() != null
                        && appliance.getRoom().trim()
                                .equalsIgnoreCase(room.trim()))
                .collect(Collectors.toList());

        List<MonitoringRecord> records =
                monitoringRecordRepository.findAll();

        double totalPotentialWasteKwh = records.stream()
                .mapToDouble(MonitoringRecord::getPotentialWasteKwh)
                .sum();

        double totalEmptyRoomOnHours = records.stream()
                .mapToDouble(MonitoringRecord::getOverlapHours)
                .sum();

        long alertCount = records.stream()
                .filter(record -> record.getPotentialWasteKwh() > 0)
                .count();

        model.addAttribute("monitoringRecords", records);
        model.addAttribute("monitoringRecord", new MonitoringRecord());

        model.addAttribute("rooms", rooms);
        model.addAttribute("appliances", roomAppliances);
        model.addAttribute("selectedRoom", room == null ? "" : room.trim());

        model.addAttribute("totalPotentialWasteKwh",
                totalPotentialWasteKwh);
        model.addAttribute("totalEmptyRoomOnHours",
                totalEmptyRoomOnHours);
        model.addAttribute("alertCount", alertCount);

        return "monitoring";
    }

    @PostMapping("/monitoring")
    public String saveMonitoring(
            @ModelAttribute MonitoringRecord monitoringRecord,
            RedirectAttributes redirectAttributes) {

        if (monitoringRecord.getRoom() == null
                || monitoringRecord.getRoom().isBlank()
                || monitoringRecord.getApplianceName() == null
                || monitoringRecord.getApplianceName().isBlank()
                || monitoringRecord.getDeviceOnStart() == null
                || monitoringRecord.getDeviceOnEnd() == null
                || monitoringRecord.getRoomEmptyStart() == null
                || monitoringRecord.getRoomEmptyEnd() == null) {

            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please select a room and appliance and enter all monitoring times.");

            return "redirect:/monitoring";
        }

        if (!monitoringRecord.getDeviceOnEnd()
                    .isAfter(monitoringRecord.getDeviceOnStart())
                || !monitoringRecord.getRoomEmptyEnd()
                    .isAfter(monitoringRecord.getRoomEmptyStart())) {

            redirectAttributes.addFlashAttribute("errorMessage",
                    "End times must be later than their corresponding start times.");

            return "redirect:/monitoring";
        }

        // Find the registered appliance using its selected room and name.
        Appliance selectedAppliance = applianceRepository.findAll()
                .stream()
                .filter(appliance -> appliance.getRoom() != null
                        && appliance.getName() != null
                        && appliance.getRoom().trim()
                                .equalsIgnoreCase(
                                        monitoringRecord.getRoom().trim())
                        && appliance.getName().trim()
                                .equalsIgnoreCase(
                                        monitoringRecord.getApplianceName().trim()))
                .findFirst()
                .orElse(null);

        if (selectedAppliance == null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "The selected appliance does not exist in that room. Please select it again.");

            return "redirect:/monitoring";
        }

        // Always use the rated power stored in the Appliance module.
        monitoringRecord.setApplianceName(selectedAppliance.getName());
        monitoringRecord.setRoom(selectedAppliance.getRoom());
        monitoringRecord.setPowerWatts(
                selectedAppliance.getRatedPowerWatts());

        if (!Double.isFinite(monitoringRecord.getPowerWatts())
                || monitoringRecord.getPowerWatts() <= 0) {

            redirectAttributes.addFlashAttribute("errorMessage",
                    "The registered appliance must have a valid rated power.");

            return "redirect:/monitoring";
        }

        monitoringRecordRepository.save(monitoringRecord);

        redirectAttributes.addFlashAttribute("successMessage",
                "Monitoring record saved successfully.");

        return "redirect:/monitoring?room="
                + java.net.URLEncoder.encode(
                        selectedAppliance.getRoom(),
                        java.nio.charset.StandardCharsets.UTF_8);
    }

    @PostMapping("/monitoring/delete/{id}")
    public String deleteMonitoring(@PathVariable Long id) {
        monitoringRecordRepository.deleteById(id);
        return "redirect:/monitoring";
    }
}