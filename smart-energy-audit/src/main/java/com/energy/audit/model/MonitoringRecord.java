
package com.energy.audit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
public class MonitoringRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applianceName;
    private String room;
    private double powerWatts;

    private LocalDateTime deviceOnStart;
    private LocalDateTime deviceOnEnd;

    private LocalDateTime roomEmptyStart;
    private LocalDateTime roomEmptyEnd;

    public MonitoringRecord() {
    }

    @Transient
    public double getOverlapHours() {
        if (!validIntervals()) {
            return 0;
        }

        LocalDateTime start = deviceOnStart.isAfter(roomEmptyStart)
                ? deviceOnStart : roomEmptyStart;

        LocalDateTime end = deviceOnEnd.isBefore(roomEmptyEnd)
                ? deviceOnEnd : roomEmptyEnd;

        if (!end.isAfter(start)) {
            return 0;
        }

        return Duration.between(start, end).toMinutes() / 60.0;
    }

    @Transient
    public double getPotentialWasteKwh() {
        return powerWatts * getOverlapHours() / 1000.0;
    }

    private boolean validIntervals() {
        return deviceOnStart != null
                && deviceOnEnd != null
                && roomEmptyStart != null
                && roomEmptyEnd != null
                && deviceOnEnd.isAfter(deviceOnStart)
                && roomEmptyEnd.isAfter(roomEmptyStart)
                && Double.isFinite(powerWatts)
                && powerWatts > 0;
    }

    public Long getId() { return id; }
    public String getApplianceName() { return applianceName; }
    public String getRoom() { return room; }
    public double getPowerWatts() { return powerWatts; }
    public LocalDateTime getDeviceOnStart() { return deviceOnStart; }
    public LocalDateTime getDeviceOnEnd() { return deviceOnEnd; }
    public LocalDateTime getRoomEmptyStart() { return roomEmptyStart; }
    public LocalDateTime getRoomEmptyEnd() { return roomEmptyEnd; }

    public void setId(Long id) { this.id = id; }
    public void setApplianceName(String name) { this.applianceName = name; }
    public void setRoom(String room) { this.room = room; }
    public void setPowerWatts(double watts) { this.powerWatts = watts; }
    public void setDeviceOnStart(LocalDateTime start) { this.deviceOnStart = start; }
    public void setDeviceOnEnd(LocalDateTime end) { this.deviceOnEnd = end; }
    public void setRoomEmptyStart(LocalDateTime start) { this.roomEmptyStart = start; }
    public void setRoomEmptyEnd(LocalDateTime end) { this.roomEmptyEnd = end; }
}
