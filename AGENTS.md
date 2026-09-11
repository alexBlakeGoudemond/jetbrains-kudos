AGENTS.md

# Purpose

This file helps AI Agents (and humans) quickly understand the Kudos plugin: what it does, where to find core logic, how
the pieces fit together, and common tasks an agent might be asked to perform.

# High-level summary

- Goal: Let developers manually add an attribution "Co-authored-by: <Name> <email>" to commit messages from the IDE
  commit UI. No detection or hooks; the developer chooses who to credit.
- Scope: Lightweight JetBrains plugin written in Kotlin.

# Key files and locations

- [plugin.xml](src/main/resources/META-INF/plugin.xml)
    - Declares plugin id/name/vendor, dependencies, and two extensions:
        - checkinHandlerFactory -> com.kudos.commit.KudosCheckinHandlerFactory
        - toolWindow (Kudos) -> com.kudos.settings.KudosToolWindowFactory

- Settings (persistence/service)
    - [KudosSettingsState](src/main/kotlin/com/kudos/settings/KudosSettingsState.kt)
        - Application-level @Service, stored in kudos.xml
        - Stores: giveKudosEnabled, kudosUiEnabled, collaborators map, selectedCollaborator
        - Helpers: formatAttribution(name) and currentAttributionOrNull()
        - Default collaborators: Claude, GitHub Copilot, ChatGPT

- Tool window UI
    - [KudosSettingsPanel](src/main/kotlin/com/kudos/settings/KudosSettingsPanel.kt)
        - Table-based editor for collaborator name/email, live persistence, preview label
    - [KudosToolWindowFactory](src/main/kotlin/com/kudos/settings/KudosToolWindowFactory.kt)
        - Registers the panel into the Kudos tool window

- Commit UI integration
    - [KudosCommitOptionsPanel](src/main/kotlin/com/kudos/commit/KudosCommitOptionsPanel.kt)
        - A RefreshableOnComponent placed into the commit dialog options area
        - Contains "Give Kudos" checkbox and a dropdown of collaborators
        - Reads/writes state via KudosSettingsState
    - [KudosCheckinHandlerFactory](src/main/kotlin/com/kudos/commit/KudosCheckinHandlerFactory.kt)
        - Factory registered as checkinHandlerFactory in plugin.xml
    - [KudosCheckinHandler](src/main/kotlin/com/kudos/commit/KudosCheckinHandler.kt)
        - Creates KudosCommitOptionsPanel and, beforeCheckin, appends attribution if enabled

- Commit message logic (pure, testable)
    - [KudosCommitMessage](src/main/kotlin/com/kudos/commit/KudosCommitMessage.kt)
        - trailerFor(attribution): returns "Co-authored-by: {attribution}"
        - withTrailer(message, attribution): appends trailer if missing (handles blank messages and avoids duplicates)

# Tests

- Unit tests (run with Gradle test):
    - `src/test/kotlin/**` contains tests for settings panel and commit message logic (e.g., KudosCommitMessage tests)

# Build & run

- Gradle wrapper: use gradlew.bat (Windows)
- Useful tasks:
    - `.\gradlew.bat runIde`  (launches the IDE with the plugin in sandbox)
    - `.\gradlew.bat build`   (builds plugin jar)

# Data flow (quick)

1. User opens Kudos tool window -> edits collaborators / toggles kudosUiEnabled.
2. Commit dialog opens -> KudosCommitOptionsPanel.refresh() reads state and sets checkbox/dropdown.
3. User checks "Give Kudos" and selects collaborator.
4. Before commit, KudosCheckinHandler.beforeCheckin() calls settings.currentAttributionOrNull() and appends trailer
   using KudosCommitMessage.withTrailer.

# Where an AI Agent should look to implement common changes

- Add a new collaborator preset or validation:
    - KudosSettingsState.defaultCollaborators(), KudosSettingsPanel.persistCollaborators()
- Change trailer format (e.g., different header):
    - KudosCommitMessage.trailerFor()
- Make collaborator selection per-project instead of global:
    - Change service level in KudosSettingsState from Service.Level.APP -> Service.Level.PROJECT and adapt storage
- Add telemetry/logging:
    - Insert logging in KudosCheckinHandler.beforeCheckin() or in the UI actions
- Improve UI/UX (sorting, reordering, search):
    - Edit KudosSettingsPanel.tableModel & ToolbarDecorator usage
- Make attribution optional per-commit automatically: modify beforeCheckin to obey CommitContext or changelist metadata

# Testing guidance for agents

- Unit tests: run gradle test and focus on KudosCommitMessage and settings tests.
- Manual verification: run the IDE via runIde and exercise the Kudos tool window + commit flow.

# Safety & conventions

- Settings persistence file: kudos.xml (stored in IDE config directory) — do not commit secrets here.
- Keep KudosCommitMessage logic independent of platform code (already done) to ease testing.

# Onboarding checklist for an AI Agent

1. Read plugin.xml to confirm registered extensions.
2. Open KudosSettingsState.kt to understand persisted fields and helpers.
3. Inspect KudosCommitMessage.kt for trailer formatting rules and tests.
4. Run unit tests locally (.\gradlew.bat test).
5. Run the plugin sandbox (.\gradlew.bat runIde) and exercise the complete manual flow:
    - Edit collaborators in Kudos tool window
    - Open commit dialog, toggle Give Kudos, choose collaborator
    - Commit and confirm commit message contains Co-authored-by trailer and no duplicate trailers

# Notes & contacts

- Author (from plugin.xml): Alex Blake-Goudemond <alexgoudemond@gmail.com>
- README and docs/README.md explain motivation and design decisions; read them for context before making behavior
  changes.

If you want, generate a short code map (functions/classes + responsibilities) or create targeted todos (e.g., convert
settings to per-project) — say which direction and a plan will be produced.