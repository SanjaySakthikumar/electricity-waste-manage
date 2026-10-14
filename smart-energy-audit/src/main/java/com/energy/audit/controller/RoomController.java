
package com.energy.audit.controller;

import com.energy.audit.model.Room;
import com.energy.audit.repository.RoomRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RoomController {

    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping("/rooms")
    public String showRooms(Model model) {
        model.addAttribute(
            "rooms",
            roomRepository.findAllByOrderByRoomNameAsc()
        );

        return "rooms";
    }

    @PostMapping("/rooms/save")
    public String saveRoom(
            @RequestParam String roomName,
            @RequestParam String roomType,
            @RequestParam double areaSqm,
            RedirectAttributes redirectAttributes) {

        String name = roomName.trim();
        String type = roomType.trim();

        if (name.isEmpty() || name.length() > 100) {
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Enter a valid room name."
            );
            return "redirect:/rooms";
        }

        if (!type.equals("Bedroom")
                && !type.equals("Office")
                && !type.equals("Classroom")
                && !type.equals("Living Room")
                && !type.equals("Other")) {

            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Select a valid room type."
            );
            return "redirect:/rooms";
        }

        if (!Double.isFinite(areaSqm)
                || areaSqm < 0.1
                || areaSqm > 100000) {

            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Room area must be between 0.1 and 100000 m²."
            );
            return "redirect:/rooms";
        }

        if (roomRepository.existsByRoomNameIgnoreCase(name)) {
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "A room with this name already exists."
            );
            return "redirect:/rooms";
        }

        Room room = new Room();
        room.setRoomName(name);
        room.setRoomType(type);
        room.setAreaSqm(areaSqm);

        roomRepository.save(room);

        redirectAttributes.addFlashAttribute(
            "successMessage",
            "Room saved successfully."
        );

        return "redirect:/rooms";
    }
}
