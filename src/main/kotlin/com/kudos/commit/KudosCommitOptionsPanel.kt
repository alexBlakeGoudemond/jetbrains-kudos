package com.kudos.commit

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.ui.components.JBCheckBox
import com.kudos.settings.KudosSettingsState
import java.awt.FlowLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * The "Give Kudos [dropdown]" row shown in the commit dialog's options area.
 * Swing's JComboBox already scrolls its popup once items exceed a comfortable
 * height, so the doc's "dropdown should support scrolling" requirement is free.
 */
class KudosCommitOptionsPanel(private val settings: KudosSettingsState) : RefreshableOnComponent {

    val checkBox = JBCheckBox("Give Kudos")
    val comboBox = ComboBox<String>()

    private val rootPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
        add(checkBox)
        add(comboBox)
    }

    init {
        comboBox.model = DefaultComboBoxModel(settings.collaborators.keys.toTypedArray())

        checkBox.addActionListener {
            settings.giveKudosEnabled = checkBox.isSelected
            comboBox.isEnabled = checkBox.isSelected && settings.kudosUiEnabled
        }
        comboBox.addActionListener {
            settings.selectedCollaborator = comboBox.selectedItem as? String
        }

        restoreState()
    }

    private fun applyUiEnabledState() {
        val uiEnabled = settings.kudosUiEnabled
        checkBox.isEnabled = uiEnabled
        comboBox.isEnabled = uiEnabled && checkBox.isSelected

        val tooltip = if (uiEnabled) null else "Kudos is currently disabled. Enable it from the Kudos tool window."
        checkBox.toolTipText = tooltip
        comboBox.toolTipText = tooltip
    }

    override fun getComponent(): JComponent = rootPanel

    /** Called when the commit dialog reopens - picks up edits made in the Kudos tool window meanwhile. */
    override fun refresh() {
        comboBox.model = DefaultComboBoxModel(settings.collaborators.keys.toTypedArray())
        comboBox.selectedItem = settings.selectedCollaborator
        checkBox.isSelected = settings.giveKudosEnabled
        applyUiEnabledState()
    }

    override fun saveState() {
        settings.giveKudosEnabled = checkBox.isSelected
        settings.selectedCollaborator = comboBox.selectedItem as? String
    }

    override fun restoreState() {
        checkBox.isSelected = settings.giveKudosEnabled
        comboBox.selectedItem = settings.selectedCollaborator
        applyUiEnabledState()
    }
}