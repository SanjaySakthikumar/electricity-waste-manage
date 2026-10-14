
package com.energy.audit.controller;

import com.energy.audit.repository.ApplianceRepository;
import com.energy.audit.repository.ElectricityBillRepository;
import com.energy.audit.repository.MonitoringRecordRepository;
import com.energy.audit.service.RecommendationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;

@Controller
public class AuditReportController {

    private final ApplianceRepository applianceRepository;
    private final ElectricityBillRepository electricityBillRepository;
    private final MonitoringRecordRepository monitoringRecordRepository;
    private final RecommendationService recommendationService;

    public AuditReportController(
            ApplianceRepository applianceRepository,
            ElectricityBillRepository electricityBillRepository,
            MonitoringRecordRepository monitoringRecordRepository,
            RecommendationService recommendationService) {

        this.applianceRepository = applianceRepository;
        this.electricityBillRepository = electricityBillRepository;
        this.monitoringRecordRepository = monitoringRecordRepository;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/report")
    public String showAuditReport(
            @RequestParam(required = false) String room,
            Model model) {

        String selectedRoom = room == null ? "" : room.trim();

        var appliances = applianceRepository.findAll()
                .stream()
                .filter(a -> selectedRoom.isEmpty()
                        || (a.getRoom() != null
                        && a.getRoom().equalsIgnoreCase(selectedRoom)))
                .toList();

        var bills = electricityBillRepository.findAll();

        var monitoringRecords = monitoringRecordRepository.findAll()
                .stream()
                .filter(r -> selectedRoom.isEmpty()
                        || (r.getRoom() != null
                        && r.getRoom().equalsIgnoreCase(selectedRoom)))
                .toList();

        var recommendations =
                recommendationService.generateAutomaticRecommendations()
                        .stream()
                        .filter(r -> selectedRoom.isEmpty()
                                || (r.getRoom() != null
                                && r.getRoom().equalsIgnoreCase(selectedRoom)))
                        .toList();

        double estimatedInventoryKwh = appliances.stream()
                .mapToDouble(a -> a.getMonthlyKwh())
                .sum();

        double monitoredEnergyKwh = monitoringRecords.stream()
                .filter(r -> r.getDeviceOnStart() != null
                        && r.getDeviceOnEnd() != null
                        && r.getDeviceOnEnd().isAfter(r.getDeviceOnStart()))
                .mapToDouble(r -> r.getPowerWatts()
                        * Duration.between(
                                r.getDeviceOnStart(),
                                r.getDeviceOnEnd()
                        ).toMinutes() / 60.0 / 1000.0)
                .sum();

        double potentialWasteKwh = monitoringRecords.stream()
                .mapToDouble(r ->
                        Math.max(0.0, r.getPotentialWasteKwh()))
                .sum();

        double recommendationSavingsKwh = recommendations.stream()
                .mapToDouble(r -> r.getEstimatedSavingsKwh())
                .sum();

        double recommendationSavingsRupees = recommendations.stream()
                .mapToDouble(r -> r.getEstimatedSavingsRupees())
                .sum();

        model.addAttribute("selectedRoom", selectedRoom);
        model.addAttribute("appliances", appliances);
        model.addAttribute("bills", bills);
        model.addAttribute("monitoringRecords", monitoringRecords);
        model.addAttribute("recommendations", recommendations);

        model.addAttribute("applianceCount", appliances.size());
        model.addAttribute("billCount", bills.size());
        model.addAttribute("monitoringCount", monitoringRecords.size());

        model.addAttribute(
                "estimatedInventoryKwh", estimatedInventoryKwh);
        model.addAttribute("monitoredEnergyKwh", monitoredEnergyKwh);
        model.addAttribute("potentialWasteKwh", potentialWasteKwh);
        model.addAttribute(
                "recommendationSavingsKwh", recommendationSavingsKwh);
        model.addAttribute(
                "recommendationSavingsRupees", recommendationSavingsRupees);

        model.addAttribute(
                "reportReady",
                !appliances.isEmpty()
                        || !bills.isEmpty()
                        || !monitoringRecords.isEmpty()
        );

        return "audit-report";
    }
}
