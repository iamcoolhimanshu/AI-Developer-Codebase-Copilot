package com.codecopilot.chat.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "conversation_messages", indexes = {
        @Index(name = "idx_msg_conv", columnList = "conversation_id")})
@Getter
@Setter
public class ConversationMessage extends BaseEntity {

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String role;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(columnDefinition = "LONGTEXT")
    private String sourcesJson;

    @Column(columnDefinition = "LONGTEXT")
    private String toolsJson;

    @Column(length = 16)
    private String model;
}