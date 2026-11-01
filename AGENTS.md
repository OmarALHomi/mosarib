# Instructions for coding agents (Antigravity and others)

This project uses a planner / implementer workflow:
- **Claude Code** plans, writes task specs and reviews the result.
- **You (the coding agent)** implement the tasks.

Before doing any work, read `.ai/README.md` and follow it exactly.

Short version:
1. Take the lowest-numbered task in `.ai/tasks/` whose header says `status: ready`.
2. Change only the files the task allows. Keep every user-visible string, colour and layout
   unless the task asks to change them.
3. Build and test from the project root:
   `./gradlew :app:compileDebugKotlin --console=plain -q` then
   `./gradlew :app:testDebugUnitTest --console=plain -q`
   (a KSP "AWT-EventQueue NullPointerException" printed by the test task is harmless noise).
4. Write your report to `.ai/reports/<same number>.md` using `.ai/templates/report.md`,
   then set the task's status to `done`.
5. Never commit, push, create branches, or run `git checkout/reset/stash` unless the task
   explicitly says so.
