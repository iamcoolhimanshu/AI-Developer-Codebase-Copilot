package com.codecopilot.embedding;

import java.util.List;

public interface EmbeddingService {

	float[] embed(String text);

	List<float[]> embedAll(List<String> texts);

	int dimension();

	String modelName();

	boolean aiEnabled();
}