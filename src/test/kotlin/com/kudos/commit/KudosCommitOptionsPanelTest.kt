package com.kudos.commit

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.kudos.settings.KudosSettingsState

class KudosCommitOptionsPanelTest : BasePlatformTestCase() {

    private fun freshPanel(): KudosCommitOptionsPanel {
        val settings = KudosSettingsState.getInstance()
        settings.resetToDefaults()
        return KudosCommitOptionsPanel(settings)
    }

    fun `test dropdown is seeded from settings collaborators`() {
        val panel = freshPanel()
        val items = (0 until panel.comboBox.itemCount).map { panel.comboBox.getItemAt(it) }

        assertEquals(listOf("Claude", "GitHub Copilot", "ChatGPT"), items)
    }

    fun `test checking the box persists giveKudosEnabled`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()
        panel.checkBox.isSelected = false

        panel.checkBox.doClick()

        assertTrue(settings.giveKudosEnabled)
    }

    fun `test selecting a collaborator persists selectedCollaborator`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()

        panel.comboBox.selectedItem = "GitHub Copilot"

        assertEquals("GitHub Copilot", settings.selectedCollaborator)
    }

    fun `test disabling kudos UI disables checkbox and dropdown with a tooltip`() {
        val panel = freshPanel()
        val settings = KudosSettingsState.getInstance()
        settings.kudosUiEnabled = false

        panel.restoreState()

        assertFalse(panel.checkBox.isEnabled)
        assertFalse(panel.comboBox.isEnabled)
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

        val items = (0 until panel.comboBox.itemCount).map { panel.comboBox.getItemAt(it) }
        assertEquals(listOf("New Person"), items)
    }
}