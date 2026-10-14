
package com.energy.audit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.energy.audit.model.ElectricityBill;

public interface ElectricityBillRepository
        extends JpaRepository<ElectricityBill, Long> {

    Optional<ElectricityBill>
        findTopByOrderByPeriodEndDesc();
}
