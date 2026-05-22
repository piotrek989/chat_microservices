package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByChatIdOrderByCreatedAtAsc(UUID chatId);

    Message findFirstByChatIdOrderByCreatedAtDesc(UUID chatId);
}
