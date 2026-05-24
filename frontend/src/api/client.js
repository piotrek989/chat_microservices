const DEFAULT_HEADERS = {
  Accept: "application/json"
};

export function getEnvBase(varName, devFallback) {
  const value = import.meta.env[varName];
  if (value) {
    return value;
  }
  if (import.meta.env.DEV && devFallback) {
    return devFallback;
  }
  throw new Error(`${varName} is not configured`);
}

export function getStoredToken() {
  return localStorage.getItem("accessToken");
}

export function getStoredTokenType() {
  return localStorage.getItem("tokenType") || "Bearer";
}

export function setStoredToken(accessToken, tokenType) {
  if (accessToken) {
    localStorage.setItem("accessToken", accessToken);
  }
  if (tokenType) {
    localStorage.setItem("tokenType", tokenType);
  }
}

export function clearStoredToken() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("tokenType");
}

export function authHeader() {
  const token = getStoredToken();
  if (!token) {
    return {};
  }
  const tokenType = getStoredTokenType();
  return { Authorization: `${tokenType} ${token}` };
}

export async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      ...DEFAULT_HEADERS,
      ...(options.headers || {})
    }
  });

  const contentType = response.headers.get("content-type") || "";
  let data = null;
  if (contentType.includes("application/json")) {
    data = await response.json();
  } else if (response.status !== 204) {
    data = await response.text();
  }

  if (!response.ok) {
    const error = new Error("Request failed");
    error.status = response.status;
    error.payload = data;
    throw error;
  }

  return data;
}
