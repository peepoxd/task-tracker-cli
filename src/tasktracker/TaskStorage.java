package tasktracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes tasks to a JSON file.
 * No external JSON library — hand-rolled parser for the known schema.
 */
public class TaskStorage {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final Path filePath;

    public TaskStorage(Path filePath) {
        this.filePath = filePath;
    }

    // ── Write ────────────────────────────────────────────────

    public void save(List<Task> tasks) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < tasks.size(); i++) {
            sb.append(tasks.get(i).toJson());
            if (i < tasks.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n");
        Files.writeString(filePath, sb.toString());
    }

    // ── Read ─────────────────────────────────────────────────

    public List<Task> load() throws IOException {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        String content = Files.readString(filePath).trim();
        if (content.isEmpty() || content.equals("[]")) {
            return new ArrayList<>();
        }
        return parseArray(content);
    }

    // ── Minimal JSON parser (for known Task schema only) ────

    private List<Task> parseArray(String json) {
        List<Task> tasks = new ArrayList<>();
        int i = skipWhitespace(json, 0);
        if (json.charAt(i) != '[') {
            throw new RuntimeException("Expected '[' at position " + i);
        }
        i = skipWhitespace(json, i + 1);

        while (i < json.length() && json.charAt(i) != ']') {
            if (json.charAt(i) == ',') {
                i = skipWhitespace(json, i + 1);
                continue;
            }
            ParseResult<Task> result = parseObject(json, i);
            tasks.add(result.value);
            i = skipWhitespace(json, result.nextIndex);
        }
        return tasks;
    }

    private ParseResult<Task> parseObject(String json, int start) {
        int i = skipWhitespace(json, start);
        if (json.charAt(i) != '{') {
            throw new RuntimeException("Expected '{' at position " + i);
        }
        i = skipWhitespace(json, i + 1);

        int id = 0;
        String description = "";
        String status = "todo";
        String createdAt = "";
        String updatedAt = "";

        while (i < json.length() && json.charAt(i) != '}') {
            if (json.charAt(i) == ',') {
                i = skipWhitespace(json, i + 1);
                continue;
            }

            // Parse key
            ParseResult<String> keyResult = parseString(json, i);
            String key = keyResult.value;
            i = skipWhitespace(json, keyResult.nextIndex);

            // Expect ':'
            if (json.charAt(i) != ':') {
                throw new RuntimeException("Expected ':' at position " + i);
            }
            i = skipWhitespace(json, i + 1);

            // Parse value
            switch (key) {
                case "id" -> {
                    ParseResult<Integer> numResult = parseNumber(json, i);
                    id = numResult.value;
                    i = numResult.nextIndex;
                }
                case "description" -> {
                    ParseResult<String> strResult = parseString(json, i);
                    description = strResult.value;
                    i = strResult.nextIndex;
                }
                case "status" -> {
                    ParseResult<String> strResult = parseString(json, i);
                    status = strResult.value;
                    i = strResult.nextIndex;
                }
                case "createdAt" -> {
                    ParseResult<String> strResult = parseString(json, i);
                    createdAt = strResult.value;
                    i = strResult.nextIndex;
                }
                case "updatedAt" -> {
                    ParseResult<String> strResult = parseString(json, i);
                    updatedAt = strResult.value;
                    i = strResult.nextIndex;
                }
                default -> {
                    // skip unknown value
                    i = skipValue(json, i);
                }
            }
            i = skipWhitespace(json, i);
        }

        // skip closing '}'
        if (i < json.length() && json.charAt(i) == '}') {
            i++;
        }

        Task task = new Task(
            id,
            description,
            TaskStatus.fromString(status),
            LocalDateTime.parse(createdAt, FMT),
            LocalDateTime.parse(updatedAt, FMT)
        );
        return new ParseResult<>(task, i);
    }

    private ParseResult<String> parseString(String json, int start) {
        int i = skipWhitespace(json, start);
        if (json.charAt(i) != '"') {
            throw new RuntimeException("Expected '\"' at position " + i);
        }
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < json.length() && json.charAt(i) != '"') {
            if (json.charAt(i) == '\\' && i + 1 < json.length()) {
                i++;
                switch (json.charAt(i)) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    default:   sb.append(json.charAt(i));
                }
            } else {
                sb.append(json.charAt(i));
            }
            i++;
        }
        if (i < json.length()) i++; // skip closing '"'
        return new ParseResult<>(sb.toString(), i);
    }

    private ParseResult<Integer> parseNumber(String json, int start) {
        int i = skipWhitespace(json, start);
        int sign = 1;
        if (json.charAt(i) == '-') {
            sign = -1;
            i++;
        }
        int num = 0;
        while (i < json.length() && Character.isDigit(json.charAt(i))) {
            num = num * 10 + (json.charAt(i) - '0');
            i++;
        }
        return new ParseResult<>(sign * num, i);
    }

    private int skipValue(String json, int start) {
        int i = skipWhitespace(json, start);
        char c = json.charAt(i);
        if (c == '"') {
            return parseString(json, i).nextIndex;
        } else if (c == '{') {
            return skipBlock(json, i, '{', '}');
        } else if (c == '[') {
            return skipBlock(json, i, '[', ']');
        } else {
            // number, boolean, null
            while (i < json.length() && ",]} \t\n\r".indexOf(json.charAt(i)) == -1) {
                i++;
            }
            return i;
        }
    }

    private int skipBlock(String json, int start, char open, char close) {
        int depth = 0;
        int i = start;
        boolean inString = false;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (inString) {
                if (c == '\\') { i += 2; continue; }
                if (c == '"') inString = false;
            } else {
                if (c == '"') inString = true;
                else if (c == open) depth++;
                else if (c == close) { depth--; if (depth == 0) return i + 1; }
            }
            i++;
        }
        return i;
    }

    private int skipWhitespace(String json, int i) {
        while (i < json.length() && " \t\n\r".indexOf(json.charAt(i)) != -1) {
            i++;
        }
        return i;
    }

    private static class ParseResult<T> {
        final T value;
        final int nextIndex;

        ParseResult(T value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }
}
