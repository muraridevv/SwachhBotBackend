package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.ProblemArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProblemAreaRepository extends JpaRepository<ProblemArea, UUID> {

    List<ProblemArea> findByHouseIdOrderByFrequencyDesc(UUID houseId);
}
