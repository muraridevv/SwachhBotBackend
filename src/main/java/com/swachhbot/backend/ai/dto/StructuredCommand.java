package com.swachhbot.backend.ai.dto;

import java.util.List;

/**
 * The strict schema the LLM must fill in.
 *
 * <p><b>Safety:</b> every field is a high-level, textual or categorical value.
 * There is no way for the model to express a motor command, a speed, an angle or
 * a raw coordinate. Unknown / out-of-range values are rejected by the plan
 * builder and validator, never trusted.
 *
 * @param action        one of CLEAN, CLEAN_ALL, CLEAN_EXCEPT
 * @param rooms         room names mentioned by the user (may be free text)
 * @param excludedRooms room names to avoid
 * @param priority      LOW | NORMAL | HIGH | URGENT
 * @param passes        requested number of passes (clamped by the validator)
 * @param timeframe     e.g. "3 days", "a week" — resolved deterministically against history
 * @param reasoning     one short sentence explaining the interpretation
 */
public record StructuredCommand(
        String action,
        List<String> rooms,
        List<String> excludedRooms,
        String priority,
        Integer passes,
        String timeframe,
        String reasoning
) {
}
