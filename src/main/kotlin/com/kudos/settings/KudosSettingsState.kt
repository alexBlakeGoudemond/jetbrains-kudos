package com.kudos.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level (global, not per-project/per-repo) settings for Kudos.
 *
 * This is a "light service" - the @Service annotation is enough for the platform
 * to discover and instantiate it, no <applicationService> entry needed in plugin.xml.
 *
 * Persisted to a dedicated kudos.xml under the IDE's config directory, rather than
 * bundled into other.xml, to keep it easy to find/inspect/delete during development.
 */
@Service(Service.Level.APP)
@State(name = "KudosSettings", storages = [Storage("kudos.xml")])
class KudosSettingsState : PersistentStateComponent<KudosSettingsState.State> {

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    // --- Public API -----------------------------------------------------
    //
    // The doc spec describes collaborator emails as `String?` (null = no email).
    // IntelliJ's XML state serializer handles plain Maps<String, String> more
    // reliably than maps containing null values, so internally we store "" for
    // "no email" and translate at this boundary. Callers never need to know that.

    var giveKudosEnabled: Boolean
        get() = myState.giveKudosEnabled
        set(value) {
            myState.giveKudosEnabled = value
        }

    /** Master toggle from the Kudos Tool Window. When false, the commit-UI checkbox is disabled. */
    var kudosUiEnabled: Boolean
        get() = myState.kudosUiEnabled
        set(value) {
            myState.kudosUiEnabled = value
        }

    var selectedCollaborator: String?
        get() = myState.selectedCollaborator.ifBlank { null }
        set(value) {
            myState.selectedCollaborator = value ?: ""
        }

    val collaborators: Map<String, String?>
        get() = myState.collaborators.mapValues { (_, email) -> email.ifBlank { null } }

    fun setCollaborators(collaborators: Map<String, String?>) {
        myState.collaborators = collaborators
            .mapValues { (_, email) -> email ?: "" }
            .toMutableMap()

        // If the previously-selected collaborator no longer exists, fall back sensibly
        // rather than leaving a dangling reference to a removed entry.
        if (myState.selectedCollaborator !in myState.collaborators) {
            myState.selectedCollaborator = myState.collaborators.keys.firstOrNull() ?: ""
        }
    }

    fun resetToDefaults() {
        myState = State()
    }

    /**
     * Formats the git trailer for a given collaborator name, per the doc's default:
     * `collaborator <collaborationEmail>` - or just the name if no email is set.
     */
    fun formatAttribution(name: String): String {
        val email = collaborators[name]
        return if (email.isNullOrBlank()) name else "$name <$email>"
    }

    /** Convenience for the commit-handler step: the formatted trailer for whatever's currently selected. */
    fun currentAttributionOrNull(): String? =
        selectedCollaborator?.let { formatAttribution(it) }

    class State {
        var giveKudosEnabled: Boolean = true
        var kudosUiEnabled: Boolean = true
        var collaborators: MutableMap<String, String> = defaultCollaborators()
        var selectedCollaborator: String = ""
    }

    companion object {
        fun getInstance(): KudosSettingsState = service()

        fun defaultCollaborators(): MutableMap<String, String> = linkedMapOf(
            "Claude" to "",
            "GitHub Copilot" to "",
            "ChatGPT" to "",
        )
    }
}
