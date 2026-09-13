package com.swachhbot.backend.service.vision;

import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.enums.ObjectCategory;
import com.swachhbot.backend.domain.enums.ObjectStatus;
import com.swachhbot.backend.dto.ObjectDtos.ObjectDto;
import com.swachhbot.backend.dto.ObjectDtos.ObjectUpsertRequest;
import com.swachhbot.backend.repository.HouseRepository;
import com.swachhbot.backend.repository.EnvironmentChangeRepository;
import com.swachhbot.backend.service.ObjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ObjectServiceTest {

    @Autowired
    private ObjectService objectService;

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private EnvironmentChangeRepository changeRepository;

    @Test
    void shouldRecordAddedChangeOnNewObject() {
        House house = houseRepository.save(House.builder().name("Test").width(1000).height(1000).build());
        UUID objId = UUID.randomUUID();
        ObjectUpsertRequest req = new ObjectUpsertRequest(
                objId, "chair", ObjectCategory.PERMANENT, ObjectStatus.NEW, "Kitchen",
                100, 100, 0.9, Instant.now(), Instant.now(), 1
        );

        objectService.upsert(house.getId(), req);

        var changes = changeRepository.findByHouseIdOrderByDetectedAtDesc(house.getId());
        assertThat(changes).isNotEmpty();
        assertThat(changes.get(0).getChangeType()).isEqualTo("ADDED");
    }

    @Test
    void shouldRecordMovedChangeOnSignificantMovement() {
        House house = houseRepository.save(House.builder().name("Test").width(1000).height(1000).build());
        UUID objId = UUID.randomUUID();
        
        // First observation
        objectService.upsert(house.getId(), new ObjectUpsertRequest(
                objId, "sofa", ObjectCategory.PERMANENT, ObjectStatus.KNOWN, "Living Room",
                100, 100, 0.95, Instant.now(), Instant.now(), 1
        ));

        // Second observation - moved by 500mm
        objectService.upsert(house.getId(), new ObjectUpsertRequest(
                objId, "sofa", ObjectCategory.PERMANENT, ObjectStatus.KNOWN, "Living Room",
                600, 100, 0.95, Instant.now(), Instant.now(), 2
        ));

        var changes = changeRepository.findByHouseIdOrderByDetectedAtDesc(house.getId());
        assertThat(changes).hasSize(2); // ADDED then MOVED
        assertThat(changes.get(0).getChangeType()).isEqualTo("MOVED");
        assertThat(changes.get(0).getDescription()).contains("moved 500.0 mm");
    }
}
