package com.swachhbot.backend.ai.planner;

import com.swachhbot.backend.ai.dto.StructuredCommand;
import com.swachhbot.backend.domain.Room;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, dependency-free intent parser.
 *
 * <p>Used when the LLM is disabled, unreachable, or returns unusable output.
 * It is intentionally simple but proves that the whole pipeline (plan →
 * validate → execute) works without any AI, which also makes it easy to test.
 */
@Component
public class RuleBasedPlanner {

    private static final Pattern TIMEFRAME = Pattern.compile(
            "(\\d+)\\s*(hour|day|week)s?", Pattern.CASE_INSENSITIVE);

    public StructuredCommand parse(String request, List<Room> rooms) {
        String text = request == null ? "" : request.toLowerCase(Locale.ROOT);
        List<String> mentioned = rooms.stream()
                .filter(r -> text.contains(r.getName().toLowerCase(Locale.ROOT).replace('_', ' ')))
                .map(Room::getName)
                .toList();

        boolean exclusive = containsAny(text, "don't", "do not", "avoid", "skip", "except", "not the");
        boolean recency = containsAny(text, "haven't", "hasn't", "not been cleaned", "not cleaned", "since");
        boolean dirtiest = containsAny(text, "dirtiest", "dirty", "dustiest", "worst");

        String action;
        List<String> roomsField = new ArrayList<>();
        List<String> excludedField = new ArrayList<>();

        if (exclusive && !mentioned.isEmpty()) {
            action = "CLEAN_EXCEPT";
            excludedField.addAll(mentioned);
        } else if (!mentioned.isEmpty()) {
            action = "CLEAN";
            roomsField.addAll(mentioned);
        } else {
            action = "CLEAN_ALL";
        }

        String timeframe = null;
        if (recency) {
            Matcher matcher = TIMEFRAME.matcher(text);
            if (matcher.find()) {
                timeframe = matcher.group(1) + " " + matcher.group(2);
            }
        }

        String priority = dirtiest ? "HIGH" : "NORMAL";
        String reasoning = "Parsed without an LLM (rule-based fallback).";

        return new StructuredCommand(action, roomsField, excludedField, priority, 1, timeframe, reasoning);
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
