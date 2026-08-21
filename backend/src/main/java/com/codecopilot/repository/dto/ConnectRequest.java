package com.codecopilot.repository.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConnectRequest {

    @NotBlank
    @Size(max = 500)
    private String url;

    @Size(max = 200)
    private String branch;

    @Size(max = 200)
    private String name;
}