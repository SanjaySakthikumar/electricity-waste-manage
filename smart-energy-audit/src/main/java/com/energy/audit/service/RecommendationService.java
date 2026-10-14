
package com.energy.audit.service;

import com.energy.audit.model.ElectricityBill;
import com.energy.audit.model.Recommendation;
import com.energy.audit.repository.ElectricityBillRepository;
import com.energy.audit.repository.RecommendationRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final ElectricityBillRepository billRepository;

    public RecommendationService(
            RecommendationRepository recommendationRepository,
            ElectricityBillRepository billRepository) {
        this.recommendationRepository = recommendationRepository;
        this.billRepository = billRepository;
    }

    public List<Recommendation> findAll() {
        return recommendationRepository.findAll();
    }

    public Recommendation save(Recommendation recommendation) {
        return recommendationRepository.save(recommendation);
    }

    public double getEstimatedMonthlySavingsKwh() {
        return findAll().stream()
                .mapToDouble(
                    Recommendation::getExpectedMonthlySavingsKwh)
                .sum();
    }

    public double getEstimatedMonthlySavingsRupees() {
        ElectricityBill bill = billRepository
                .findTopByOrderByPeriodEndDesc()
                .orElse(null);

        if (bill == null || bill.getBilledKwh() <= 0) {
            return 0;
        }

        double averageRate =
                bill.getBillAmount() / bill.getBilledKwh();

        return getEstimatedMonthlySavingsKwh() * averageRate;
    }

    public double getTotalImplementationCost() {
        return findAll().stream()
                .mapToDouble(Recommendation::getImplementationCost)
                .sum();
    }

    public double getEstimatedPaybackMonths() {
        double monthlySavings = getEstimatedMonthlySavingsRupees();
        double cost = getTotalImplementationCost();

        if (monthlySavings <= 0 || cost <= 0) {
            return 0;
        }

        return cost / monthlySavings;
    }
}
