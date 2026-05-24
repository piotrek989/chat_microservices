import { requestJson, authHeader, getEnvBase } from "./client.js";

const FILE_BASE = getEnvBase("VITE_FILE_API", "http://localhost:8082");

export function listFiles(chatId) {
  const url = chatId ? `${FILE_BASE}/files?chatId=${encodeURIComponent(chatId)}` : `${FILE_BASE}/files`;
  return requestJson(url, {
    headers: {
      ...authHeader()
    }
  });
}

export async function uploadFile({ file, chatId, uploaderId, description, tags }) {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("chatId", chatId);
  formData.append("uploaderId", uploaderId);
  if (description) {
    formData.append("description", description);
  }
  if (tags?.length) {
    tags.forEach((tag) => formData.append("tags", tag));
  }

  const response = await fetch(`${FILE_BASE}/files`, {
    method: "POST",
    headers: {
      ...authHeader()
    },
    body: formData
  });

  if (!response.ok) {
    const payload = await response.text();
    const error = new Error("Upload failed");
    error.status = response.status;
    error.payload = payload;
    throw error;
  }

  return response.json();
}
