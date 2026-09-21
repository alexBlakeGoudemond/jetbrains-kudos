package com.kudos.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil
import com.kudos.infrastructure.kudosLogger

/**
 * Listener for Kudos settings changes published over the application message bus.
 */
interface KudosSettingsListener {
    fun collaboratorsChanged()

    /**
     * The "Give Kudos" checkbox, the tool window master toggle, or the set of ticked collaborators
     * changed. Has an empty default so existing listeners don't have to care about it.
     */
    fun selectionChanged() {}
}

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
class KudosSettingsState : PersistentStateComponent<KudosSettingsState.State>, Disposable {

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
            if (myState.giveKudosEnabled == value) return
            myState.giveKudosEnabled = value
            fireSelectionChanged()
        }

    /** Master toggle from the Kudos Tool Window. When false, the commit-UI checkbox is disabled. */
    var kudosUiEnabled: Boolean
        get() = myState.kudosUiEnabled
        set(value) {
            if (myState.kudosUiEnabled == value) return
            myState.kudosUiEnabled = value
            fireSelectionChanged()
        }

    /**
     * The collaborators currently checked for this unit of work. A [LinkedHashSet] to preserve
     * selection order (so e.g. "colleague first, then AI" produces trailers in that order) while
     * still deduplicating - the commit UI can't select the same collaborator twice anyway, but
     * callers (and old persisted state) shouldn't be able to sneak in a duplicate.
     */
    var selectedCollaborators: Set<String>
        get() = myState.selectedCollaborators.toCollection(LinkedHashSet())
        set(value) {
            val updated = value.toCollection(LinkedHashSet()).toMutableList()
            if (updated == myState.selectedCollaborators) return
            myState.selectedCollaborators = updated
            fireSelectionChanged()
        }

    val collaborators: Map<String, String?>
        get() = myState.collaborators.mapValues { (_, email) -> email.ifBlank { null } }

    fun setCollaborators(collaborators: Map<String, String?>) {
        myState.collaborators = collaborators
            .mapValues { (_, email) -> email ?: "" }
            .toMutableMap()

        // Drop any previously-selected collaborators that no longer exist, rather than leaving
        // dangling references to removed entries. Unlike the old single-selection fallback, we
        // don't auto-select a replacement here: with multiple collaborators, silently picking
        // "whichever is first" on someone's behalf is more likely to be wrong than helpful.
        myState.selectedCollaborators = myState.selectedCollaborators
            .filter { it in myState.collaborators }
            .toMutableList()

        // Notify listeners that collaborators changed so UI components can refresh live
        ApplicationManager.getApplication().messageBus
            .syncPublisher(KUDOS_SETTINGS_TOPIC)
            .collaboratorsChanged()
    }

    fun resetToDefaults() {
        myState = State()
        fireSelectionChanged()
    }

    private fun fireSelectionChanged() {
        ApplicationManager.getApplication().messageBus
            .syncPublisher(KUDOS_SETTINGS_TOPIC)
            .selectionChanged()
    }

    /**
     * Formats the git trailer for a given collaborator name, per the doc's default:
     * `collaborator <collaborationEmail>` - or just the name if no email is set.
     */
    fun formatAttribution(name: String): String {
        val email = collaborators[name]
        return if (email.isNullOrBlank()) name else "$name <$email>"
    }

    /**
     * Names only (never emails) of the collaborators currently selected, in selection order, skipping
     * any that no longer exist. This is the single source of truth for "who will be credited", shared
     * by the commit trailers and the commit-box hint so the two can never disagree.
     */
    fun currentNames(): List<String> =
        selectedCollaborators.filter { it in collaborators }

    /**
     * Convenience for the commit-handler step: the formatted trailer text for every currently
     * selected collaborator, in selection order. Empty if nothing is selected.
     */
    fun currentAttributions(): List<String> =
        currentNames().map { formatAttribution(it) }

    override fun dispose() {
        LOG.info("Disposing " + Companion::class.java.name + " underway")
    }

    class State {
        var giveKudosEnabled: Boolean = true
        var kudosUiEnabled: Boolean = true
        var collaborators: MutableMap<String, String> = defaultCollaborators()
        var selectedCollaborators: MutableList<String> = mutableListOf()
    }

    companion object {
        private val LOG = kudosLogger<KudosSettingsState>()

        fun getInstance(): KudosSettingsState = service()

        fun defaultCollaborators(): MutableMap<String, String> = linkedMapOf(
            "Claude" to "",
            "GitHub Copilot" to "",
            "ChatGPT" to "",
        )

        /**
         * Message-bus topic for notifying UI components about settings changes.
         * Subscribers should update UI on the EDT when collaboratorsChanged() is invoked.
         */

        val KUDOS_SETTINGS_TOPIC: Topic<KudosSettingsListener> =
            Topic.create("KudosSettings", KudosSettingsListener::class.java)
    }
}