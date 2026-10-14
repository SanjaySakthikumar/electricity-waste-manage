
package com.energy.audit.service;

import com.energy.audit.model.Appliance;
import com.energy.audit.model.ElectricityBill;
import com.energy.audit.repository.ApplianceRepository;
import com.energy.audit.repository.ElectricityBillRepository;

import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EnergyAnalysisService {

    private final ApplianceRepository applianceRepository;
    private final ElectricityBillRepository billRepository;

    public EnergyAnalysisService(
            ApplianceRepository applianceRepository,
            ElectricityBillRepository billRepository) {
        this.applianceRepository = applianceRepository;
        this.billRepository = billRepository;
    }

    public List<Appliance> getAppliances() {
        return applianceRepository.findAll();
    }

    public double getEstimatedMonthlyKwh() {
        return getAppliances().stream()
                .mapToDouble(Appliance::getMonthlyKwh)
                .sum();
    }

    public ElectricityBill getLatestBill() {
        return billRepository.findTopByOrderByPeriodEndDesc()
                .orElse(null);
    }

    public double getEstimatedPeriodKwh(ElectricityBill bill) {
        if (bill == null
                || bill.getPeriodStart() == null
                || bill.getPeriodEnd() == null) {
            return 0.0;
        }

        long days = ChronoUnit.DAYS.between(
                bill.getPeriodStart(),
                bill.getPeriodEnd()) + 1;

        if (days <= 0) {
            return 0.0;
        }

        return getEstimatedMonthlyKwh() * days / 30.0;
    }

    public double getUnexplainedKwh(ElectricityBill bill) {
        if (bill == null) {
            return 0.0;
        }

        return bill.getBilledKwh()
                - getEstimatedPeriodKwh(bill);
    }

    public double getAverageRatePerKwh(ElectricityBill bill) {
        if (bill == null || bill.getBilledKwh() <= 0) {
            return 0.0;
        }

        return bill.getBillAmount() / bill.getBilledKwh();
    }

    public double getEstimatedCost(double kwh, ElectricityBill bill) {
        if (kwh <= 0 || bill == null) {
            return 0.0;
        }

        return kwh * getAverageRatePerKwh(bill);
    }

    public double getEnergyIntensityKwhPerSqm(double floorAreaSqm) {
        if (floorAreaSqm <= 0) {
            return 0.0;
        }

        return getEstimatedMonthlyKwh() / floorAreaSqm;
    }

    public double getConsumptionDifferencePercent(ElectricityBill bill) {
        if (bill == null || bill.getBilledKwh() <= 0) {
            return 0.0;
        }

        double estimated = getEstimatedPeriodKwh(bill);

        return ((bill.getBilledKwh() - estimated)
                / bill.getBilledKwh()) * 100.0;
    }
}
