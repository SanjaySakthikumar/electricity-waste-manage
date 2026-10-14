
package com.energy.audit.repository;

import com.energy.audit.model.Appliance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplianceRepository
        extends JpaRepository<Appliance, Long> {
}
