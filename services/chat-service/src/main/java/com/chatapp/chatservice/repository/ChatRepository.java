package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.Chat;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
    @Query("select distinct c from Chat c join c.participants p where p.id.userId = :userId")
    List<Chat> findByParticipantUserId(@Param("userId") String userId);
}
