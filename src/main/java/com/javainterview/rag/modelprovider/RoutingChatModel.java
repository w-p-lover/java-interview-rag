package com.javainterview.rag.modelprovider;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

public class RoutingChatModel implements ChatModel {
    private final ModelProviderRegistry registry;

    public RoutingChatModel(ModelProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return registry.callChat(model -> model.call(prompt));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return registry.streamChat(prompt);
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return registry.callChat(ChatModel::getDefaultOptions);
    }
}
