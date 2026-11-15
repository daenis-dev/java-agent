package com.greenpalmsolutions.agents.conversation.api.model;

import jakarta.validation.constraints.NotBlank;

public record InterpretationRequest(
        @NotBlank String conversationId,
        @NotBlank String title,
        @NotBlank String question) {
}
