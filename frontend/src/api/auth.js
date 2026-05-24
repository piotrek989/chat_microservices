import { requestJson, setStoredToken, authHeader, getEnvBase } from "./client.js";

const AUTH_BASE = getEnvBase("VITE_AUTH_API", "http://localhost:8080");
const CONFIRM_ENDPOINT = import.meta.env.VITE_AUTH_CONFIRM_ENDPOINT || "";

export async function registerUser(payload) {
  return requestJson(`${AUTH_BASE}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
}

export async function loginUser(payload) {
  const result = await requestJson(`${AUTH_BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  if (result?.accessToken) {
    setStoredToken(result.accessToken, result.tokenType || "Bearer");
  }
  return result;
}

export async function fetchCurrentUser() {
  return requestJson(`${AUTH_BASE}/auth/me`, {
    headers: {
      ...authHeader()
    }
  });
}

export async function resolveUserByUsername(username) {
  const query = encodeURIComponent(username);
  return requestJson(`${AUTH_BASE}/auth/resolve?username=${query}`, {
    headers: {
      ...authHeader()
    }
  });
}

export function isConfirmEnabled() {
  return Boolean(CONFIRM_ENDPOINT);
}

export async function confirmEmail(payload) {
  if (!CONFIRM_ENDPOINT) {
    throw new Error("Confirmation endpoint not configured");
  }
  return requestJson(`${AUTH_BASE}${CONFIRM_ENDPOINT}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
}
