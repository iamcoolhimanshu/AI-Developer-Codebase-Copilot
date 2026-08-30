package com.codecopilot.patch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatchRequest {

	@NotBlank
	private String instruction;

	@NotNull
	private Long repositoryId;
}