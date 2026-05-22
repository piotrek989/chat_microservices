package com.chatapp.chatservice.service;

import com.chatapp.chatservice.dto.ChatSummaryDto;
import com.chatapp.chatservice.dto.CreateChatRequest;
import com.chatapp.chatservice.dto.LastMessageDto;
import com.chatapp.chatservice.dto.MessageDto;
import com.chatapp.chatservice.dto.SendMessageRequest;
import com.chatapp.chatservice.entity.Chat;
import com.chatapp.chatservice.entity.Message;
import com.chatapp.chatservice.repository.ChatParticipantRepository;
import com.chatapp.chatservice.repository.ChatRepository;
import com.chatapp.chatservice.repository.MessageRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;

    public ChatService(ChatRepository chatRepository,
                       ChatParticipantRepository chatParticipantRepository,
                       MessageRepository messageRepository) {
        this.chatRepository = chatRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.messageRepository = messageRepository;
    }

    public UUID createChat(String userId, CreateChatRequest request) {
        Set<String> participantIds = new HashSet<>();
        if (request.getParticipantIds() != null) {
            participantIds.addAll(request.getParticipantIds());
        }
        participantIds.add(userId);

        Chat chat = new Chat(UUID.randomUUID());
        for (String participantId : participantIds) {
            chat.addParticipant(participantId);
        }

        Chat savedChat = chatRepository.save(chat);
        return savedChat.getId();
    }

    public List<ChatSummaryDto> getUserChats(String userId) {
        List<Chat> chats = chatRepository.findByParticipantUserId(userId);
        List<ChatSummaryDto> result = new ArrayList<>();

        for (Chat chat : chats) {
            Message lastMessage = messageRepository.findFirstByChatIdOrderByCreatedAtDesc(chat.getId());
            LastMessageDto lastMessageDto = null;
            if (lastMessage != null) {
                lastMessageDto = new LastMessageDto(
                    lastMessage.getId(),
                    lastMessage.getSenderId(),
                    lastMessage.getContent(),
                    lastMessage.getCreatedAt()
                );
            }
            result.add(new ChatSummaryDto(chat.getId(), chat.getCreatedAt(), lastMessageDto));
        }

        return result;
    }

    public MessageDto sendMessage(UUID chatId, String userId, SendMessageRequest request) {
        Chat chat = getChatOrThrow(chatId);
        ensureParticipant(chatId, userId);

        Message message = new Message(UUID.randomUUID(), chat, userId, request.getContent());
        Message savedMessage = messageRepository.save(message);

        return new MessageDto(
            savedMessage.getId(),
            chatId,
            savedMessage.getSenderId(),
            savedMessage.getContent(),
            savedMessage.getCreatedAt()
        );
    }

    public List<MessageDto> getMessages(UUID chatId, String userId) {
        getChatOrThrow(chatId);
        ensureParticipant(chatId, userId);

        List<Message> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        List<MessageDto> result = new ArrayList<>();
        for (Message message : messages) {
            result.add(new MessageDto(
                message.getId(),
                chatId,
                message.getSenderId(),
                message.getContent(),
                message.getCreatedAt()
            ));
        }

        return result;
    }

    private Chat getChatOrThrow(UUID chatId) {
        return chatRepository.findById(chatId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));
    }

    private void ensureParticipant(UUID chatId, String userId) {
        boolean isParticipant = chatParticipantRepository.existsByIdChatIdAndIdUserId(chatId, userId);
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a chat participant");
        }
    }
}
