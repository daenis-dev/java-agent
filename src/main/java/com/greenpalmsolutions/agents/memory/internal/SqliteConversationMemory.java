package com.greenpalmsolutions.agents.memory.internal;

import com.greenpalmsolutions.agents.memory.api.ConversationMemory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class SqliteConversationMemory implements ConversationMemory {

    private final String jdbc;

    SqliteConversationMemory(@Value("${agent.database}") String file) throws Exception {
        Path path = Path
                .of(file)
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(path.getParent());
        jdbc = "jdbc:sqlite:" + path;

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("CREATE TABLE IF NOT EXISTS messages ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, conversation_id TEXT NOT NULL, "
                    + "role TEXT NOT NULL, content TEXT NOT NULL, "
                    + "created_at TEXT NOT NULL DEFAULT (datetime('now')))");
            statement.execute("CREATE INDEX IF NOT EXISTS messages_conversation ON messages(conversation_id, id)");
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbc);
    }

    @Override
    public synchronized void append(String conversationId, String role, String content) {
        String sql = "INSERT INTO messages(conversation_id, role, content) VALUES (?, ?, ?)";

        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, conversationId);
            statement.setString(2, role);
            statement.setString(3, content);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot persist conversation", exception);
        }
    }

    @Override
    public List<Message> recent(String id, int limit) {
        List<Message> result = new ArrayList<>();
        String sql = "SELECT id, conversation_id, role, content, created_at FROM messages "
                + "WHERE conversation_id = ? ORDER BY id DESC LIMIT ?";

        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setInt(2, Math.max(1, Math.min(limit, 200)));

            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(
                            new Message(
                                    rows.getLong(1),
                                    rows.getString(2),
                                    rows.getString(3),
                                    rows.getString(4),
                                    rows.getString(5)));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot read conversation", exception);
        }

        Collections.reverse(result);
        return result;
    }

    @Override
    public List<String> conversations() {
        List<String> result = new ArrayList<>();
        String sql = "SELECT conversation_id FROM messages GROUP BY conversation_id ORDER BY MAX(id) DESC LIMIT 100";

        try (Connection connection = connect(); Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                result.add(rows.getString(1));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot list conversations", exception);
        }

        return result;
    }
}
