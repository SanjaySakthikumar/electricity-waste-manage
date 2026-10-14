
package com.energy.audit.repository;

import com.energy.audit.model.BuildingProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingProfileRepository
        extends JpaRepository<BuildingProfile, Long> {

    Optional<BuildingProfile> findTopByOrderByIdDesc();
}
