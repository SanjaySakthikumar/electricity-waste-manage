
package com.energy.audit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "building_profiles")
public class BuildingProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String buildingName;

    @Column(nullable = false)
    private double floorAreaSqm;

    public BuildingProfile() {
    }

    public Long getId() {
        return id;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public double getFloorAreaSqm() {
        return floorAreaSqm;
    }

    public void setFloorAreaSqm(double floorAreaSqm) {
        this.floorAreaSqm = floorAreaSqm;
    }
}
