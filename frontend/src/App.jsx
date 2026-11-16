import React, { useEffect, useMemo, useState } from "react";
import {
  registerUser,
  loginUser,
  fetchCurrentUser,
  confirmEmail,
  isConfirmEnabled,
  resolveUserByUsername
} from "./api/auth.js";
import { clearStoredToken, getStoredToken } from "./api/client.js";
import { createChat, getChats, getMessages, sendMessage } from "./api/chat.js";
import { listFiles, uploadFile } from "./api/files.js";
import { getFileNotifications, getMessageNotifications } from "./api/notifications.js";

const emptyRegister = { username: "", email: "", password: "" };
const emptyLogin = { username: "", password: "" };
const emptyConfirm = { username: "", code: "" };

function formatDate(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function decodeJwtPayload(token) {
  if (!token) {
    return null;
  }
  const parts = token.split(".");
  if (parts.length < 2) {
    return null;
  }
  try {
    const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = payload.padEnd(Math.ceil(payload.length / 4) * 4, "=");
    const json = atob(padded);
    return JSON.parse(json);
  } catch (error) {
    return null;
  }
}

export default function App() {
  const [authMode, setAuthMode] = useState("login");
  const [registerForm, setRegisterForm] = useState(emptyRegister);
  const [loginForm, setLoginForm] = useState(emptyLogin);
  const [confirmForm, setConfirmForm] = useState(emptyConfirm);
  const [authError, setAuthError] = useState("");
  const [authNotice, setAuthNotice] = useState("");
  const [authBusy, setAuthBusy] = useState(false);

  const [user, setUser] = useState(null);
  const [chats, setChats] = useState([]);
  const [selectedChatId, setSelectedChatId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [messageDraft, setMessageDraft] = useState("");
  const [files, setFiles] = useState([]);
  const [fileDraft, setFileDraft] = useState({
    file: null,
    description: "",
    tags: ""
  });
  const [participantDraft, setParticipantDraft] = useState("");
  const [notifications, setNotifications] = useState({ messages: [], files: [] });
  const [dataError, setDataError] = useState("");
  const [dataNotice, setDataNotice] = useState("");
  const [tokenProfile, setTokenProfile] = useState(null);

  const hasToken = Boolean(getStoredToken());
  const confirmEnabled = isConfirmEnabled();

  useEffect(() => {
    if (!hasToken) {
      return;
    }
    setTokenProfile(decodeJwtPayload(getStoredToken()));
    const load = async () => {
      try {
        const currentUser = await fetchCurrentUser();
        setUser(currentUser);
        await loadChats();
        await loadNotifications();
      } catch (error) {
        clearStoredToken();
        setUser(null);
      }
    };
    load();
  }, []);

  useEffect(() => {
    if (!hasToken) {
      return undefined;
    }

    const interval = setInterval(() => {
      loadNotifications();
      if (selectedChatId) {
        loadMessages(selectedChatId);
        loadFiles(selectedChatId);
      }
    }, 10000);

    const refreshNotificationsWhenVisible = () => {
      if (document.visibilityState === "visible") {
        loadNotifications();
      }
    };
    document.addEventListener("visibilitychange", refreshNotificationsWhenVisible);

    return () => {
      clearInterval(interval);
      document.removeEventListener("visibilitychange", refreshNotificationsWhenVisible);
    };
  }, [hasToken, selectedChatId]);

  const selectedChat = useMemo(
    () => chats.find((chat) => chat.chatId === selectedChatId),
    [chats, selectedChatId]
  );

  const conversationItems = useMemo(() => {
    const messageItems = messages.map((message) => ({
      type: "text",
      key: `message-${message.messageId}`,
      createdAt: message.createdAt,
      payload: message
    }));
    const fileItems = files.map((file) => ({
      type: "file",
      key: `file-${file.fileId}`,
      createdAt: file.createdAt,
      payload: file
    }));

    return [...messageItems, ...fileItems].sort((left, right) => {
      const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0;
      const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0;
      return leftTime - rightTime;
    });
  }, [messages, files]);

  const resolveUsername = (senderId, senderUsername) => {
    if (senderUsername) {
      return senderUsername;
    }
    if (senderId && user?.id && senderId === user.id) {
      return user.username;
    }
    if (senderId && tokenProfile?.sub === senderId) {
      return tokenProfile["cognito:username"] || tokenProfile.username || user?.username;
    }
    if (!senderId) {
      return "Nieznany";
    }
    return `User ${senderId.slice(0, 8)}`;
  };

  const handleRegister = async (event) => {
    event.preventDefault();
    setAuthError("");
    setAuthNotice("");
    setAuthBusy(true);
    try {
      await registerUser(registerForm);
      setAuthNotice("Konto utworzone. Sprawdz poczte i wpisz kod weryfikacyjny.");
      if (confirmEnabled) {
        setConfirmForm((prev) => ({ ...prev, username: registerForm.username }));
        setAuthMode("confirm");
      }
    } catch (error) {
      setAuthError("Rejestracja nie powiodla sie.");
    } finally {
      setAuthBusy(false);
    }
  };

  const handleConfirm = async (event) => {
    event.preventDefault();
    setAuthError("");
    setAuthNotice("");
    setAuthBusy(true);
    try {
      await confirmEmail({
        username: confirmForm.username,
        code: confirmForm.code
      });
      setAuthNotice("Email potwierdzony. Mozesz sie zalogowac.");
      setAuthMode("login");
    } catch (error) {
      setAuthError("Nie udalo sie potwierdzic emaila.");
    } finally {
      setAuthBusy(false);
    }
  };

  const handleLogin = async (event) => {
    event.preventDefault();
    setAuthError("");
    setAuthNotice("");
    setAuthBusy(true);
    try {
      await loginUser(loginForm);
      const currentUser = await fetchCurrentUser();
      setUser(currentUser);
      setAuthNotice("");
      await loadChats();
      await loadNotifications();
    } catch (error) {
      setAuthError("Logowanie nie powiodlo sie.");
    } finally {
      setAuthBusy(false);
    }
  };

  const handleLogout = () => {
    clearStoredToken();
    setUser(null);
    setChats([]);
    setSelectedChatId(null);
    setMessages([]);
    setFiles([]);
    setNotifications({ messages: [], files: [] });
  };

  const loadChats = async () => {
    try {
      const data = await getChats();
      setChats(data || []);
    } catch (error) {
      setDataError("Nie udalo sie pobrac listy chatow.");
    }
  };

  const loadMessages = async (chatId) => {
    if (!chatId) {
      return;
    }
    try {
      const data = await getMessages(chatId);
      setMessages(data || []);
    } catch (error) {
      setDataError("Nie udalo sie pobrac wiadomosci.");
    }
  };

  const loadFiles = async (chatId) => {
    if (!chatId) {
      return;
    }
    try {
      const data = await listFiles(chatId);
      setFiles(data || []);
    } catch (error) {
      setDataError("Nie udalo sie pobrac plikow.");
    }
  };

  const loadNotifications = async () => {
    try {
      const [messageItems, fileItems] = await Promise.all([
        getMessageNotifications(),
        getFileNotifications()
      ]);
      setNotifications({
        messages: messageItems || [],
        files: fileItems || []
      });
    } catch (error) {
      setDataError("Nie udalo sie pobrac powiadomien.");
    }
  };

  const handleSelectChat = async (chatId) => {
    setSelectedChatId(chatId);
    setDataError("");
    await loadMessages(chatId);
    await loadFiles(chatId);
  };

  const handleCreateChat = async (event) => {
    event.preventDefault();
    setDataError("");
    const participantUsernames = participantDraft
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);
    if (!participantUsernames.length) {
      setDataNotice("Wpisz co najmniej jeden username uczestnika.");
      return;
    }
    try {
      const resolvedUsers = await Promise.all(
        participantUsernames.map((username) => resolveUserByUsername(username))
      );
      const participantIds = resolvedUsers.map((userItem) => userItem.id);
      const response = await createChat(participantIds);
      setParticipantDraft("");
      setDataNotice("Chat utworzony.");
      await loadChats();
      if (response?.chatId) {
        handleSelectChat(response.chatId);
      }
    } catch (error) {
      setDataError("Nie udalo sie utworzyc chatu. Sprawdz username.");
    }
  };

  const handleSendMessage = async (event) => {
    event.preventDefault();
    if (!selectedChatId || !messageDraft.trim()) {
      return;
    }
    try {
      await sendMessage(selectedChatId, messageDraft.trim());
      setMessageDraft("");
      await loadMessages(selectedChatId);
    } catch (error) {
      setDataError("Nie udalo sie wyslac wiadomosci.");
    }
  };

  const handleUpload = async (event) => {
    event.preventDefault();
    if (!selectedChatId || !fileDraft.file || !user?.id) {
      setDataError("Wybierz chat oraz plik do wyslania.");
      return;
    }
    try {
      const tags = fileDraft.tags
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean);
      await uploadFile({
        file: fileDraft.file,
        chatId: selectedChatId,
        uploaderId: user.id,
        description: fileDraft.description,
        tags
      });
      setFileDraft({ file: null, description: "", tags: "" });
      await loadFiles(selectedChatId);
    } catch (error) {
      setDataError("Nie udalo sie wyslac pliku.");
    }
  };

  if (!user) {
    return (
      <div className="app-shell">
        <div className="top-bar">
          <div className="brand">
            Chat<span>2</span>
          </div>
        </div>
        <div className="auth-card">
          <div className="auth-toggle">
            <button
              className={`button ${authMode === "login" ? "" : "secondary"}`}
              onClick={() => setAuthMode("login")}
              type="button"
            >
              Logowanie
            </button>
            <button
              className={`button ${authMode === "register" ? "" : "secondary"}`}
              onClick={() => setAuthMode("register")}
              type="button"
            >
              Rejestracja
            </button>
            <button
              className={`button ${authMode === "confirm" ? "" : "secondary"}`}
              onClick={() => setAuthMode("confirm")}
              type="button"
              disabled={!confirmEnabled}
            >
              Potwierdzenie
            </button>
          </div>

          {authError && <div className="notice">{authError}</div>}
          {authNotice && <div className="notice">{authNotice}</div>}
          {!confirmEnabled && (
            <div className="notice">
              Brak skonfigurowanego endpointu potwierdzania emaila. Ustaw
              VITE_AUTH_CONFIRM_ENDPOINT.
            </div>
          )}

          {authMode === "login" && (
            <form className="section" onSubmit={handleLogin}>
              <input
                className="input"
                placeholder="Username"
                value={loginForm.username}
                onChange={(event) =>
                  setLoginForm((prev) => ({ ...prev, username: event.target.value }))
                }
              />
              <input
                className="input"
                placeholder="Haslo"
                type="password"
                value={loginForm.password}
                onChange={(event) =>
                  setLoginForm((prev) => ({ ...prev, password: event.target.value }))
                }
              />
              <button className="button" type="submit" disabled={authBusy}>
                Zaloguj
              </button>
            </form>
          )}

          {authMode === "register" && (
            <form className="section" onSubmit={handleRegister}>
              <input
                className="input"
                placeholder="Username"
                value={registerForm.username}
                onChange={(event) =>
                  setRegisterForm((prev) => ({ ...prev, username: event.target.value }))
                }
              />
              <input
                className="input"
                placeholder="Email"
                value={registerForm.email}
                onChange={(event) =>
                  setRegisterForm((prev) => ({ ...prev, email: event.target.value }))
                }
              />
              <input
                className="input"
                placeholder="Haslo"
                type="password"
                value={registerForm.password}
                onChange={(event) =>
                  setRegisterForm((prev) => ({ ...prev, password: event.target.value }))
                }
              />
              <button className="button" type="submit" disabled={authBusy}>
                Zarejestruj
              </button>
            </form>
          )}

          {authMode === "confirm" && (
            <form className="section" onSubmit={handleConfirm}>
              <input
                className="input"
                placeholder="Username"
                value={confirmForm.username}
                onChange={(event) =>
                  setConfirmForm((prev) => ({ ...prev, username: event.target.value }))
                }
              />
              <input
                className="input"
                placeholder="Kod z emaila"
                value={confirmForm.code}
                onChange={(event) =>
                  setConfirmForm((prev) => ({ ...prev, code: event.target.value }))
                }
              />
              <button className="button" type="submit" disabled={authBusy || !confirmEnabled}>
                Potwierdz
              </button>
            </form>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <div className="top-bar">
        <div className="brand">
          Chat<span>2</span>
        </div>
        <div className="split" style={{ alignItems: "center" }}>
          <span className="badge">{user.username}</span>
          <button className="button secondary" type="button" onClick={handleLogout}>
            Wyloguj
          </button>
        </div>
      </div>

      <div className="container">
        <div className="panel">
          <h2>Twoje Chaty</h2>
          <div className="chats-list">
            {chats.map((chat) => (
              <div
                key={chat.chatId}
                className={`chat-item ${chat.chatId === selectedChatId ? "active" : ""}`}
                onClick={() => handleSelectChat(chat.chatId)}
              >
                <div>Chat {chat.chatId.slice(0, 6)}</div>
                {chat.lastMessage && (
                  <div className="notice">{chat.lastMessage.content}</div>
                )}
              </div>
            ))}
            {!chats.length && <div className="notice">Brak chatow.</div>}
          </div>

          <form className="section" onSubmit={handleCreateChat}>
            <input
              className="input"
              placeholder="Uczestnicy (ID oddzielone przecinkami)"
              value={participantDraft}
              onChange={(event) => setParticipantDraft(event.target.value)}
            />
            <button className="button" type="submit">
              Nowy chat
            </button>
          </form>
        </div>

        <div className="panel">
          <h2>Wiadomosci</h2>
          {dataError && <div className="notice">{dataError}</div>}
          {dataNotice && <div className="notice">{dataNotice}</div>}
          {!selectedChatId && <div className="notice">Wybierz chat po lewej.</div>}
          {selectedChatId && (
            <>
              <div className="messages">
                {conversationItems.map((item) => {
                  if (item.type === "text") {
                    const message = item.payload;
                    const isOwn = message.senderId === user.id;
                    return (
                      <div
                        key={item.key}
                        className={`message ${isOwn ? "own" : ""}`}
                      >
                        <div>{message.content}</div>
                        <div className="meta">
                          {resolveUsername(message.senderId, message.senderUsername)} • {formatDate(message.createdAt)}
                        </div>
                      </div>
                    );
                  }

                  const file = item.payload;
                  return (
                    <div key={item.key} className="message media">
                      {file.presignedUrl && file.contentType?.startsWith("image") && (
                        <img src={file.presignedUrl} alt={file.originalName} />
                      )}
                      <div>{file.originalName}</div>
                      {file.description && <div className="meta">{file.description}</div>}
                      <div className="meta">
                        {resolveUsername(file.uploaderId)} • {formatDate(file.createdAt)}
                      </div>
                    </div>
                  );
                })}
                {!conversationItems.length && <div className="notice">Brak wiadomosci.</div>}
              </div>

              <form className="section" onSubmit={handleSendMessage}>
                <textarea
                  rows="3"
                  placeholder="Napisz wiadomosc..."
                  value={messageDraft}
                  onChange={(event) => setMessageDraft(event.target.value)}
                />
                <button className="button" type="submit">
                  Wyslij
                </button>
              </form>
            </>
          )}
        </div>

        <div className="panel">
          <h2>Pliki i Powiadomienia</h2>
          {selectedChatId && (
            <div className="section">
              <form className="section" onSubmit={handleUpload}>
                <input
                  className="input"
                  type="file"
                  accept="image/*"
                  onChange={(event) =>
                    setFileDraft((prev) => ({ ...prev, file: event.target.files?.[0] || null }))
                  }
                />
                <input
                  className="input"
                  placeholder="Opis (opcjonalnie)"
                  value={fileDraft.description}
                  onChange={(event) =>
                    setFileDraft((prev) => ({ ...prev, description: event.target.value }))
                  }
                />
                <input
                  className="input"
                  placeholder="Tagi (np. dokument, prywatne)"
                  value={fileDraft.tags}
                  onChange={(event) =>
                    setFileDraft((prev) => ({ ...prev, tags: event.target.value }))
                  }
                />
                <button className="button" type="submit">
                  Wyslij plik
                </button>
              </form>
            </div>
          )}

          <div className="section">
            <div className="split">
              <button className="button secondary" type="button" onClick={loadNotifications}>
                Odswiez powiadomienia
              </button>
              <button
                className="button secondary"
                type="button"
                onClick={() => selectedChatId && loadMessages(selectedChatId)}
              >
                Odswiez chat
              </button>
            </div>

            <div>
              <div className="notice">Nowe wiadomosci</div>
              {notifications.messages.map((item) => (
                <div key={item.id} className="notification">
                  <div>{item.title}</div>
                  <div className="meta">{item.body}</div>
                  <div className="meta">{formatDate(item.createdAt)}</div>
                </div>
              ))}
              {!notifications.messages.length && <div className="notice">Brak powiadomien.</div>}
            </div>

            <div>
              <div className="notice">Nowe pliki</div>
              {notifications.files.map((item) => (
                <div key={item.id} className="notification">
                  <div>{item.title}</div>
                  <div className="meta">{item.body}</div>
                  <div className="meta">{formatDate(item.createdAt)}</div>
                </div>
              ))}
              {!notifications.files.length && <div className="notice">Brak powiadomien.</div>}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
