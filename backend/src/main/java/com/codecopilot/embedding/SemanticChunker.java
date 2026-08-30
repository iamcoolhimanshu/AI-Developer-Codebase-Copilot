package com.codecopilot.embedding;

import com.codecopilot.parser.ParsedSourceFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SemanticChunker {

	public record ChunkSpec(String chunkType, String className, String methodName, int startLine, int endLine,
			String content) {
	}

	private final int maxChars;
	private final int overlap;

	public SemanticChunker(@Value("${app.indexing.chunk.max-chars:1500}") int maxChars,
			@Value("${app.indexing.chunk.overlap:120}") int overlap) {
		this.maxChars = Math.max(200, maxChars);
		this.overlap = Math.min(overlap, this.maxChars / 2);
	}

	public List<ChunkSpec> chunkJava(ParsedSourceFile parsed) {
		List<ChunkSpec> out = new ArrayList<>();
		for (ParsedSourceFile.ParsedType type : parsed.types()) {

			StringBuilder header = new StringBuilder();
			if (parsed.packageName() != null && !parsed.packageName().isBlank()) {
				header.append("package ").append(parsed.packageName()).append(";\n");
			}
			for (String imp : parsed.imports()) {
				header.append("import ").append(imp).append(";\n");
			}
			header.append(String.join(" ", type.modifiers())).append(' ').append(type.kind().name().toLowerCase())
					.append(' ').append(type.name());
			if (type.parentClass() != null) {
				header.append(" extends ").append(type.parentClass());
			}
			if (!type.interfaces().isEmpty()) {
				header.append(" implements ").append(String.join(", ", type.interfaces()));
			}
			if (!type.annotations().isEmpty()) {
				header.append("\nannotations: ").append(String.join(", ", type.annotations()));
			}
			if (!type.fields().isEmpty()) {
				header.append("\nfields:\n");
				for (ParsedSourceFile.ParsedField f : type.fields()) {
					header.append("  ").append(f.modifiers().isEmpty() ? "" : String.join(" ", f.modifiers()) + " ")
							.append(f.type()).append(' ').append(f.name()).append('\n');
				}
			}
			header.append("\nmethods: ");
			header.append(type.methods().stream().map(m -> m.name() + "()").distinct().toList());
			if (!header.isEmpty()) {
				out.add(new ChunkSpec("CLASS", type.name(), null, type.startLine(), type.endLine(), header.toString()));
			}

			for (ParsedSourceFile.ParsedMethod method : type.methods()) {
				String content = methodComment(method) + methodSignature(method) + "\n```java\n" + method.body()
						+ "\n```";
				out.add(new ChunkSpec("METHOD", type.name(), method.name(), method.startLine(), method.endLine(),
						content));
			}
		}
		return out;
	}

	public List<ChunkSpec> chunkText(String filePath, String content) {
		List<ChunkSpec> out = new ArrayList<>();
		if (content == null || content.isBlank()) {
			return out;
		}
		if (content.length() <= maxChars) {
			out.add(new ChunkSpec("FILE", null, null, 1, countLines(content), content));
			return out;
		}
		int start = 0;
		int line = 1;
		while (start < content.length()) {
			int end = Math.min(start + maxChars, content.length());
			String piece = content.substring(start, end);
			int lastNewline = piece.lastIndexOf('\n');
			if (end < content.length() && lastNewline > maxChars / 2) {
				end = start + lastNewline;
				piece = content.substring(start, end);
			}
			out.add(new ChunkSpec("FILE", null, null, line, line + countLines(piece), piece));
			line += countLines(piece);
			start = Math.max(end - overlap, start + 1);
			if (start >= content.length()) {
				break;
			}
		}
		return out;
	}

	private String methodComment(ParsedSourceFile.ParsedMethod method) {
		if (!method.annotations().isEmpty()) {
			return "@" + String.join(" @", method.annotations()) + " ";
		}
		return "";
	}

	private String methodSignature(ParsedSourceFile.ParsedMethod method) {
		String params = method.parameters().stream().map(p -> p.type() + " " + p.name()).reduce((a, b) -> a + ", " + b)
				.orElse("");
		if (method.constructor()) {
			return "constructor: " + params + " {";
		}
		return String.join(" ", method.modifiers()) + " " + method.returnType() + " " + method.name() + "(" + params
				+ ") {";
	}

	private int countLines(String s) {
		int n = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '\n') {
				n++;
			}
		}
		return n + 1;
	}
}