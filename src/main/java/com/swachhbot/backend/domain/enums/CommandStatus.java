package com.swachhbot.backend.domain.enums;

/** Delivery lifecycle of a dispatched command. */
public enum CommandStatus {
    PENDING,
    SENT,
    ACKNOWLEDGED,
    COMPLETED,
    FAILED
}
