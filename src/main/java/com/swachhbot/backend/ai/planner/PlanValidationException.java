package com.swachhbot.backend.ai.planner;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Raised when an AI-generated plan fails any safety or consistency check. */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PlanValidationException extends RuntimeException {
    public PlanValidationException(String message) {
        super(message);
    }
}
