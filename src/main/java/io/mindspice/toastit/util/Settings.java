package io.mindspice.toastit.util;

import com.github.freva.asciitable.HorizontalAlign;
import com.github.freva.asciitable.OverflowBehaviour;
import io.mindspice.toastit.calendar.Calendar;
import io.mindspice.toastit.calendar.CalendarCell;
import io.mindspice.toastit.entries.DatedEntry;
import io.mindspice.toastit.shell.ShellMode;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;


public class Settings {

    // Paths
    public static String ROOT_PATH;
    public static String DATABASE_PATH;
    public static String TASK_PATH;
    public static String NOTE_PATH;
    public static String JOURNAL_PATH;
    public static String PROJECT_PATH;
    public static String TEMP_PATH;

    // Edit Settings

    private static final Map<String, Consumer<Path>> EDITOR_MAP = new HashMap<>();

    public static void addEditor(String editorName, Consumer<Path> editorConsumer) {
        EDITOR_MAP.put(editorName, editorConsumer);
    }

    public static Consumer<Path> getEditor(String key) {
        var editor = EDITOR_MAP.get(key);
        return editor == null ? EDITOR_MAP.get("nano") : editor;
    }

    public static Consumer<Path> getEditorOr(String key) {
        var editor = EDITOR_MAP.get(key);
        return editor == null ? Editor.of(key) : editor;
    }

    public static String FULL_TEXT_EDITOR;
    public static String SIMPLE_TEXT_EDITOR;

    // TAGS
    public static final Map<String, Tag> TAG_MAP = new HashMap<>();
    public static Tag DEFAULT_TAG = Tag.Default();

    public static Tag getTag(String tag) {
        return TAG_MAP.getOrDefault(tag, DEFAULT_TAG);
    }

    // Global
    public static int MAX_PREVIEW_LENGTH;

    // Application
    public static int EXEC_THREADS;
    public static List<String> DATE_INPUT_PATTERNS;
    public static List<String> TIME_INPUT_PATTERNS;
    public static String DATE_TIME_FULL_PATTERN;
    public static String DATE_TIME_SHORT_PATTERN;
    public static boolean THREADED_SEARCH;
    public static int SEARCH_TIMEOUT_SEC;

    // Events
    public static int EVENT_LOOK_FORWARD_DAYS;
    public static int EVENT_REFRESH_INV_MIN;
    public static int EVENT_NOTIFY_FADE_TIME_SEC;

    // TASKS
    public static int TASK_REFRESH_INV_MIN;
    public static int TASK_NOTIFY_FADE_TIME_SEC;

    // Shell Config
    public static String SHELL_BIND_ADDRESS;
    public static int SHELL_BIND_PORT;
    public static String SHELL_USER;
    public static String SHELL_PASSWORD;
    public static String SHELL_KEY_PAIR;

    public static List<ShellMode<?>> SHELL_MODES;

    // Calendar
    public static int CALENDAR_CELL_HEIGHT;
    public static int CALENDAR_CELL_WIDTH;
    public static int CALENDAR_REFRESH_SEC;
    public static UnaryOperator<CalendarCell> CALENDAR_CELL_MAPPER;
    public static Function<DatedEntry, String> CALENDAR_DATA_MAPPER;

    // Global Table Settings
    public static int TABLE_MAX_COLUMN_WIDTH;
    public static HorizontalAlign TABLE_DEFAULT_ALIGNMENT;
    public static OverflowBehaviour TABLE_OVERFLOW_BEHAVIOR;




}
