ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS sender_username VARCHAR(255);
