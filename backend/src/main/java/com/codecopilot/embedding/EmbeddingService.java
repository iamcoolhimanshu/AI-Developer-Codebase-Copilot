package com.codecopilot.embedding;

import java.util.List;

/**
 * Provider-isolated embedding abstraction. Real embedding calls go through
 * Spring AI (OpenAI-compatible endpoint); a deterministic stub is used when no
 * API key is configured so the application remains runnable offline.
 */
public interface EmbeddingService {

    float[] embed(String text);

    List<float[]> embedAll(List<String> texts);

    int dimension();

    String modelName();

    boolean aiEnabled();
}