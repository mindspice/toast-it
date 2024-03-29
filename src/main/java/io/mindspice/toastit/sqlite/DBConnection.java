package io.mindspice.toastit.sqlite;

import io.mindspice.toastit.entries.event.EventEntry;
import io.mindspice.toastit.entries.project.ProjectEntry;
import io.mindspice.toastit.entries.task.TaskEntry;
import io.mindspice.toastit.entries.text.TextEntry;
import io.mindspice.toastit.util.DateTimeUtil;
import io.mindspice.toastit.util.JSON;
import io.mindspice.toastit.util.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class DBConnection {
    private final String url;
    private final Connection connection;

    public DBConnection() throws IOException {
        boolean exists = Files.exists(Path.of(Settings.DATABASE_PATH));
        if (!exists) {
            Files.createDirectories(Path.of(Settings.DATABASE_PATH).getParent());
        };
        url = "jdbc:sqlite:" + Settings.DATABASE_PATH;
        try {
            Connection conn = DriverManager.getConnection(url);
            if (conn == null) {
                throw new IllegalStateException("Failed to connect/create database at: " + url);
            }
            connection = conn;
        } catch (SQLException e) {
            throw new IllegalStateException("Exception encountered connecting to database. Error: " + e);
        }
        if (!exists) {
            initDBTables();
        }
    }

    public void initDBTables() {
        for (String table : TableInit.INIT_STATEMENTS) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(table);
            } catch (SQLException e) {
                System.err.println(Arrays.toString(e.getStackTrace()));
                throw new IllegalStateException("Exception encountered creating database tables: " + e);
            }
        }
        System.out.println("Initialized and created tables");
    }

    public Connection getConnection() {
        return connection;
    }

    ////////////
    // SELECT //
    ////////////

    // Result -> Objects

    private List<TaskEntry.Stub> execMapTaskStubs(PreparedStatement ps) throws IOException {
        List<TaskEntry.Stub> tasks = new ArrayList<>();
        try (ResultSet result = ps.executeQuery()) {
            while (result.next()) {
                var task = new TaskEntry.Stub(
                        result.getString("uuid"),
                        result.getString("name"),
                        result.getBoolean("started"),
                        result.getBoolean("completed"),
                        result.getString("tags"),
                        result.getLong("due_by"),
                        result.getLong("started_at"),
                        result.getLong("completed_at"),
                        result.getString("reminders"),
                        result.getString("meta_path")
                );
                tasks.add(task);
            }
            return tasks;
        } catch (SQLException e) {
            throw new IOException("SQL error returned Error: " + e.getMessage());
        }
    }

    private List<EventEntry.Stub> execMapEventStubs(PreparedStatement ps) throws IOException {
        List<EventEntry.Stub> events = new ArrayList<>();
        try (ResultSet result = ps.executeQuery()) {
            while (result.next()) {
                var event = new EventEntry.Stub(
                        result.getString("uuid"),
                        result.getString("name"),
                        result.getString("tags"),
                        result.getLong("start_time"),
                        result.getLong("end_time"),
                        result.getString("reminders"),
                        result.getString("linked_uuid"),
                        result.getBoolean("completed")
                );
                events.add(event);
            }
            return events;
        } catch (SQLException e) {
            throw new IOException("SQL error returned Error: " + e.getMessage());
        }
    }

    private List<EventEntry> execMapEvents(PreparedStatement ps) throws IOException {
        List<EventEntry> events = new ArrayList<>();
        try (ResultSet result = ps.executeQuery()) {
            while (result.next()) {
                var event = new EventEntry(
                        UUID.fromString(result.getString("uuid")),
                        result.getString("name"),
                        JSON.jsonArrayToStringList(result.getString("tags")),
                        DateTimeUtil.unixToLocal(result.getLong("start_time")),
                        DateTimeUtil.unixToLocal(result.getLong("end_time")),
                        JSON.jsonArrayToReminderList(result.getString("reminders")),
                        UUID.fromString(result.getString("linked_uuid")),
                        result.getBoolean("completed")
                );
                events.add(event);
            }
            return events;
        } catch (SQLException e) {
            throw new IOException("SQL error returned Error: " + e.getMessage());
        }
    }

    private List<ProjectEntry.Stub> execMapProjectStubs(PreparedStatement ps) throws IOException {
        List<ProjectEntry.Stub> projects = new ArrayList<>();
        try (ResultSet result = ps.executeQuery()) {
            while (result.next()) {
                var project = new ProjectEntry.Stub(
                        result.getString("uuid"),
                        result.getString("name"),
                        result.getBoolean("started"),
                        result.getBoolean("completed"),
                        result.getString("tags"),
                        result.getLong("due_by"),
                        result.getLong("started_at"),
                        result.getLong("completed_at"),
                        result.getString("reminders"),
                        result.getString("meta_path"),
                        result.getString("project_path"),
                        result.getString("open_with")
                );
                projects.add(project);
            }
            return projects;
        } catch (SQLException e) {
            throw new IOException("SQL error returned Error: " + e.getMessage());
        }
    }

    private List<TextEntry.Stub> execMapTextStubs(PreparedStatement ps) throws IOException {
        List<TextEntry.Stub> entries = new ArrayList<>();
        try (ResultSet result = ps.executeQuery()) {
            while (result.next()) {
                var text = new TextEntry.Stub(
                        result.getString("uuid"),
                        result.getString("name"),
                        result.getLong("created_at"),
                        result.getString("tags"),
                        result.getString("meta_path")
                );
                entries.add(text);
            }
            return entries;
        } catch (SQLException e) {
            throw new IOException("SQL error returned Error: " + e.getMessage());
        }
    }

    // Select Statements

    private <T> T genericMetaSelect(UUID uuid, String tableName, Class<T> clazz) throws IOException {
        String query = String.format("SELECT meta_path FROM %s WHERE uuid = ?", tableName);

        String metaPath = null;
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());

            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    metaPath = result.getString("meta_path");
                } else {
                    throw new IOException("No meta_path found for UUID: " + uuid);
                }
            }
        } catch (SQLException e) {
            throw new IOException(String.format("SQL error returned for: %s Error: %s", uuid, e.getMessage()));
        }
        return JSON.loadObjectFromFile(metaPath, clazz);
    }

    public TaskEntry getTaskByUUID(UUID uuid) throws IOException {
        return genericMetaSelect(uuid, "tasks", TaskEntry.class);
    }

    public ProjectEntry getProjectByUUID(UUID uuid) throws IOException {
        return genericMetaSelect(uuid, "projects", ProjectEntry.class);
    }

    public TextEntry getNoteEntryByUUID(UUID uuid) throws IOException {
        return genericMetaSelect(uuid, "notes", TextEntry.class);
    }

    public TextEntry getJournalEntryByUUID(UUID uuid) throws IOException {
        return genericMetaSelect(uuid, "journals", TextEntry.class);
    }

    public EventEntry getEventByUUID(UUID uuid) throws IOException {
        String query = "SELECT * FROM events WHERE uuid = ?";

        EventEntry event = null;
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());

            List<EventEntry> events = execMapEvents(ps);
            return events.isEmpty() ? null : events.getFirst();
        } catch (
                SQLException e) {
            throw new IOException(String.format("SQL error returned for: %s Error: %s", uuid, e.getMessage()));
        }
    }

    public EventEntry.Stub getEventStubByUUID(UUID uuid) throws IOException {
        String query = "SELECT * FROM events WHERE uuid = ?";

        EventEntry.Stub event = null;
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());

            List<EventEntry.Stub> events = execMapEventStubs(ps);
            return events.isEmpty() ? null : events.getFirst();
        } catch (SQLException e) {
            throw new IOException(String.format("SQL error returned for: %s Error: %s", uuid, e.getMessage()));
        }
    }

    public TaskEntry.Stub getTaskStubByUUID(UUID uuid) throws IOException {
        String query = "SELECT * FROM tasks WHERE uuid = ?";

        TaskEntry.Stub stub = null;
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());

            List<TaskEntry.Stub> tasks = execMapTaskStubs(ps);
            return tasks.isEmpty() ? null : tasks.getFirst();
        } catch (SQLException e) {
            throw new IOException(String.format("SQL error returned for: %s Error: %s", uuid, e.getMessage()));
        }
    }

    public ProjectEntry.Stub getProjectStubByUUID(UUID uuid) throws IOException {
        String query = "SELECT * FROM projects WHERE uuid = ?";

        ProjectEntry.Stub stub = null;
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());

            List<ProjectEntry.Stub> projects = execMapProjectStubs(ps);
            return projects.isEmpty() ? null : projects.getFirst();

        } catch (SQLException e) {
            throw new IOException(String.format("SQL error returned for: %s Error: %s", uuid, e.getMessage()));
        }
    }

    private TextEntry.Stub textEntrySelect(UUID uuid, String table) throws IOException {
        String query = String.format("SELECT * FROM %s WHERE uuid = ?", table);

        TextEntry.Stub stub = null;
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());

            List<TextEntry.Stub> entries = execMapTextStubs(ps);
            return entries.isEmpty() ? null : entries.getFirst();
        } catch (SQLException e) {
            throw new IOException(String.format("SQL error returned for: %s Error: %s", uuid, e.getMessage()));
        }
    }

    public TextEntry.Stub getNoteStubByUUID(UUID uuid) throws IOException {
        return textEntrySelect(uuid, "notes");
    }

    public TextEntry.Stub getJournalStubByUUID(UUID uuid) throws IOException {
        return textEntrySelect(uuid, "journals");
    }

    public List<EventEntry> getEvents(long threshold) throws IOException {
        String query = threshold < 0
                ? "SELECT * FROM events"
                : String.format("SELECT * FROM events WHERE start_time < %d", threshold);

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            return execMapEvents(ps);
        } catch (SQLException e) {
            throw new IOException("Error querying events:" + e.getMessage());
        }
    }

    public List<TaskEntry.Stub> getActiveTasks() throws IOException {
        String query = "SELECT * FROM tasks WHERE started = true and archived = false";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            return execMapTaskStubs(ps);
        } catch (SQLException e) {
            throw new IOException("Error querying tasks:" + e.getMessage());
        }
    }

    public List<TaskEntry.Stub> getAllTasks() throws IOException {
        String query = "SELECT * FROM tasks where archived = false";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            return execMapTaskStubs(ps);
        } catch (SQLException e) {
            throw new IOException("Error querying tasks:" + e.getMessage());
        }
    }

    public List<ProjectEntry.Stub> getActiveProjects() throws IOException {
        String query = "SELECT * FROM projects WHERE started = true and archived = false";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            return execMapProjectStubs(ps);
        } catch (SQLException e) {
            throw new IOException("Error querying projects:" + e.getMessage());
        }
    }

    public List<ProjectEntry.Stub> getAllProjects() throws IOException {
        String query = "SELECT * FROM projects where archived = false";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            return execMapProjectStubs(ps);
        } catch (SQLException e) {
            throw new IOException("Error querying projects:" + e.getMessage());
        }
    }

    public List<TextEntry.Stub> getAllNotes() throws IOException {
        String query = "SELECT * FROM notes where archived = false";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            return execMapTextStubs(ps);
        } catch (SQLException e) {
            throw new IOException("Error querying notes:" + e.getMessage());
        }
    }

    public List<TextEntry.Stub> getAllJournals() throws IOException {
        String query = "SELECT * FROM journals where archived = false";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            return execMapTextStubs(ps);
        } catch (SQLException e) {
            throw new IOException("Error querying journals:" + e.getMessage());
        }
    }

    ////////////
    // INSERT //
    ///////////

    public void upsertEvent(EventEntry eventEntry) throws IOException {
        EventEntry.Stub entry = eventEntry.getStub();
        String query = """
                INSERT INTO events (uuid, name, tags, start_time, end_time, reminders, linked_uuid, completed)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                   name = excluded.name,
                   tags = excluded.tags,
                   start_time = excluded.start_time,
                   end_time = excluded.end_time,
                   reminders = excluded.reminders,
                   linked_uuid = excluded.linked_uuid,
                   completed = excluded.completed;
                """;

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, entry.uuid());
            ps.setString(2, entry.name());
            ps.setString(3, entry.tags());
            ps.setLong(4, entry.startTime());
            ps.setLong(5, entry.endTime());
            ps.setString(6, entry.reminders());
            ps.setString(7, entry.linkedUUID());
            ps.setBoolean(8, entry.completed());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(String.format("SQL error returned for: %s Error: %s", entry.uuid(), e.getMessage()));
        }
    }

    public void upsertTask(TaskEntry taskEntry) throws IOException {
        TaskEntry.Stub entry = taskEntry.getStub();
        String query = """
                INSERT INTO tasks(uuid, name, started, completed, tags, due_by,
                    started_at, completed_at, reminders, meta_path)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                   name = excluded.name,
                   tags = excluded.tags,
                   started = excluded.started,
                   completed = excluded.completed,
                   tags = excluded.tags,
                   due_by = excluded.due_by,
                   started_at = excluded.started_at,
                   completed_at = excluded.completed_at,
                   reminders = excluded.reminders,
                   meta_path = excluded.meta_path;
                """;

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, entry.uuid());
            ps.setString(2, entry.name());
            ps.setBoolean(3, entry.started());
            ps.setBoolean(4, entry.completed());
            ps.setString(5, entry.tags());
            ps.setLong(6, entry.dueBy());
            ps.setLong(7, entry.startedAt());
            ps.setLong(8, entry.completedAt());
            ps.setString(9, entry.reminders());
            ps.setString(10, entry.metaPath());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e);
            throw new IOException(String.format("SQL error returned for: %s Error: %s", entry.uuid(), e.getMessage()));
        }
    }

    public void upsertProject(ProjectEntry projectEntry) throws IOException {
        ProjectEntry.Stub entry = projectEntry.getStub();
        String query = """
                INSERT INTO projects (uuid, name, started, completed, tags, due_by, started_at, 
                    completed_at, reminders, meta_path, project_path, open_with)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                   name = excluded.name,
                   tags = excluded.tags,
                   started = excluded.started,
                   completed = excluded.completed,
                   tags = excluded.tags,
                   due_by = excluded.due_by,
                   started_at = excluded.started_at,
                   completed_at = excluded.completed_at,
                   reminders = excluded.reminders,
                   meta_path = excluded.meta_path,
                   project_path = excluded.project_path,
                   open_with = excluded.open_with
                """;

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, entry.uuid());
            ps.setString(2, entry.name());
            ps.setBoolean(3, entry.started());
            ps.setBoolean(4, entry.completed());
            ps.setString(5, entry.tags());
            ps.setLong(6, entry.dueBy());
            ps.setLong(7, entry.startedAt());
            ps.setLong(8, entry.completedAt());
            ps.setString(9, entry.reminders());
            ps.setString(10, entry.metaPath());
            ps.setString(11, entry.projectPath());
            ps.setString(12, entry.openWith());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IOException(String.format("SQL error returned for: %s Error: %s, Trace:",
                    entry.uuid(), e.getMessage(), Arrays.toString(e.getStackTrace())));
        }
    }

    private void upsertTextEntry(TextEntry textEntry, String table) throws IOException {
        TextEntry.Stub entry = textEntry.getStub();
        String query = String.format("""
                INSERT INTO %s (uuid, name, created_at, tags, meta_path)
                   VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                   name = excluded.name,
                   created_at = excluded.created_at,
                   tags = excluded.tags,
                   meta_path = excluded.meta_path;
                """, table);

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, entry.uuid());
            ps.setString(2, entry.name());
            ps.setLong(3, entry.createdAt());
            ps.setString(4, entry.tags());
            ps.setString(5, entry.metaPath());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(String.format("SQL error returned for: %s Error: %s", entry.uuid(), e.getMessage()));
        }
    }

    public void upsertNote(TextEntry entry) throws IOException {
        upsertTextEntry(entry, "notes");
    }

    public void upsertJournal(TextEntry entry) throws IOException {
        upsertTextEntry(entry, "journals");
    }

    private void setArchived(String uuid, String table, boolean isArchived) throws IOException {
        String query = String.format("UPDATE %s SET archived = ? WHERE uuid = ?", table);

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setBoolean(1, isArchived);
            ps.setString(2, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(String.format("SQL error returned for: %s Error: %s", uuid, e.getMessage()));
        }
    }

    public void archiveEvent(UUID uuid, boolean isArchived) throws IOException {
        setArchived(uuid.toString(), "events", isArchived);
    }

    public void archiveTask(UUID uuid, boolean isArchived) throws IOException {
        setArchived(uuid.toString(), "tasks", isArchived);
    }

    public void archiveProject(UUID uuid, boolean isArchived) throws IOException {
        setArchived(uuid.toString(), "project", isArchived);
    }

    public void archiveNote(UUID uuid, boolean isArchived) throws IOException {
        setArchived(uuid.toString(), "notes", isArchived);
    }

    public void archiveJournal(UUID uuid, boolean isArchived) throws IOException {
        setArchived(uuid.toString(), "journals", isArchived);
    }

    ////////////
    // DELETE //
    ////////////

    public void deletePastEventEntries(long threshold) throws IOException {
        String query = "DELETE FROM events WHERE end_time < ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, threshold);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(
                    String.format("SQL error returned for deleting events of: %d Error: %s", threshold, e.getMessage())
            );
        }
    }

    public void deleteEventByUUID(UUID uuid) throws IOException {
        deleteByUUID("events", uuid);
    }

    public void deleteTaskByUUID(UUID uuid) throws IOException {
        deleteByUUID("tasks", uuid);
    }

    public void deleteProjectByUUID(UUID uuid) throws IOException {
        deleteByUUID("projects", uuid);
    }

    public void deleteNoteByUUID(UUID uuid) throws IOException {
        deleteByUUID("notes", uuid);
    }

    public void deleteJournalByUUID(UUID uuid) throws IOException {
        deleteByUUID("journals", uuid);
    }

    private void deleteByUUID(String table, UUID uuid) throws IOException {
        String query = String.format("DELETE FROM %s WHERE uuid = ?", table);

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e);
            throw new IOException(
                    String.format("SQL error returned for deleting entry of: %s Error: %s", uuid, e.getMessage())
            );
        }
    }

}
