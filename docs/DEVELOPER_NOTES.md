# Developer Notes

## Architecture Overview

### 1. What is the entrypoint of this plugin?

JetBrains plugins do not have a single standard executable entrypoint (like a `main()` function). Instead, entrypoints
are defined declaratively in the plugin manifest (`src/main/resources/META-INF/plugin.xml`). The IDE discovers, loads,
and initializes plugin components dynamically based on extension points and lifecycle listeners:

- **VCS Checkin Handler Entrypoint**:
    - Extension point: `com.intellij.checkinHandlerFactory`
    - Implementation: `com.kudos.commit.KudosCheckinHandlerFactory`
    - Instantiated by the IDE whenever a commit UI (Commit tool window or Commit dialog) is constructed for a project.

- **Tool Window Entrypoint**:
    - Extension point: `com.intellij.toolWindow` (ID: `Kudos`)
    - Implementation: `com.kudos.settings.KudosToolWindowFactory`
    - Lazily instantiated when the user opens or activates the "Kudos" tool window.

- **Dynamic Plugin Lifecycle Listener Entrypoint**:
    - Application listener topic: `com.intellij.ide.plugins.DynamicPluginListener`
    - Implementation: `com.kudos.KudosDynamicPluginListener`
    - Hooked into application lifecycle events to handle dynamic plugin unloading and updates without requiring an IDE
      restart.

- **Persistent Service Entrypoint**:
    - Service: `com.kudos.settings.KudosSettingsState`
    - Managed by the IntelliJ Platform's service container (`@Service(Service.Level.APP)`), lazily loaded on first
      access via `KudosSettingsState.instance`.

---

### 2. How exactly is this plugin wired into the IDE?

The plugin wires into the IntelliJ Platform through configuration in `plugin.xml` and several core platform extension
points and services:

#### A. Plugin Manifest & Dependencies (`plugin.xml`)

- Declares platform dependencies:
    - `<plugin id="com.intellij.modules.platform"/>` for core platform functionality.
    - `<module name="intellij.platform.vcs.impl"/>` for Version Control System (VCS) integration.
- Declares `require-restart="false"` to support dynamic loading and unloading.
- Configures message bundles (`messages.MyMessageBundle`).

#### B. Commit Workflow Integration (`com.intellij.checkinHandlerFactory`)

- `KudosCheckinHandlerFactory` extends `CheckinHandlerFactory` and creates a `KudosCheckinHandler` per commit session.
- `KudosCheckinHandler` integrates with the IDE's commit flow in two ways:
    1. **UI Injection**: Overrides `getBeforeCheckinConfigurationPanel()` to return a `KudosCommitOptionsPanel` (
       implementing `RefreshableOnComponent`). This injects the "Give Kudos" checkbox and collaborator dropdown into the
       commit options panel (e.g., Git commit options area).
    2. **Commit Message Modification**: Overrides `beforeCheckin()`. Just before a commit is created, it checks whether
       Kudos is enabled and a collaborator is selected in `KudosSettingsState`. If so, it appends the
       `Co-authored-by: <attribution>` Git trailer to the commit message using `KudosCommitMessage.withTrailer()`.

#### C. Settings & Configuration Tool Window (`com.intellij.toolWindow`)

- Registered under `com.intellij.toolWindow` with ID `Kudos` (left anchor, secondary, `doNotActivateOnStart="true"`).
- `KudosToolWindowFactory` implements `ToolWindowFactory` and builds the tool window UI on demand by instantiating
  `KudosSettingsPanel` and adding it to the tool window's `ContentFactory`.
- `KudosSettingsPanel` provides a table editor for managing collaborators (name and email), previewing attributions, and
  enabling/disabling the Kudos commit UI.

#### D. Application-Level Persistence (`@Service` & `PersistentStateComponent`)

- `KudosSettingsState` is an application-level service annotated with `@Service(Service.Level.APP)` and
  `@State(name = "com.kudos.settings.KudosSettingsState", storages = [Storage("kudos.xml")])`.
- Automatically persists plugin configuration (selected collaborator, enabled status, collaborator map) to `kudos.xml`
  in the IDE's configuration directory across sessions.

#### E. Dynamic Plugin Lifecycle Management (`DynamicPluginListener`)

- `KudosDynamicPluginListener` listens to the `DynamicPluginListener` topic via `<applicationListeners>` in
  `plugin.xml`.
- When `beforePluginUnload` is triggered for the "Kudos" plugin:
  1. Calls `KudosCommitPlaceholder.disposeAll()` to remove all commit message block inlays and clean document user data.
  2. Calls `KudosCheckinHandler.disposeAll()` to detach active `KudosCheckinHandler` instances from long-lived VCS
     commit workflows (`ChangesViewCommitWorkflow._commitHandlers`), preventing classloader pinning.
  3. Invokes `IconLoader.clearCache()` to release cached SVG icon resources.

