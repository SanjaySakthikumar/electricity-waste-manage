
package com.energy.audit.repository;

import com.energy.audit.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNameIgnoreCase(String roomName);

    List<Room> findAllByOrderByRoomNameAsc();

    boolean existsByRoomNameIgnoreCase(String roomName);
}
