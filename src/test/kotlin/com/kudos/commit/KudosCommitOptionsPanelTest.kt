package com.kudos.commit

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.kudos.settings.KudosSettingsState

class KudosCommitOptionsPanelTest : BasePlatformTestCase() {

    private fun freshPanel(): KudosCommitOptionsPanel {
        val settings = KudosSettingsState.getInstance()
        settings.resetToDefaults()
        return KudosCommitOptionsPanel(settings)
    }

    private fun listItems(panel: KudosCommitOptionsPanel): List<String> =
        (0 until panel.collaboratorsList.model.size).map { panel.collaboratorsList.model.getElementAt(it) }

    /** Selects the rows for [names] the way a user clicking with Cmd/Ctrl held down would. */
    private fun selectByName(panel: KudosCommitOptionsPanel, vararg names: String) {
        panel.collaboratorsList.clearSelection()
        val model = panel.collaboratorsList.model
        for (index in 0 until model.size) {
            if (model.getElementAt(index) in names) {
                panel.collaboratorsList.addSelectionInterval(index, index)
            }
        }
    }

    fun `test list is seeded from settings collaborators`() {
        val panel = freshPanel()

        assertEquals(listOf("Claude", "GitHub Copilot", "ChatGPT"), listItems(panel))
    }

    fun `test list allows selecting more than one collaborator`() {
        val panel = freshPanel()

        assertEquals(
            javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
            panel.collaboratorsList.selectionMode
        )
    }

    fun `test checking the box persists giveKudosEnabled`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()
        panel.checkBox.isSelected = false

        panel.checkBox.doClick()

        assertTrue(settings.giveKudosEnabled)
    }

    fun `test selecting one collaborator persists selectedCollaborators`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()

        selectByName(panel, "GitHub Copilot")

        assertEquals(setOf("GitHub Copilot"), settings.selectedCollaborators)
    }

    fun `test selecting multiple collaborators persists all of them`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()

        selectByName(panel, "Claude", "ChatGPT")

        assertEquals(setOf("Claude", "ChatGPT"), settings.selectedCollaborators)
    }

    fun `test disabling kudos UI disables checkbox and list with a tooltip`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()
        settings.kudosUiEnabled = false

        panel.restoreState()

        assertFalse(panel.checkBox.isEnabled)
        assertFalse(panel.collaboratorsList.isEnabled)
        assertNotNull(panel.checkBox.toolTipText)
    }

    fun `test enabling kudos UI clears the tooltip and re-enables the checkbox`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()
        settings.kudosUiEnabled = true

        panel.restoreState()

        assertTrue(panel.checkBox.isEnabled)
        assertNull(panel.checkBox.toolTipText)
    }

    fun `test refresh picks up collaborator edits made elsewhere`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()

        settings.setCollaborators(mapOf("New Person" to null))
        panel.refresh()

        assertEquals(listOf("New Person"), listItems(panel))
    }

    fun `test refresh preserves a still-valid selection`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()
        selectByName(panel, "Claude", "ChatGPT")

        // Unrelated settings change elsewhere shouldn't disturb an existing valid selection.
        settings.setCollaborators(settings.collaborators + ("New Person" to null))
        panel.refresh()

        assertEquals(setOf("Claude", "ChatGPT"), settings.selectedCollaborators)
    }
}
