import { requestJson, authHeader, getEnvBase } from "./client.js";

const CHAT_BASE = getEnvBase("VITE_CHAT_API", "http://localhost:8081");

export function getChats() {
  return requestJson(`${CHAT_BASE}/chats`, {
    headers: {
      ...authHeader()
    }
  });
}

export function createChat(participantIds) {
  return requestJson(`${CHAT_BASE}/chats`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeader()
    },
    body: JSON.stringify({ participantIds })
  });
}

export function getMessages(chatId) {
  return requestJson(`${CHAT_BASE}/chats/${chatId}/messages`, {
    headers: {
      ...authHeader()
    }
  });
}

export function sendMessage(chatId, content) {
  return requestJson(`${CHAT_BASE}/chats/${chatId}/messages`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeader()
    },
    body: JSON.stringify({ content })
  });
}
