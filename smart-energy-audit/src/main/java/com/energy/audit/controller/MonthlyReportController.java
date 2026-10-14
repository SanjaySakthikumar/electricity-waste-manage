
package com.energy.audit.controller;

import com.energy.audit.service.MonthlyEnergyReportService;
import com.energy.audit.service.MonthlyEnergyReportService.MonthlyReport;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;

@Controller
public class MonthlyReportController {

    private final MonthlyEnergyReportService monthlyEnergyReportService;

    public MonthlyReportController(
            MonthlyEnergyReportService monthlyEnergyReportService) {
        this.monthlyEnergyReportService = monthlyEnergyReportService;
    }

    @GetMapping("/monthly-report")
    public String showMonthlyReport(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String room,
            Model model) {

        YearMonth selectedMonth;

        try {
            selectedMonth = (month == null || month.isBlank())
                    ? YearMonth.now()
                    : YearMonth.parse(month);
        } catch (Exception exception) {
            selectedMonth = YearMonth.now();
            model.addAttribute(
                    "inputWarning",
                    "Invalid month supplied. The current month is displayed."
            );
        }

        MonthlyReport report =
                monthlyEnergyReportService.generateReport(
                        selectedMonth, room
                );

        model.addAttribute("report", report);
        model.addAttribute("selectedMonth", selectedMonth.toString());
        model.addAttribute("selectedRoom", room == null ? "" : room);
        model.addAttribute("inputWarning",
                model.containsAttribute("inputWarning")
                        ? model.getAttribute("inputWarning")
                        : null);

        return "monthly-report";
    }
}
