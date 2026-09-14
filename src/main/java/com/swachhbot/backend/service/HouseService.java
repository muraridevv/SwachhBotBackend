package com.swachhbot.backend.service;

import com.swachhbot.backend.domain.Furniture;
import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.dto.HouseDtos.*;
import com.swachhbot.backend.repository.HouseRepository;
import com.swachhbot.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HouseService {

    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    public List<HouseDto> findAll() {
        return houseRepository.findAllWithRooms().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public HouseDto findById(UUID id) {
        House house = houseRepository.findWithRoomsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("House not found: " + id));
        return toDto(house);
    }

    public HouseDto create(HouseRequest request) {
        if (request.id() != null) {
            var existing = houseRepository.findById(request.id());
            if (existing.isPresent()) {
                return toDto(existing.get());
            }
        }
        
        House house = House.builder()
                .id(request.id())
                .name(request.name())
                .width(request.width())
                .height(request.height())
                .build();

        try {
            return toDto(houseRepository.saveAndFlush(house));
        } catch (org.springframework.dao.DataIntegrityViolationException | org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            // Handle concurrent creation gracefully
            return houseRepository.findById(request.id())
                    .map(this::toDto)
                    .orElseThrow(() -> e);
        }
    }

    public HouseDto update(UUID id, HouseRequest request) {
        House house = houseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("House not found: " + id));
        house.setName(request.name());
        house.setWidth(request.width());
        house.setHeight(request.height());
        return toDto(houseRepository.save(house));
    }

    public void delete(UUID id) {
        if (!houseRepository.existsById(id)) {
            throw new ResourceNotFoundException("House not found: " + id);
        }
        houseRepository.deleteById(id);
    }

    public RoomDto addRoom(UUID houseId, RoomRequest request) {
        if (request.id() != null) {
            var existing = roomRepository.findById(request.id());
            if (existing.isPresent()) {
                return toDto(existing.get());
            }
        }

        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("House not found: " + houseId));
        Room room = Room.builder()
                .id(request.id())
                .house(house)
                .name(request.name())
                .x(request.x())
                .y(request.y())
                .width(request.width())
                .height(request.height())
                .build();
        
        try {
            return toDto(roomRepository.saveAndFlush(room));
        } catch (org.springframework.dao.DataIntegrityViolationException | org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            return roomRepository.findById(request.id())
                    .map(this::toDto)
                    .orElseThrow(() -> e);
        }
    }

    // ----- Mapping -----

    private HouseDto toDto(House house) {
        return new HouseDto(
                house.getId(),
                house.getName(),
                house.getWidth(),
                house.getHeight(),
                house.getRooms().stream().map(this::toDto).toList()
        );
    }

    private RoomDto toDto(Room room) {
        return new RoomDto(
                room.getId(),
                room.getName(),
                room.getX(),
                room.getY(),
                room.getWidth(),
                room.getHeight(),
                room.getFurniture().stream().map(this::toDto).toList()
        );
    }

    private FurnitureDto toDto(Furniture furniture) {
        return new FurnitureDto(
                furniture.getId(),
                furniture.getType(),
                furniture.getX(),
                furniture.getY(),
                furniture.getWidth(),
                furniture.getHeight(),
                furniture.getRotationDeg()
        );
    }
}
