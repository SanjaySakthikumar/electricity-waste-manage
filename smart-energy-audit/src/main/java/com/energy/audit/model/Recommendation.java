
package com.energy.audit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    private String description;

    @Min(0)
    private double expectedMonthlySavingsKwh;

    @Min(0)
    private double implementationCost;

    public Recommendation() {
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getExpectedMonthlySavingsKwh() {
        return expectedMonthlySavingsKwh;
    }
    public double getImplementationCost() {
        return implementationCost;
    }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setExpectedMonthlySavingsKwh(double kwh) {
        this.expectedMonthlySavingsKwh = kwh;
    }
    public void setImplementationCost(double cost) {
        this.implementationCost = cost;
    }
}
