# Date-phrase parser spike

Throwaway Java 21 prototype for Phase 0. It is not production code and has no Android or third-party dependencies.

Run:

```bash
JAVA_HOME=/path/to/jdk-21 ./run.sh
```

Expected final line: `N passed, 0 failed`.

## Decisions captured by the prototype

- `tomorrow` and `today` resolve against the device date.
- `next week`, `wait a week`, `in N weeks`, `next month`, and `this weekend` resolve against the current list date.
- `in N days` resolves against the device date per the table in `06-scheduling-dates-calendar.md`. It is flagged for confirmation because the same document's rules section implies the current list date.
- `next shopping day` requires the recurring schedule and is not resolved by this parser.
- A resolved date before the device date requires confirmation.
- The nearest list is the earliest existing list on or after the resolved date. If none exists, a list for the resolved date is proposed.
- `this weekend` uses the next Saturday on or after the current list date, so a Saturday list resolves to itself.
