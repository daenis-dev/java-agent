package com.greenpalmsolutions.agents.memory.api;

import java.util.List;

public interface ConversationMemory {

    record Message(long id, String conversationId, String role, String content, String createdAt) {
    }

    void append(String conversationId, String role, String content);

    List<Message> recent(String conversationId, int limit);

    List<String> conversations();
}
