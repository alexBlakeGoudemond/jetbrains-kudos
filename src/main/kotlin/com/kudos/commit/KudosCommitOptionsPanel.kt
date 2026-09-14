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
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.JLayer
import javax.swing.plaf.LayerUI

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
        // Indent collaborator rows so they appear nested under the "Give Kudos" checkbox.
        // Uses a left inset (18px) while keeping top/bottom/right at 0.
        border = JBUI.Borders.empty(0, 18, 0, 0)
    }

    private val scrollPane = JBScrollPane(collaboratorsList).apply {
        preferredSize = Dimension(220, 90)
        // Remove scroll pane chrome so the outer accent border reads as a single container.
        border = JBUI.Borders.empty()
        viewportBorder = JBUI.Borders.empty()
    }

    private val contentPanel = JPanel(BorderLayout(0, 4)).apply {
        add(checkBox, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
    }

    /*
     * The actual content container.
     *
     * We deliberately keep the original BorderLayout and sizing behaviour.
     * There is NO custom layout here.
     */
    private val borderedPanel = JPanel(BorderLayout()).apply {
        // Keep the original spacing around the content.
        border = JBUI.Borders.empty(0, -3, 0, 8)

        add(contentPanel, BorderLayout.CENTER)
    }

    /*
     * JLayer paints over its view after the view has painted.
     *
     * This is the important bit:
     *
     *     borderedPanel
     *          ↓
     *     contentPanel
     *          ↓
     *     Give Kudos checkbox
     *          ↓
     *     JLayer paints accent border LAST
     *
     * Therefore the checkbox cannot cut through the border.
     */
    private val rootPanel = JLayer(
        borderedPanel,
        object : LayerUI<JPanel>() {

            override fun paint(
                g: Graphics,
                c: JComponent
            ) {
                // First paint the entire normal component hierarchy.
                super.paint(g, c)

                // Then paint our border ON TOP of everything.
                val graphics = g.create() as Graphics2D

                try {
                    graphics.color = KUDOS_ACCENT_COLOR
                    graphics.stroke = BasicStroke(JBUI.scale(1).toFloat())

                    graphics.drawRect(
                        0,
                        0,
                        c.width - 1,
                        c.height - 1
                    )
                } finally {
                    graphics.dispose()
                }
            }
        }
    )

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
        ApplicationManager.getApplication().messageBus.connect(settings).subscribe(
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
            collaboratorsList.addItem(
                name,
                name,
                name in selected
            )
        }
    }

    private fun checkedNames(): Set<String> =
        collaboratorsList.checkedItems.toCollection(LinkedHashSet())

    private fun applyUiEnabledState() {
        val uiEnabled = settings.kudosUiEnabled

        checkBox.isEnabled = uiEnabled
        collaboratorsList.isEnabled = uiEnabled && checkBox.isSelected

        val tooltip =
            if (uiEnabled) {
                null
            } else {
                "Kudos is currently disabled. Enable it from the Kudos tool window."
            }

        checkBox.toolTipText = tooltip
        collaboratorsList.toolTipText = tooltip
    }

    override fun getComponent(): JComponent = rootPanel

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
        private val KUDOS_ACCENT_COLOR = JBColor(
            0x8759B3,
            0xB39DDB
        )
    }
}