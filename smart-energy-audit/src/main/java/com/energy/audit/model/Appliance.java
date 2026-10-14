
package com.energy.audit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
public class Appliance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Appliance name is required")
    private String name;

    @NotBlank(message = "Room name is required")
    private String room;

    @Positive(message = "Rated power must be greater than zero")
    private double ratedPowerWatts;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @PositiveOrZero(message = "Operating hours cannot be negative")
    private double hoursPerDay;

    @Min(value = 1, message = "Operating days must be at least 1")
    private int daysPerMonth;

    public Appliance() {
    }

    public double getMonthlyKwh() {
        if (!Double.isFinite(ratedPowerWatts)
                || ratedPowerWatts <= 0
                || quantity <= 0
                || !Double.isFinite(hoursPerDay)
                || hoursPerDay < 0
                || daysPerMonth <= 0) {
            return 0.0;
        }

        return (ratedPowerWatts * quantity
                * hoursPerDay * daysPerMonth) / 1000.0;
    }

    public double getEstimatedMonthlyCost(double ratePerKwh) {
        if (!Double.isFinite(ratePerKwh) || ratePerKwh < 0) {
            return 0.0;
        }

        return getMonthlyKwh() * ratePerKwh;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public double getRatedPowerWatts() {
        return ratedPowerWatts;
    }

    public void setRatedPowerWatts(double ratedPowerWatts) {
        this.ratedPowerWatts = ratedPowerWatts;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getHoursPerDay() {
        return hoursPerDay;
    }

    public void setHoursPerDay(double hoursPerDay) {
        this.hoursPerDay = hoursPerDay;
    }

    public int getDaysPerMonth() {
        return daysPerMonth;
    }

    public void setDaysPerMonth(int daysPerMonth) {
        this.daysPerMonth = daysPerMonth;
    }
}
