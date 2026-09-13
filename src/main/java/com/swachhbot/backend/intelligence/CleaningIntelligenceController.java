package com.swachhbot.backend.intelligence;

import com.swachhbot.backend.intelligence.dto.IntelligenceDtos.HouseCleaningStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/intelligence")
@RequiredArgsConstructor
public class CleaningIntelligenceController {

    private final CleaningIntelligenceService intelligenceService;

    @GetMapping("/strategy")
    public HouseCleaningStrategy getStrategy(@RequestParam UUID houseId) {
        return intelligenceService.getStrategy(houseId);
    }
}
