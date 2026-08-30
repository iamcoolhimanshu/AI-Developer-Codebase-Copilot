package com.codecopilot.embedding;

import com.codecopilot.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

	private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);

	private final ObjectProvider<EmbeddingModel> embeddingModels;
	private final AppProperties properties;
	private final boolean keyPresent;

	public EmbeddingServiceImpl(ObjectProvider<EmbeddingModel> embeddingModels, AppProperties properties,
			@Value("${app.ai.api-key:}") String apiKey) {
		this.embeddingModels = embeddingModels;
		this.properties = properties;
		this.keyPresent = apiKey != null && !apiKey.isBlank();
	}

	@Override
	public float[] embed(String text) {
		List<float[]> all = embedAll(List.of(text));
		return all.isEmpty() ? null : all.get(0);
	}

	@Override
	public List<float[]> embedAll(List<String> texts) {
		if (aiEnabled()) {
			try {
				EmbeddingModel model = embeddingModels.getIfAvailable();
				if (model != null) {
					List<float[]> batch = model.embed(texts);
					return batch;
				}
			} catch (Exception e) {
				log.warn("Embedding call failed, falling back to stub: {}", e.getMessage());
			}
		}
		return texts.stream().map(this::stubEmbedding).toList();
	}

	private float[] stubEmbedding(String text) {
		int dim = properties.getIndexing().getEmbeddingDimension();
		long[] seeds = fnv(text);
		float[] v = new float[dim];
		for (int i = 0; i < dim; i++) {
			long mixed = seeds[(i % seeds.length)] + (long) i * 0x9E3779B97F4A7C15L;
			v[i] = (float) ((mixed >> 32) & 0xFF) / 128f - 1f;
		}
		return normalize(v);
	}

	private long[] fnv(String s) {
		long[] seeds = new long[8];
		long hash = 0xcbf29ce484222325L;
		for (int c = 0; c < s.length(); c++) {
			hash ^= s.charAt(c);
			hash *= 0x100000001b3L;
			seeds[c % 8] ^= hash;
		}
		return seeds;
	}

	private float[] normalize(float[] v) {
		double sum = 0;
		for (float f : v) {
			sum += f * f;
		}
		if (sum == 0) {
			return v;
		}
		double norm = Math.sqrt(sum);
		for (int i = 0; i < v.length; i++) {
			v[i] = (float) (v[i] / norm);
		}
		return v;
	}

	@Override
	public int dimension() {
		return properties.getIndexing().getEmbeddingDimension();
	}

	@Override
	public String modelName() {
		return keyPresent ? "xai-embedding" : "stub-hash";
	}

	@Override
	public boolean aiEnabled() {
		return keyPresent;
	}
}