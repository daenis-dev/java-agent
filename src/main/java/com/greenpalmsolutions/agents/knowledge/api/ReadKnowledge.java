package com.greenpalmsolutions.agents.knowledge.api;

public interface ReadKnowledge {

    String relevantContext(String question);

    String status();

    String writeAgentInterpretation(String title, String content);
}
