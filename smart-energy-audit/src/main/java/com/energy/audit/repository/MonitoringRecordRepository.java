
package com.energy.audit.repository;

import com.energy.audit.model.MonitoringRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringRecordRepository
        extends JpaRepository<MonitoringRecord, Long> {
}
