package com.kudos.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * The commit-box hint repaints off [KudosSettingsListener.selectionChanged], so these pin down when it fires -
 * and, just as importantly, when it doesn't.
 */
class KudosSettingsEventsTest : BasePlatformTestCase() {

    private var selectionEvents = 0

    override fun setUp() {
        super.setUp()
        KudosSettingsState.getInstance().resetToDefaults()
        selectionEvents = 0

        // Subscribe *after* the reset above so its event isn't counted.
        ApplicationManager.getApplication().messageBus.connect(testRootDisposable).subscribe(
            KudosSettingsState.KUDOS_SETTINGS_TOPIC,
            object : KudosSettingsListener {
                override fun collaboratorsChanged() {}
                override fun selectionChanged() {
                    selectionEvents++
                }
            }
        )
    }

    fun `test toggling Give Kudos publishes once`() {
        KudosSettingsState.getInstance().giveKudosEnabled = false

        assertEquals(1, selectionEvents)
    }

    fun `test toggling the tool window master switch publishes once`() {
        KudosSettingsState.getInstance().kudosUiEnabled = false

        assertEquals(1, selectionEvents)
    }

    fun `test ticking a collaborator publishes once`() {
        KudosSettingsState.getInstance().selectedCollaborators = setOf("Claude")

        assertEquals(1, selectionEvents)
    }

    fun `test writing an unchanged value publishes nothing`() {
        val settings = KudosSettingsState.getInstance()

        settings.giveKudosEnabled = settings.giveKudosEnabled
        settings.kudosUiEnabled = settings.kudosUiEnabled
        settings.selectedCollaborators = settings.selectedCollaborators

        assertEquals(0, selectionEvents)
    }

    fun `test currentNames returns names only and skips removed collaborators`() {
        val settings = KudosSettingsState.getInstance()
        settings.setCollaborators(mapOf("Ada Lovelace" to "ada@example.com", "Claude" to null))
        settings.selectedCollaborators = linkedSetOf("Ada Lovelace", "Claude", "Long Gone")

        assertEquals(listOf("Ada Lovelace", "Claude"), settings.currentNames())
    }
}