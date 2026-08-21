package com.codecopilot.chat.repo;

import com.codecopilot.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByProjectIdAndUserIdOrderByLastMessageAtDesc(Long projectId, Long userId);

    Optional<Conversation> findByIdAndProjectId(Long id, Long projectId);
}