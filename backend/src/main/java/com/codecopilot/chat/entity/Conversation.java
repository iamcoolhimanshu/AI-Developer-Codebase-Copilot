package com.codecopilot.chat.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conv_project", columnList = "project_id")})
@Getter
@Setter
public class Conversation extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title = "New conversation";

    @Column(columnDefinition = "TEXT")
    private String metadata;

    private Instant lastMessageAt;
}