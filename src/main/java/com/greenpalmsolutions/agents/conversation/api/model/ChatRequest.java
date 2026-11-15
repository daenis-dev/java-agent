package com.greenpalmsolutions.agents.conversation.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank @Size(max = 100) String conversationId,
        @NotBlank @Size(max = 12000) String message) {
}
