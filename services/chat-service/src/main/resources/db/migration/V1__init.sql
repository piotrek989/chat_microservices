CREATE TABLE IF NOT EXISTS chats (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_participants (
    chat_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (chat_id, user_id),
    CONSTRAINT fk_chat_participants_chat
        FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY,
    chat_id UUID NOT NULL,
    sender_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_messages_chat
        FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_participants_user_id ON chat_participants (user_id);
CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON messages (chat_id);