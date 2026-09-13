package com.swachhbot.backend.learning.support;

import com.swachhbot.backend.domain.Room;

import java.util.List;
import java.util.Optional;

/** Small deterministic helpers for mapping coordinates to rooms. */
public final class RoomLocator {

    private RoomLocator() {
    }

    public static Optional<Room> roomAt(List<Room> rooms, double x, double y) {
        return rooms.stream()
                .filter(r -> x >= r.getX() && x <= r.getX() + r.getWidth()
                        && y >= r.getY() && y <= r.getY() + r.getHeight())
                .findFirst();
    }

    public static String describeLocation(List<Room> rooms, double x, double y) {
        return roomAt(rooms, x, y)
                .map(r -> "the " + r.getName())
                .orElse("(%.0f, %.0f)".formatted(x, y));
    }

    /** Buckets a coordinate so nearby observations merge into one insight. */
    public static long bucket(double value, int size) {
        return (long) (Math.floor(value / size) * size);
    }
}
