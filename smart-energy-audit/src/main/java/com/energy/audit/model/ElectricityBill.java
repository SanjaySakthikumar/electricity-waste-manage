
package com.energy.audit.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ElectricityBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private double billedKwh;
    private double billAmount;

    public ElectricityBill() {
    }

    public Long getId() { return id; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public double getBilledKwh() { return billedKwh; }
    public double getBillAmount() { return billAmount; }

    public void setId(Long id) { this.id = id; }
    public void setPeriodStart(LocalDate date) {
        this.periodStart = date;
    }
    public void setPeriodEnd(LocalDate date) {
        this.periodEnd = date;
    }
    public void setBilledKwh(double kwh) {
        this.billedKwh = kwh;
    }
    public void setBillAmount(double amount) {
        this.billAmount = amount;
    }
}
