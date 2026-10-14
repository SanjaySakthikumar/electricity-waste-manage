
package com.energy.audit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.energy.audit.model.ElectricityBill;
import com.energy.audit.repository.ElectricityBillRepository;

@Controller
public class BillController {

    private final ElectricityBillRepository repository;

    public BillController(ElectricityBillRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/bills")
    public String showBills(Model model) {
        model.addAttribute("bill", new ElectricityBill());
        model.addAttribute("bills", repository.findAll());
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

        repository.save(bill);

        redirectAttributes.addFlashAttribute(
                "message", "Electricity bill saved successfully!"
        );

        return "redirect:/bills";
    }
}
