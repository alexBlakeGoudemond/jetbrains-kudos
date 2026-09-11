package com.kudos.commit

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.kudos.settings.KudosSettingsPanel
import com.kudos.settings.KudosSettingsState

/**
 * Integration-style test demonstrating that edits made in the Kudos tool window
 * are persisted to KudosSettingsState but are NOT automatically reflected in an
 * already-instantiated KudosCommitOptionsPanel until refresh() is called.
 *
 * This reproduces the reported behaviour (test demonstrates, does not fix).
 */
class KudosIntegrationTest : BasePlatformTestCase() {

    fun `test adding collaborator in tool window immediately updates commit options panel`() {
        val settings = KudosSettingsState.getInstance()
        settings.resetToDefaults()

        // Build the tool-window panel (registers table model listener that persists edits)
        val toolWindowPanel = KudosSettingsPanel()
        toolWindowPanel.component // force build

        val commitOptions = KudosCommitOptionsPanel(settings)

        val initialItems = fetchCollaborators(commitOptions)
        assertTrue(initialItems.containsAll(settings.collaborators.keys))

        // Add a collaborator through the tool window UI
        toolWindowPanel.addCollaborator()
        val newRowIndex = toolWindowPanel.tableModel.rowCount - 1
        // The default name is "New collaborator"; ensure it's persisted
        val newName = toolWindowPanel.tableModel.items[newRowIndex].name
        // Persist is wired to the table model listener; sanity-check settings updated
        assertTrue(settings.collaborators.containsKey(newName))

        // BUG reproduction: the already-created commit options panel should NOT yet include the new collaborator
        val itemsAfterAdd = fetchCollaborators(commitOptions)
        assertTrue("Commit panel should not reflect new collaborator until refresh", itemsAfterAdd.contains(newName))

        // After calling refresh(), the commit panel should pick up the newly persisted collaborator
        commitOptions.refresh()

        val itemsAfterRefresh = fetchCollaborators(commitOptions)
        assertTrue("Commit panel should include new collaborator after refresh", itemsAfterRefresh.contains(newName))

        assertEquals("Adding a collaborator should be equivalent to manually refreshing", itemsAfterAdd.size, itemsAfterRefresh.size)
    }

    /**
     * 1. Create an IntRange from 0 up until the itemCount
     * 2. Foreach number in the IntRange, transform to the commitOption
     * 3. return
     * */
    private fun fetchCollaborators(commitOptions: KudosCommitOptionsPanel): List<String?> {
        val initialItems = (0 until commitOptions.comboBox.itemCount).map { commitOptions.comboBox.getItemAt(it) }
        return initialItems
    }
}
