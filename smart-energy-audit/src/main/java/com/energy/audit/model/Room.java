
package com.energy.audit.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String roomName;

    @Column(nullable = false, length = 50)
    private String roomType;

    @Column(nullable = false)
    private double areaSqm;

    public Room() {
    }

    public Room(String roomName, String roomType, double areaSqm) {
        this.roomName = roomName;
        this.roomType = roomType;
        this.areaSqm = areaSqm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public double getAreaSqm() {
        return areaSqm;
    }

    public void setAreaSqm(double areaSqm) {
        this.areaSqm = areaSqm;
    }
}
