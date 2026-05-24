package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.ChatParticipant;
import com.chatapp.chatservice.entity.ChatParticipantId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {
    boolean existsByIdChatIdAndIdUserId(UUID chatId, String userId);

    List<ChatParticipant> findByIdChatId(UUID chatId);
}
