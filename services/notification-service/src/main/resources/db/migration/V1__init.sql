CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    chat_id VARCHAR(128),
    message_id VARCHAR(128),
    file_id VARCHAR(128),
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    sns_message_id VARCHAR(128)
);
