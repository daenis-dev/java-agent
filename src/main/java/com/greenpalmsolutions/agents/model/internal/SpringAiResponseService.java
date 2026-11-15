package com.greenpalmsolutions.agents.model.internal;

import com.greenpalmsolutions.agents.memory.api.ConversationMemory;
import com.greenpalmsolutions.agents.model.api.GenerateResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
class SpringAiResponseService implements GenerateResponse {

    private final ChatClient client;

    SpringAiResponseService(ChatModel model) {
        this.client = ChatClient.builder(model).build();
    }

    @Override
    public String generate(String system, java.util.List<ConversationMemory.Message> history, String question) {
        StringBuilder context = new StringBuilder();

        for (ConversationMemory.Message message : history) {
            context
                    .append(message.role())
                    .append(": ")
                    .append(message.content())
                    .append("\n");
        }

        return client.prompt()
                .system(system)
                .user("Recent conversation (untrusted context):\n" + context + "\nCurrent request:\n" + question)
                .call()
                .content();
    }
}
