package com.kudos.commit

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.kudos.settings.KudosSettingsState

class KudosCommitOptionsPanelTest : BasePlatformTestCase() {

    private fun freshPanel(): KudosCommitOptionsPanel {
        val settings = KudosSettingsState.getInstance()
        settings.resetToDefaults()
        return KudosCommitOptionsPanel(settings)
    }

    private fun listItems(panel: KudosCommitOptionsPanel): List<String?> =
        (0 until panel.collaboratorsList.model.size).map { panel.collaboratorsList.getItemAt(it) }

    fun `test list is seeded from settings collaborators, all unchecked`() {
        val panel = freshPanel()

        assertEquals(listOf("Claude", "GitHub Copilot", "ChatGPT"), listItems(panel))
        assertTrue(panel.collaboratorsList.checkedItems.isEmpty())
    }

    fun `test checking the box persists giveKudosEnabled`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()
        panel.checkBox.isSelected = false

        panel.checkBox.doClick()

        assertTrue(settings.giveKudosEnabled)
    }

    fun `test checking one collaborator and saving persists selectedCollaborators`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()

        panel.collaboratorsList.setItemSelected("GitHub Copilot", true)
        panel.saveState()

        assertEquals(setOf("GitHub Copilot"), settings.selectedCollaborators)
    }

    fun `test checking multiple collaborators and saving persists all of them`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()

        panel.collaboratorsList.setItemSelected("Claude", true)
        panel.collaboratorsList.setItemSelected("ChatGPT", true)
        panel.saveState()

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

    fun `test refresh preserves a still-valid checked selection`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()
        panel.collaboratorsList.setItemSelected("Claude", true)
        panel.collaboratorsList.setItemSelected("ChatGPT", true)
        panel.saveState()

        // Unrelated settings change elsewhere shouldn't disturb an existing valid selection.
        settings.setCollaborators(settings.collaborators + ("New Person" to null))
        panel.refresh()

        assertEquals(setOf("Claude", "ChatGPT"), settings.selectedCollaborators)
        assertTrue(panel.collaboratorsList.isItemSelected("Claude"))
        assertTrue(panel.collaboratorsList.isItemSelected("ChatGPT"))
        assertFalse(panel.collaboratorsList.isItemSelected("New Person"))
    }
}