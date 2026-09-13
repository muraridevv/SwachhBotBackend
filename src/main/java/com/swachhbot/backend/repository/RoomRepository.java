package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByHouseId(UUID houseId);
}
