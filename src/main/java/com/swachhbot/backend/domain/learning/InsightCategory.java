package com.swachhbot.backend.domain.learning;

/** What kind of thing the robot has learned. */
public enum InsightCategory {
    /** An area that repeatedly needs extra attention. */
    DIRTY_AREA,
    /** A spot where the robot repeatedly gets stuck / blocked. */
    BLOCKED_AREA,
    /** An object that keeps reappearing but is not part of the room. */
    TEMPORARY_OBJECT,
    /** Large, persistent changes to obstacles (an object was moved). */
    FURNITURE_CHANGE,
    /** Smaller changes between map snapshots. */
    MAP_CHANGE,
    /** How long / how many passes cleaning actually takes. */
    CLEANING_PATTERN,
    /** A room that has stayed clean over many sessions. */
    ROOM_CLEANLINESS
}
