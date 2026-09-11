package com.kudos.settings

import com.intellij.ui.JBColor
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import javax.swing.JComponent
import javax.swing.event.ListSelectionListener

/**
 * A single row in the collaborators table. `email` is "" rather than null here -
 * KudosSettingsState translates that at its own boundary, see setCollaborators().
 */
data class CollaboratorRow(var name: String, var email: String)

private class NameColumn : ColumnInfo<CollaboratorRow, String>("Name") {
    override fun valueOf(item: CollaboratorRow): String = item.name
    override fun isCellEditable(item: CollaboratorRow): Boolean = true
    override fun setValue(item: CollaboratorRow, value: String) {
        item.name = value
    }
}

private class EmailColumn : ColumnInfo<CollaboratorRow, String>("Email (optional)") {
    override fun valueOf(item: CollaboratorRow): String = item.email
    override fun isCellEditable(item: CollaboratorRow): Boolean = true
    override fun setValue(item: CollaboratorRow, value: String) {
        item.email = value
    }
}

class KudosSettingsPanel {

    private val settings = KudosSettingsState.getInstance()

    val tableModel = ListTableModel<CollaboratorRow>(NameColumn(), EmailColumn()).apply {
        items = rowsFromSettings()
    }

    val table = JBTable(tableModel).apply {
        setShowGrid(false)
        rowHeight = JBUI.scale(24)
    }

    val previewLabel = JBLabel().apply {
        foreground = JBColor.GRAY
    }

    val component: JComponent by lazy { buildComponent() }

    private fun rowsFromSettings(): MutableList<CollaboratorRow> =
        settings.collaborators.map { (name, email) -> CollaboratorRow(name, email ?: "") }.toMutableList()

    private fun buildComponent(): JComponent {
        val tablePanel = ToolbarDecorator.createDecorator(table)
            .setAddAction { addCollaborator() }
            .setRemoveAction { removeSelectedCollaborator() }
            .disableUpDownActions()
            .createPanel()

        // Any edit (add, remove, or in-cell rename/email change) persists immediately -
        // this is a live tool window, not an Apply/Cancel preferences dialog.
        tableModel.addTableModelListener {
            persistCollaborators()
            updatePreview()
        }
        table.selectionModel.addListSelectionListener(ListSelectionListener { updatePreview() })

        updatePreview()

        return panel {
            row {
                checkBox("Enable Kudos in Commit UI")
                    .applyToComponent {
                        isSelected = settings.kudosUiEnabled
                        addActionListener { settings.kudosUiEnabled = isSelected }
                    }
                    .comment(
                        "When disabled, the \u201cGive Kudos\u201d checkbox still appears in the commit dialog " +
                                "but is disabled, with a tooltip explaining Kudos is turned off."
                    )
            }

            group("Collaborators") {
                row {
                    cell(tablePanel)
                        .align(Align.FILL)
                        .resizableColumn()
                }.resizableRow()

                row("Preview:") {
                    cell(previewLabel)
                }
            }

            row {
                button("Reset to Defaults") {
                    settings.resetToDefaults()
                    tableModel.items = rowsFromSettings()
                    updatePreview()
                }
            }
        }.apply {
            border = JBUI.Borders.empty(8)
        }
    }

    fun addCollaborator() {
        tableModel.addRow(CollaboratorRow("New collaborator", ""))
        val newRowIndex = tableModel.rowCount - 1
        table.editCellAt(newRowIndex, 0)
        table.setRowSelectionInterval(newRowIndex, newRowIndex)

        // Persist immediately so consumers (commit UI) can update live
        persistCollaborators()
        updatePreview()
    }

    fun removeSelectedCollaborator() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            tableModel.removeRow(selectedRow)
        }
    }

    private fun persistCollaborators() {
        val map = tableModel.items.associate { it.name to it.email.ifBlank { null } }
        settings.setCollaborators(map)
    }

    fun updatePreview() {
        val row = tableModel.items.getOrNull(table.selectedRow) ?: tableModel.items.firstOrNull()
        previewLabel.text = row?.let { settings.formatAttribution(it.name) } ?: "No collaborators configured"
    }
}