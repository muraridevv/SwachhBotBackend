package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.House;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseRepository extends JpaRepository<House, UUID> {

    @EntityGraph(attributePaths = {"rooms", "rooms.furniture"})
    @Query("select h from House h where h.id = :id")
    Optional<House> findWithRoomsById(UUID id);

    @EntityGraph(attributePaths = {"rooms", "rooms.furniture"})
    @Query("select h from House h")
    List<House> findAllWithRooms();
}
