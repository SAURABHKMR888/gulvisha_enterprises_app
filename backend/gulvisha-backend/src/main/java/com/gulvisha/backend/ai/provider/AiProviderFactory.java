package com.gulvisha.backend.ai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the correct {@link AiProvider} implementation by name.
 * New providers are auto-registered by adding them as Spring beans.
 */
@Component
public class AiProviderFactory {

    private final Map<String, AiProvider> providers;

    public AiProviderFactory(List<AiProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(AiProvider::getProviderName, Function.identity()));
    }

    public AiProvider getProvider(String name) {
        AiProvider provider = providers.get(name);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown AI provider: " + name);
        }
        return provider;
    }

    public List<String> availableProviders() {
        return providers.keySet().stream().sorted().toList();
    }
}
