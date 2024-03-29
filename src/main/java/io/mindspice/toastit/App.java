package io.mindspice.toastit;

import io.mindspice.toastit.entries.CalendarEvents;
import io.mindspice.toastit.entries.DatedEntry;
import io.mindspice.toastit.entries.TodoManager;
import io.mindspice.toastit.entries.project.ProjectManager;
import io.mindspice.toastit.entries.task.TaskManager;
import io.mindspice.toastit.entries.text.TextManager;
import io.mindspice.toastit.enums.EntryType;
import io.mindspice.toastit.sqlite.DBConnection;
import io.mindspice.toastit.entries.event.EventManager;
import io.mindspice.kawautils.wrappers.KawaInstance;
import io.mindspice.toastit.shell.ApplicationShell;
import io.mindspice.toastit.util.Settings;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;


public class App implements CalendarEvents {

    private static final App INSTANCE;
    private KawaInstance scheme;
    private ApplicationShell shell;
    private DBConnection dbConnection;
    private ScheduledExecutorService exec;

    //Managers
    private EventManager eventManager;
    private TaskManager taskManager;
    private ProjectManager projectManager;
    private TextManager noteManager;
    private TextManager journalManager;
    private TodoManager todoManager;

    public BiFunction<LocalDate, Function<DatedEntry, String>, List<String>> calendarEventProvider = (date, func) -> {
        var eventList = new ArrayList<String>();
        eventList.addAll(eventManager.getCalendarEvents(date, func));
        eventList.addAll(projectManager.getCalendarEvents(date, func));
        eventList.addAll(taskManager.getCalendarEvents(date, func));
        return eventList;
    };

    static {
        try {
            INSTANCE = new App();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static App instance() {
        return INSTANCE;
    }

    private App() throws IOException {
        scheme = new KawaInstance();
        scheme.defineObject("SchemeInstance", scheme);
        var loadResult = scheme.loadSchemeFile(new File("scheme_files/init.scm"));
        if (!loadResult.valid()) {
            System.err.println("Error loading init.scm: " + loadResult.exception().orElseThrow());
            System.out.println(Arrays.toString(loadResult.exception().get().getStackTrace()));
        }

        dbConnection = new DBConnection();


    }

    public App init() throws IOException {

        scheme.defineObject("AppInstance", this);

        exec = Executors.newScheduledThreadPool(Settings.EXEC_THREADS);
        eventManager = new EventManager();
        taskManager = new TaskManager();
        projectManager = new ProjectManager();
        noteManager = new TextManager();
        journalManager = new TextManager();
        todoManager = new TodoManager();
        shell = new ApplicationShell(scheme);

        eventManager.init();
        taskManager.init();
        projectManager.init();
        noteManager.init(EntryType.NOTE);
        journalManager.init(EntryType.JOURNAL);

        var loadResult = scheme.loadSchemeFile(new File("scheme_files/post-init.scm"));
        if (!loadResult.valid()) {
            System.err.println("Error loading post-init.scm: " + loadResult.exception().orElseThrow());
            System.out.println(Arrays.toString(loadResult.exception().get().getStackTrace()));
        }
        return INSTANCE;
    }

    public DBConnection getDatabase() {
        return dbConnection;
    }

    public ScheduledExecutorService getExec() {
        return exec;
    }

    public ApplicationShell getShell() {
        return shell;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public ProjectManager getProjectManager() {
        return projectManager;
    }

    public TextManager getNoteManager() {
        return noteManager;
    }

    public TextManager getJournalManager() {
        return journalManager;
    }

    public TodoManager getTodoManager() {
        return todoManager;
    }


    @Override
    public List<String> getCalendarEvents(LocalDate date, Function<DatedEntry, String> dataMapper) {
        return calendarEventProvider.apply(date, dataMapper);
    }
}
