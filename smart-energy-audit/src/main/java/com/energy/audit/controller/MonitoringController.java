
package com.energy.audit.controller;

import com.energy.audit.model.MonitoringRecord;
import com.energy.audit.repository.MonitoringRecordRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class MonitoringController {

    private final MonitoringRecordRepository monitoringRecordRepository;

    public MonitoringController(
            MonitoringRecordRepository monitoringRecordRepository) {
        this.monitoringRecordRepository = monitoringRecordRepository;
    }

    @GetMapping("/monitoring")
    public String showMonitoring(Model model) {

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
            Model model) {

        if (monitoringRecord.getApplianceName() == null
                || monitoringRecord.getApplianceName().isBlank()
                || monitoringRecord.getRoom() == null
                || monitoringRecord.getRoom().isBlank()
                || monitoringRecord.getPowerWatts() <= 0
                || monitoringRecord.getDeviceOnStart() == null
                || monitoringRecord.getDeviceOnEnd() == null
                || monitoringRecord.getRoomEmptyStart() == null
                || monitoringRecord.getRoomEmptyEnd() == null) {

            model.addAttribute("errorMessage",
                    "Please enter all fields and a power value greater than zero.");

            return showMonitoring(model);
        }

        if (!monitoringRecord.getDeviceOnEnd()
                .isAfter(monitoringRecord.getDeviceOnStart())
                || !monitoringRecord.getRoomEmptyEnd()
                .isAfter(monitoringRecord.getRoomEmptyStart())) {

            model.addAttribute("errorMessage",
                    "End times must be later than their corresponding start times.");

            return showMonitoring(model);
        }

        monitoringRecordRepository.save(monitoringRecord);

        return "redirect:/monitoring";
    }

    @PostMapping("/monitoring/delete/{id}")
    public String deleteMonitoring(@PathVariable Long id) {
        monitoringRecordRepository.deleteById(id);
        return "redirect:/monitoring";
    }
}
