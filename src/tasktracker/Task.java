package tasktracker;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Task {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private int id;
    private String description;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Task(int id, String description) {
        this.id = id;
        this.description = description;
        this.status = TaskStatus.TODO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Task(int id, String description, TaskStatus status,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() { return id; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String toJson() {
        return "    {\n"
             + "      \"id\": " + id + ",\n"
             + "      \"description\": \"" + escapeJson(description) + "\",\n"
             + "      \"status\": \"" + status.getValue() + "\",\n"
             + "      \"createdAt\": \"" + createdAt.format(FMT) + "\",\n"
             + "      \"updatedAt\": \"" + updatedAt.format(FMT) + "\"\n"
             + "    }";
    }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s)", id, description, status.getValue());
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}
