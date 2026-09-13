package com.kudos.commit

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.kudos.settings.KudosSettingsListener
import com.kudos.settings.KudosSettingsState
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities

/**
 * The "Give Kudos" row shown in the commit dialog's options area.
 *
 * A unit of work can involve more than one collaborator at once (e.g. pairing with a colleague
 * *and* using an AI agent), so collaborators are shown as a multi-select list rather than a
 * single-select dropdown: hold Cmd/Ctrl (or Shift for a range) to select more than one. The list
 * is wrapped in a scroll pane so it stays a fixed, comfortable size regardless of how many
 * collaborators are configured.
 */
class KudosCommitOptionsPanel(private val settings: KudosSettingsState) : RefreshableOnComponent {

    // When true, selection-listener persistence is temporarily suppressed to avoid persisting
    // intermediate empty selections while the UI rebuilds the list model and reapplies a valid
    // selection (e.g. during refresh/collaboratorsChanged updates).
    private var suppressSelectionPersistence: Boolean = false

    @Suppress("DialogTitleCapitalization")
    val checkBox = JBCheckBox("Give Kudos")

    val collaboratorsList = JBList(DefaultListModel<String>()).apply {
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        visibleRowCount = 4
    }

    private val scrollPane = JBScrollPane(collaboratorsList).apply {
        preferredSize = Dimension(220, 90)
    }

    private val rootPanel = JPanel(BorderLayout(0, 4)).apply {
        add(checkBox, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
    }

    init {
        reloadListModel()

        checkBox.addActionListener {
            settings.giveKudosEnabled = checkBox.isSelected
            applyUiEnabledState()
        }
        collaboratorsList.addListSelectionListener { event ->
            if (!suppressSelectionPersistence) {
                // Persist selection immediately; doing this on every event avoids race conditions where
                // other code updates collaborators before the final adjustment event has been delivered.
                settings.selectedCollaborators = selectedNames()
            }
        }

        restoreState()

        // Subscribe to settings changes so the commit options update live when collaborators are edited
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            KudosSettingsState.KUDOS_SETTINGS_TOPIC,
            object : KudosSettingsListener {
                override fun collaboratorsChanged() {
                    if (SwingUtilities.isEventDispatchThread()) {
                            suppressSelectionPersistence = true
                            try {
                                reloadListModel()
                                applySelection(settings.selectedCollaborators)
                                applyUiEnabledState()
                            } finally {
                                suppressSelectionPersistence = false
                            }
                        } else {
                            SwingUtilities.invokeLater {
                                suppressSelectionPersistence = true
                                try {
                                    reloadListModel()
                                    applySelection(settings.selectedCollaborators)
                                    applyUiEnabledState()
                                } finally {
                                    suppressSelectionPersistence = false
                                }
                            }
                        }
                }
            }
        )
    }

    /** Rebuilds the list's contents from settings, without touching the current selection. */
    private fun reloadListModel() {
        val model = DefaultListModel<String>()
        settings.collaborators.keys.forEach { model.addElement(it) }
        collaboratorsList.model = model
    }

    /** Selects exactly the rows whose collaborator name is in [names]; ignores unknown names. */
    private fun applySelection(names: Set<String>) {
        collaboratorsList.clearSelection()
        val model = collaboratorsList.model
        for (index in 0 until model.size) {
            if (model.getElementAt(index) in names) {
                collaboratorsList.addSelectionInterval(index, index)
            }
        }
    }

    /** The collaborator names currently checked in the list, in list (i.e. settings) order. */
    private fun selectedNames(): Set<String> = collaboratorsList.selectedValuesList.toCollection(LinkedHashSet())

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
        // Persist the current UI selection first — otherwise rebuilding the list model can
        // cause a race where settings' selectedCollaborators is stale and gets overwritten.
        saveState()
        suppressSelectionPersistence = true
        try {
            reloadListModel()
            applySelection(settings.selectedCollaborators)
            checkBox.isSelected = settings.giveKudosEnabled
            applyUiEnabledState()
        } finally {
            suppressSelectionPersistence = false
        }
    }

    override fun saveState() {
        settings.giveKudosEnabled = checkBox.isSelected
        settings.selectedCollaborators = selectedNames()
    }

    override fun restoreState() {
        checkBox.isSelected = settings.giveKudosEnabled
        applySelection(settings.selectedCollaborators)
        applyUiEnabledState()
    }
}
