package io.mindspice.toastit.sqlite;

import java.util.List;


public class TableInit {

    public static String EVENT_TABLE = """
            CREATE TABLE IF NOT EXISTS events (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                tags TEXT,  -- JSON array
                start_time INTEGER,  -- Unix time
                end_time INTEGER,    -- Unix time
                reminders TEXT, -- JSON array
                linked_uuid TEXT,
                completed BOOLEAN
            );
            """;

    public static String EVENT_ARCHIVE = """
            CREATE TABLE IF NOT EXISTS event_archive (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                tags TEXT,  -- JSON array
                start_time INTEGER,  -- Unix time
                end_time INTEGER,    -- Unix time
                reminders TEXT, -- JSON array
                linked_uuid TEXT,
                completed BOOLEAN
            );
            """;

    public static String TASK_TABLE = """
            CREATE TABLE IF NOT EXISTS tasks (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                started BOOLEAN,
                completed BOOLEAN,
                tags TEXT,   -- JSON array
                due_by INTEGER, -- Unix time
                started_at INTEGER,-- Unix time
                completed_at INTEGER,-- Unix time
                reminders TEXT, -- JSON array
                meta_path TEXT
            );
            """;

    public static String TASK_ARCHIVE = """
            CREATE TABLE IF NOT EXISTS task_archive (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                started BOOLEAN,
                completed BOOLEAN,
                tags TEXT,   -- JSON array
                due_by INTEGER, -- Unix time
                started_at INTEGER,-- Unix time
                completed_at INTEGER,-- Unix time
                reminders TEXT, -- JSON array
                meta_path TEXT
            );
            """;

    public static String PROJECT_TABLE = """
            CREATE TABLE IF NOT EXISTS projects (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                started BOOLEAN,
                completed BOOLEAN,
                tags TEXT,   -- JSON array
                due_by INTEGER, -- Unix time
                started_at INTEGER,-- Unix time
                completed_at INTEGER,-- Unix time
                reminders TEXT, --JSON array
                meta_path TEXT,
                project_path TEXT,
                open_with TEST,
                has_note BOOLEAN
            );
            """;

    public static String PROJECT_ARCHIVE = """
            CREATE TABLE IF NOT EXISTS projects (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                started BOOLEAN,
                completed BOOLEAN,
                tags TEXT,   -- JSON array
                due_by INTEGER, -- Unix time
                started_at INTEGER,-- Unix time
                completed_at INTEGER,-- Unix time
                reminders TEXT, --JSON array
                meta_path TEXT,
                project_path TEXT,
                open_with TEST,
                has_note BOOLEAN
            );
            """;

    public static String NOTE_TABLE = """
            CREATE TABLE IF NOT EXISTS notes (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                created_at INTEGER, -- Unix time
                tags TEXT,   -- JSON array
                meta_path TEXT
            );
            """;

    public static String JOURNAL_TABLE = """
            CREATE TABLE IF NOT EXISTS journals (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                created_at INTEGER, -- Unix time
                tags TEXT,   -- JSON array
                meta_path TEXT
            );
            """;

    public static List<String> INIT_STATEMENTS = List.of(
            EVENT_TABLE,
            TASK_TABLE,
            PROJECT_TABLE,
            NOTE_TABLE,
            JOURNAL_TABLE,
            EVENT_ARCHIVE,
            TASK_ARCHIVE,
            PROJECT_ARCHIVE
    );
}
