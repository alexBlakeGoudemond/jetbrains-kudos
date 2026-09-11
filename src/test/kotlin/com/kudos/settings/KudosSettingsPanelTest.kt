package com.kudos.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KudosSettingsPanelTest : BasePlatformTestCase() {

    private fun freshPanel(): KudosSettingsPanel {
        KudosSettingsState.getInstance().resetToDefaults()
        val panel = KudosSettingsPanel()
        panel.component // force the lazy Swing tree to build so tableModel/table are populated
        return panel
    }

    fun `test table is seeded from settings defaults`() {
        val panel = freshPanel()

        val names = panel.tableModel.items.map { it.name }
        assertEquals(listOf("Claude", "GitHub Copilot", "ChatGPT"), names)
    }

    fun `test editing a table cell persists to settings`() {
        val panel = freshPanel()

        panel.tableModel.setValueAt("ada@example.com", 0, 1) // row 0 = Claude, column 1 = email
        val settings = KudosSettingsState.getInstance()

        assertEquals("ada@example.com", settings.collaborators["Claude"])
    }

    fun `test add row inserts an editable blank collaborator`() {
        val panel = freshPanel()
        val before = panel.tableModel.rowCount

        panel.addCollaborator()

        assertEquals(before + 1, panel.tableModel.rowCount)
        assertEquals("New collaborator", panel.tableModel.items.last().name)
    }

    fun `test remove selected row deletes it from settings`() {
        val panel = freshPanel()

        panel.table.setRowSelectionInterval(0, 0) // "Claude"
        panel.removeSelectedCollaborator()

        val settings = KudosSettingsState.getInstance()
        assertFalse(settings.collaborators.containsKey("Claude"))
    }

    fun `test preview label reflects the selected row`() {
        val panel = freshPanel()

        panel.tableModel.setValueAt("ada@example.com", 0, 1)
        panel.table.setRowSelectionInterval(0, 0)
        panel.updatePreview()

        assertEquals("Claude <ada@example.com>", panel.previewLabel.text)
    }

    fun `test preview falls back to first row when nothing selected`() {
        val panel = freshPanel()

        panel.table.clearSelection()
        panel.updatePreview()

        assertEquals("Claude", panel.previewLabel.text)
    }
}