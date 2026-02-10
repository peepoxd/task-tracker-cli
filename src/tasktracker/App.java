package tasktracker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class App {

    private static final Path DATA_FILE = Paths.get("tasks.json");

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        TaskStorage storage = new TaskStorage(DATA_FILE);

        try {
            String command = args[0];
            switch (command) {
                case "add"              -> handleAdd(storage, args);
                case "update"           -> handleUpdate(storage, args);
                case "delete"           -> handleDelete(storage, args);
                case "mark-in-progress" -> handleMarkStatus(storage, args, TaskStatus.IN_PROGRESS);
                case "mark-done"        -> handleMarkStatus(storage, args, TaskStatus.DONE);
                case "list"             -> handleList(storage, args);
                default -> {
                    System.out.println("Unknown command: " + command);
                    printUsage();
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ── Commands ────────────────────────────────────────────

    private static void handleAdd(TaskStorage storage, String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: task-cli add \"description\"");
            return;
        }
        List<Task> tasks = storage.load();
        int nextId = tasks.stream().mapToInt(Task::getId).max().orElse(0) + 1;
        Task task = new Task(nextId, args[1]);
        tasks.add(task);
        storage.save(tasks);
        System.out.println("Task added successfully (ID: " + nextId + ")");
    }

    private static void handleUpdate(TaskStorage storage, String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: task-cli update <id> \"new description\"");
            return;
        }
        int id = parseId(args[1]);
        List<Task> tasks = storage.load();
        Task task = findTask(tasks, id);
        task.setDescription(args[2]);
        storage.save(tasks);
        System.out.println("Task " + id + " updated successfully");
    }

    private static void handleDelete(TaskStorage storage, String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: task-cli delete <id>");
            return;
        }
        int id = parseId(args[1]);
        List<Task> tasks = storage.load();
        Task task = findTask(tasks, id);
        tasks.remove(task);
        storage.save(tasks);
        System.out.println("Task " + id + " deleted successfully");
    }

    private static void handleMarkStatus(TaskStorage storage, String[] args, TaskStatus status) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: task-cli " + args[0] + " <id>");
            return;
        }
        int id = parseId(args[1]);
        List<Task> tasks = storage.load();
        Task task = findTask(tasks, id);
        task.setStatus(status);
        storage.save(tasks);
        System.out.println("Task " + id + " marked as " + status.getValue());
    }

    private static void handleList(TaskStorage storage, String[] args) throws IOException {
        List<Task> tasks = storage.load();

        // Optional status filter
        if (args.length >= 2) {
            TaskStatus filter = TaskStatus.fromString(args[1]);
            tasks = tasks.stream()
                    .filter(t -> t.getStatus() == filter)
                    .toList();
        }

        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }

        // Print table header
        System.out.printf("%-4s %-40s %-15s %-20s%n", "ID", "Description", "Status", "Updated");
        System.out.println("-".repeat(81));

        for (Task task : tasks) {
            String desc = task.getDescription();
            if (desc.length() > 38) desc = desc.substring(0, 35) + "...";
            System.out.printf("%-4d %-40s %-15s %-20s%n",
                    task.getId(),
                    desc,
                    task.getStatus().getValue(),
                    task.getUpdatedAt().toString().replace("T", " "));
        }
    }

    // ── Helpers ─────────────────────────────────────────────

    private static int parseId(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid task ID: " + s);
        }
    }

    private static Task findTask(List<Task> tasks, int id) {
        return tasks.stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: ID " + id));
    }

    private static void printUsage() {
        System.out.println("""
                Task Tracker CLI — manage your tasks from the command line

                Usage:
                  task-cli add "description"           Add a new task
                  task-cli update <id> "description"   Update task description
                  task-cli delete <id>                 Delete a task
                  task-cli mark-in-progress <id>       Mark task as in-progress
                  task-cli mark-done <id>              Mark task as done
                  task-cli list                        List all tasks
                  task-cli list todo                   List tasks by status
                  task-cli list in-progress               (todo | in-progress | done)
                  task-cli list done
                """);
    }
}
