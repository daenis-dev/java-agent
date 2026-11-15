package com.greenpalmsolutions.agents.model.api;

import com.greenpalmsolutions.agents.memory.api.ConversationMemory;
import java.util.List;

public interface GenerateResponse {

    String generate(String system, List<ConversationMemory.Message> history, String question);
}
