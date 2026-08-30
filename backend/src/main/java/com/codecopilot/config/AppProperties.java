package com.codecopilot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private Storage storage = new Storage();
	private Indexing indexing = new Indexing();
	private Security security = new Security();
	private Github github = new Github();
	private Ai ai = new Ai();

	@Getter
	@Setter
	public static class Storage {
		private String root = "./data/repos";
	}

	@Getter
	@Setter
	public static class Indexing {
		private int maxFilesPerRepo = 20000;
		private long maxRepoSizeBytes = 500_000_000L;
		private int embeddingDimension = 1024;
		private Chunk chunk = new Chunk();
	}

	@Getter
	@Setter
	public static class Chunk {
		private int maxChars = 1500;
		private int overlap = 120;
	}

	@Getter
	@Setter
	public static class Security {
		private String jwtSecret = "dev-only-secret-change-me-in-production-0123456789abcdef";
		private long jwtExpirationMs = 86_400_000L;
		private long refreshExpirationMs = 2_592_000_000L;
	}

	@Getter
	@Setter
	public static class Github {
		private String token;
	}

	@Getter
	@Setter
	public static class Ai {
		private int maxAgentToolCalls = 12;
		private int maxContextChunks = 14;
		private boolean requireKey = true;
	}
}