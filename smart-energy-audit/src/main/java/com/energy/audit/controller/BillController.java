package com.energy.audit.controller;

import com.energy.audit.model.Appliance;
import com.energy.audit.model.ElectricityBill;
import com.energy.audit.repository.ApplianceRepository;
import com.energy.audit.repository.ElectricityBillRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Controller
public class BillController {

    private final ElectricityBillRepository billRepository;
    private final ApplianceRepository applianceRepository;

    @Value("${ecopulse.tariff-per-kwh:8.0}")
    private double tariffPerKwh;

    public BillController(
            ElectricityBillRepository billRepository,
            ApplianceRepository applianceRepository) {

        this.billRepository = billRepository;
        this.applianceRepository = applianceRepository;
    }

    @GetMapping("/bills")
    public String showBills(Model model) {

        model.addAttribute("bill", new ElectricityBill());
        model.addAttribute("bills", billRepository.findAll());

        return "bills";
    }

    /*
     * Generate an estimated bill for the previous calendar month.
     * The estimate is displayed but is NOT saved as an actual bill.
     */
    @GetMapping("/bills/estimate")
    public String generatePreviousMonthEstimate(Model model) {

        YearMonth previousMonth = YearMonth.now().minusMonths(1);

        List<ApplianceEstimate> applianceEstimates =
                new ArrayList<>();

        double totalKwh = 0.0;

        List<Appliance> appliances = applianceRepository.findAll();

        for (Appliance appliance : appliances) {

            double energyKwh = appliance.getMonthlyKwh();

            if (!Double.isFinite(energyKwh) || energyKwh < 0) {
                energyKwh = 0.0;
            }

            totalKwh += energyKwh;

            applianceEstimates.add(
                    new ApplianceEstimate(
                            appliance.getName(),
                            appliance.getRoom(),
                            appliance.getRatedPowerWatts(),
                            appliance.getQuantity(),
                            appliance.getHoursPerDay(),
                            appliance.getDaysPerMonth(),
                            energyKwh,
                            energyKwh * tariffPerKwh
                    )
            );
        }

        double estimatedAmount = totalKwh * tariffPerKwh;

        model.addAttribute("bill", new ElectricityBill());
        model.addAttribute("bills", billRepository.findAll());

        model.addAttribute("estimateGenerated", true);
        model.addAttribute("estimatedMonth", previousMonth);
        model.addAttribute("estimateStart", previousMonth.atDay(1));
        model.addAttribute(
                "estimateEnd",
                previousMonth.atEndOfMonth()
        );

        model.addAttribute("applianceEstimates", applianceEstimates);
        model.addAttribute("estimatedTotalKwh", totalKwh);
        model.addAttribute("estimatedAmount", estimatedAmount);
        model.addAttribute("tariffPerKwh", tariffPerKwh);
        model.addAttribute("estimatedApplianceCount", appliances.size());

        if (appliances.isEmpty()) {
            model.addAttribute(
                    "estimateNotice",
                    "No appliances are registered yet. Add appliances and their working schedules before generating an estimate."
            );
        }

        return "bills";
    }

    @PostMapping("/bills")
    public String saveBill(
            @ModelAttribute ElectricityBill bill,
            RedirectAttributes redirectAttributes) {

        if (bill.getPeriodStart() == null
                || bill.getPeriodEnd() == null
                || bill.getPeriodEnd().isBefore(bill.getPeriodStart())
                || !Double.isFinite(bill.getBilledKwh())
                || !Double.isFinite(bill.getBillAmount())
                || bill.getBilledKwh() <= 0
                || bill.getBillAmount() < 0) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Enter valid billing dates, positive kWh and a non-negative bill amount."
            );

            return "redirect:/bills";
        }

        billRepository.save(bill);

        redirectAttributes.addFlashAttribute(
                "message",
                "Electricity bill saved successfully!"
        );

        return "redirect:/bills";
    }

    public static class ApplianceEstimate {

        private final String name;
        private final String room;
        private final double ratedPowerWatts;
        private final int quantity;
        private final double hoursPerDay;
        private final int daysPerMonth;
        private final double energyKwh;
        private final double estimatedCost;

        public ApplianceEstimate(
                String name,
                String room,
                double ratedPowerWatts,
                int quantity,
                double hoursPerDay,
                int daysPerMonth,
                double energyKwh,
                double estimatedCost) {

            this.name = name;
            this.room = room;
            this.ratedPowerWatts = ratedPowerWatts;
            this.quantity = quantity;
            this.hoursPerDay = hoursPerDay;
            this.daysPerMonth = daysPerMonth;
            this.energyKwh = energyKwh;
            this.estimatedCost = estimatedCost;
        }

        public String getName() {
            return name;
        }

        public String getRoom() {
            return room;
        }

        public double getRatedPowerWatts() {
            return ratedPowerWatts;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getHoursPerDay() {
            return hoursPerDay;
        }

        public int getDaysPerMonth() {
            return daysPerMonth;
        }

        public double getEnergyKwh() {
            return energyKwh;
        }

        public double getEstimatedCost() {
            return estimatedCost;
        }
    }
}