
package com.energy.audit.controller;

import com.energy.audit.model.Appliance;
import com.energy.audit.repository.ApplianceRepository;
import com.energy.audit.service.MonthlyEnergyReportService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Controller
public class MonthlyReportController {

    private final MonthlyEnergyReportService reportService;
    private final ApplianceRepository applianceRepository;

    public MonthlyReportController(
            MonthlyEnergyReportService reportService,
            ApplianceRepository applianceRepository) {
        this.reportService = reportService;
        this.applianceRepository = applianceRepository;
    }

    @GetMapping("/report")
    public String showReport(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String room,
            Model model) {

        YearMonth selectedMonth;

        try {
            if (month == null || month.isBlank()) {
                selectedMonth = YearMonth.now();
            } else {
                selectedMonth = YearMonth.parse(month);
            }
        } catch (DateTimeParseException exception) {
            selectedMonth = YearMonth.now();
            model.addAttribute(
                    "monthError",
                    "Invalid month. Showing the current month.");
        }

        List<String> rooms = applianceRepository.findAll()
                .stream()
                .map(Appliance::getRoom)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        String selectedRoom = room;

        if (selectedRoom == null || selectedRoom.isBlank()) {

            selectedRoom = rooms.isEmpty() ? null : rooms.get(0);

        } else {

            String requestedRoom = selectedRoom.trim();

            boolean roomExists = rooms.stream()
                    .anyMatch(
                            roomName ->
                                    roomName.equalsIgnoreCase(requestedRoom));

            if (!roomExists) {
                selectedRoom = rooms.isEmpty() ? null : rooms.get(0);
            } else {
                selectedRoom = requestedRoom;
            }
        }

        model.addAttribute("rooms", rooms);
        model.addAttribute("selectedRoom", selectedRoom);
        model.addAttribute("selectedMonth", selectedMonth.toString());

        if (selectedRoom == null) {
            model.addAttribute(
                    "reportError",
                    "Register appliances with a room name before generating a report.");
            return "monthly-report";
        }

        Map<String, Object> report =
                reportService.generateReport(selectedMonth, selectedRoom);

        model.addAllAttributes(report);

        // Preserve the selected values after adding report attributes.
        model.addAttribute("rooms", rooms);
        model.addAttribute("selectedRoom", selectedRoom);
        model.addAttribute("selectedMonth", selectedMonth.toString());

        return "monthly-report";
    }
}
