
package com.energy.audit.controller;

import com.energy.audit.model.Appliance;
import com.energy.audit.model.MonitoringRecord;
import com.energy.audit.repository.ApplianceRepository;
import com.energy.audit.repository.BuildingProfileRepository;
import com.energy.audit.repository.MonitoringRecordRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final BuildingProfileRepository buildingRepository;
    private final ApplianceRepository applianceRepository;
    private final MonitoringRecordRepository monitoringRepository;

    public DashboardController(
            BuildingProfileRepository buildingRepository,
            ApplianceRepository applianceRepository,
            MonitoringRecordRepository monitoringRepository) {
        this.buildingRepository = buildingRepository;
        this.applianceRepository = applianceRepository;
        this.monitoringRepository = monitoringRepository;
    }

    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) String room,
            Model model) {

        // Uses the existing BuildingProfile as the setup-completion check.
        boolean roomReady = buildingRepository.count() > 0;

        List<Appliance> allAppliances = applianceRepository.findAll();

        List<String> rooms = allAppliances.stream()
                .map(Appliance::getRoom)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        String selectedRoom = room;

        // Validate the requested room without capturing a mutable variable
        // inside a lambda.
        if (selectedRoom == null || selectedRoom.isBlank()) {

            selectedRoom = rooms.isEmpty() ? null : rooms.get(0);

        } else {

            String requestedRoom = selectedRoom.trim();

            boolean roomExists = rooms.stream()
                    .anyMatch(
                            roomName ->
                                    roomName.equalsIgnoreCase(requestedRoom));

            if (!roomExists) {
                selectedRoom = rooms.isEmpty() ? null : rooms.get(0);
            } else {
                selectedRoom = requestedRoom;
            }
        }

        final String roomKey =
                selectedRoom == null ? "" : selectedRoom.trim();

        List<Appliance> roomAppliances = allAppliances.stream()
                .filter(appliance -> appliance.getRoom() != null)
                .filter(appliance ->
                        appliance.getRoom().trim()
                                .equalsIgnoreCase(roomKey))
                .toList();

        boolean appliancesReady =
                roomReady && !roomAppliances.isEmpty();

        Set<String> registeredNames = roomAppliances.stream()
                .map(Appliance::getName)
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        YearMonth currentMonth = YearMonth.now();

        LocalDateTime monthStart =
                currentMonth.atDay(1).atStartOfDay();

        LocalDateTime nextMonthStart =
                currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<MonitoringRecord> validRecords =
                monitoringRepository.findAll().stream()

                        .filter(record -> record.getRoom() != null)
                        .filter(record ->
                                record.getRoom().trim()
                                        .equalsIgnoreCase(roomKey))

                        .filter(record ->
                                record.getApplianceName() != null)
                        .filter(record ->
                                registeredNames.contains(
                                        record.getApplianceName()
                                                .trim()
                                                .toLowerCase(Locale.ROOT)))

                        .filter(record ->
                                record.getDeviceOnStart() != null)
                        .filter(record ->
                                record.getDeviceOnEnd() != null)
                        .filter(record ->
                                record.getRoomEmptyStart() != null)
                        .filter(record ->
                                record.getRoomEmptyEnd() != null)

                        .filter(record ->
                                Double.isFinite(record.getPowerWatts()))
                        .filter(record -> record.getPowerWatts() > 0)

                        .filter(record ->
                                record.getDeviceOnEnd()
                                        .isAfter(record.getDeviceOnStart()))
                        .filter(record ->
                                record.getRoomEmptyEnd()
                                        .isAfter(record.getRoomEmptyStart()))

                        // Keep only ON intervals overlapping the current month.
                        .filter(record ->
                                record.getDeviceOnStart()
                                        .isBefore(nextMonthStart))
                        .filter(record ->
                                record.getDeviceOnEnd()
                                        .isAfter(monthStart))

                        .toList();

        boolean monitoringReady =
                appliancesReady && !validRecords.isEmpty();

        boolean reportReady = monitoringReady;

        model.addAttribute("roomReady", roomReady);
        model.addAttribute("appliancesReady", appliancesReady);
        model.addAttribute("baselineReady", appliancesReady);
        model.addAttribute("monitoringReady", monitoringReady);
        model.addAttribute("reportReady", reportReady);

        model.addAttribute("rooms", rooms);
        model.addAttribute("selectedRoom", selectedRoom);

        model.addAttribute("roomCount", rooms.size());
        model.addAttribute("applianceCount", roomAppliances.size());
        model.addAttribute("monitoringRecordCount", validRecords.size());

        return "dashboard";
    }
}
