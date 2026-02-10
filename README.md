# Task Tracker CLI

Command-line task manager built with **pure Java 17** — zero external dependencies.

> From [roadmap.sh/projects/task-tracker](https://roadmap.sh/projects/task-tracker)

## Why Zero Dependencies?

This project intentionally avoids Maven, Gradle, Jackson, Gson, and any external library. Everything is hand-written:

- **JSON parser** — recursive descent parser in `TaskStorage.java` (~100 lines) handles strings, numbers, escape sequences, and nested structures
- **JSON writer** — manual StringBuilder serialization with proper escaping
- **CLI argument parser** — switch expression dispatching in `App.java`
- **Build** — plain `javac` via `build.sh`

## Usage

```bash
./build.sh                              # compile
./task-cli add "Design REST API"        # add task
./task-cli list                         # list all
./task-cli mark-in-progress 1           # update status
./task-cli mark-done 1
./task-cli list done                    # filter by status
./task-cli update 1 "Design gRPC API"  # edit description
./task-cli delete 1                     # remove
```

## Architecture

```
4 files, 0 dependencies

App.java          → CLI entry point, command dispatch (switch expression)
Task.java         → Model with id, description, status, timestamps
TaskStatus.java   → Enum: todo → in-progress → done
TaskStorage.java  → Hand-rolled JSON parser/writer (recursive descent)

Data: tasks.json (auto-created in working directory)
```

## Key Concepts

- **Recursive descent parsing** — `parseArray()` → `parseObject()` → `parseString()`/`parseNumber()` with `ParseResult<T>` carrying both value and next index
- **Escape handling** — supports `\"`, `\\`, `\n`, `\r`, `\t` in both read and write
- **Unknown field tolerance** — `skipValue()` skips any JSON type (string, object, array, number, boolean) gracefully
- **Stream API** — filtering, max ID calculation, and search via Java streams
