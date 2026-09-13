package com.kudos.commit

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.ui.CheckBoxList
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.kudos.settings.KudosSettingsListener
import com.kudos.settings.KudosSettingsState
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * The "Give Kudos" row shown in the commit dialog's options area.
 *
 * A unit of work can involve more than one collaborator at once (e.g. pairing with a colleague
 * *and* using an AI agent), so collaborators are shown as a [CheckBoxList] - tick as many as
 * apply - rather than a single-select dropdown. This also matches the checkbox-driven style of
 * the rest of the commit options panel (Update copyright, Reformat code, etc.).
 */
class KudosCommitOptionsPanel(private val settings: KudosSettingsState) : RefreshableOnComponent {

    @Suppress("DialogTitleCapitalization")
    val checkBox = JBCheckBox("Give Kudos")

    val collaboratorsList = CheckBoxList<String>().apply {
        // CheckBoxList's per-row focus border adds a hair of left padding that the other
        // Commit Checks rows don't have, which reads as the checkboxes being out of alignment.
        // Zeroing it here lines the checkboxes back up with "Give Kudos" and "Run Git hooks".
        border = JBUI.Borders.empty()
    }

    private val scrollPane = JBScrollPane(collaboratorsList).apply {
        preferredSize = Dimension(220, 90)
        // Remove scroll pane chrome so the outer accent border reads as a single container.
        border = JBUI.Borders.empty()
        viewportBorder = JBUI.Borders.empty()
    }
 
    private val rootPanel = JPanel(BorderLayout(0, 4)).apply {
        add(checkBox, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        // A purple accent border to visually flag this section as plugin-contributed rather
        // than a built-in IDE commit check. Uses a light/dark pair so it stays visible in
        // either theme instead of picking one that only reads well in Darcula. Extra top
        // padding specifically so the "Give Kudos" checkbox has clear space below the line.
        border = JBUI.Borders.compound(
            JBUI.Borders.customLine(KUDOS_ACCENT_COLOR, 1),
            // Slightly larger top inset so the checkbox nests inside the accent border visually.
            JBUI.Borders.empty(12, 8, 8, 8)
        )
    }

    init {
        reloadListModel()

        checkBox.addActionListener {
            settings.giveKudosEnabled = checkBox.isSelected
            applyUiEnabledState()
        }

        // Fires after CheckBoxList has already applied the click to its backing JCheckBox, so
        // checkedItems reflects the settled state - persist it straight away.
        collaboratorsList.setCheckBoxListListener { _, _ ->
            settings.selectedCollaborators = checkedNames()
        }

        restoreState()

        // Subscribe to settings changes so the commit options update live when collaborators are edited
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            KudosSettingsState.KUDOS_SETTINGS_TOPIC,
            object : KudosSettingsListener {
                override fun collaboratorsChanged() {
                    if (SwingUtilities.isEventDispatchThread()) {
                        reloadListModel()
                        applyUiEnabledState()
                    } else {
                        SwingUtilities.invokeLater {
                            reloadListModel()
                            applyUiEnabledState()
                        }
                    }
                }
            }
        )
    }

    /** Rebuilds the checkbox rows from settings, restoring which ones were previously checked. */
    private fun reloadListModel() {
        val selected = settings.selectedCollaborators
        collaboratorsList.clear()
        settings.collaborators.keys.forEach { name ->
            collaboratorsList.addItem(name, name, name in selected)
        }
    }

    private fun checkedNames(): Set<String> = collaboratorsList.checkedItems.toCollection(LinkedHashSet())

    private fun applyUiEnabledState() {
        val uiEnabled = settings.kudosUiEnabled
        checkBox.isEnabled = uiEnabled
        collaboratorsList.isEnabled = uiEnabled && checkBox.isSelected

        val tooltip = if (uiEnabled) null else "Kudos is currently disabled. Enable it from the Kudos tool window."
        checkBox.toolTipText = tooltip
        collaboratorsList.toolTipText = tooltip
    }

    override fun getComponent(): JComponent = rootPanel

    /** Called when the commit dialog reopens - picks up edits made in the Kudos tool window meanwhile. */
    override fun refresh() {
        reloadListModel()
        checkBox.isSelected = settings.giveKudosEnabled
        applyUiEnabledState()
    }

    override fun saveState() {
        settings.giveKudosEnabled = checkBox.isSelected
        settings.selectedCollaborators = checkedNames()
    }

    override fun restoreState() {
        checkBox.isSelected = settings.giveKudosEnabled
        reloadListModel()
        applyUiEnabledState()
    }

    companion object {
        /** Light/dark pair so the accent border reads clearly in both IDE themes. */
        private val KUDOS_ACCENT_COLOR = JBColor(0x8759B3, 0xB39DDB)
    }
}