import { requestJson, authHeader, getEnvBase } from "./client.js";

const NOTIFICATION_BASE = getEnvBase("VITE_NOTIFICATION_API", "http://localhost:8083");

export function getMessageNotifications() {
  return requestJson(`${NOTIFICATION_BASE}/notifications/messages`, {
    headers: {
      ...authHeader()
    }
  });
}

export function getFileNotifications() {
  return requestJson(`${NOTIFICATION_BASE}/notifications/files`, {
    headers: {
      ...authHeader()
    }
  });
}
