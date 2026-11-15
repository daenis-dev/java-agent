package com.greenpalmsolutions.agents.conversation.api.controller;

import com.greenpalmsolutions.agents.conversation.api.model.ChatRequest;
import com.greenpalmsolutions.agents.conversation.api.model.InterpretationRequest;
import com.greenpalmsolutions.agents.conversation.internal.ConversationService;
import com.greenpalmsolutions.agents.knowledge.api.ReadKnowledge;
import com.greenpalmsolutions.agents.memory.api.ConversationMemory;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversation;
    private final ConversationMemory memory;
    private final ReadKnowledge knowledge;

    public record Answer(String answer) {
    }

    @PostMapping("/chat")
    public Answer chat(@Valid @RequestBody ChatRequest request) {
        return new Answer(conversation.chat(request));
    }

    @PostMapping("/interpret")
    public Answer interpret(@Valid @RequestBody ChatRequest request) {
        return new Answer(conversation.interpret(request));
    }

    @GetMapping("/conversations")
    public List<String> conversations() {
        return memory.conversations();
    }

    @GetMapping("/conversations/{id}")
    public List<ConversationMemory.Message> history(@PathVariable String id) {
        return memory.recent(id, 200);
    }

    @GetMapping("/knowledge/status")
    public Map<String, String> knowledgeStatus() {
        return Map.of("status", knowledge.status());
    }

    @PostMapping("/knowledge/interpretations")
    public Map<String, String> save(@Valid @RequestBody InterpretationRequest request) {
        String generated = conversation.interpret(new ChatRequest(request.conversationId(), request.question()));
        String path = knowledge.writeAgentInterpretation(request.title(), generated);
        return Map.of(
                "path", path,
                "content", generated);
    }
}
