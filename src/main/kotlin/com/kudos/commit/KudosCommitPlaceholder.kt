package com.kudos.commit

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import com.kudos.infrastructure.kudosLogger
import com.kudos.settings.KudosSettingsListener
import com.kudos.settings.KudosSettingsState
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D
import java.util.concurrent.ConcurrentHashMap

/**
 * Shows a grey, non-editable hint in the commit message box - "Kudos Plugin will mention: Claude, ChatGPT" -
 * whenever Kudos is on *and* at least one collaborator is ticked, so it's obvious a trailer is about to be added
 * without opening the cog popup.
 *
 * Why not just `EditorTextField.setPlaceholder(...)`? Two reasons:
 *  1. It is only painted while the document is empty, so it vanishes the moment you start typing - which is
 *     exactly when you need the reminder.
 *  2. The Commit tool window already uses it for its own "Commit message" hint, and `CommitMessage.stopLoading()`
 *     resets it, so anything we put there gets overwritten.
 *
 * Instead we attach a *block inlay* to the message editor. Inlays are painted by the editor but are not part of
 * the document, so the hint never ends up in the commit message, can't be selected or deleted, and is never
 * removed by clicking into the box.
 *
 * One instance is bound to one commit message field for that field's lifetime (it is disposed with the
 * [CommitMessage] that owns the field).
 */
class KudosCommitPlaceholder private constructor(
    private val settings: KudosSettingsState,
    private val editorField: EditorTextField,
) : Disposable {

    private val document = editorField.document
    private var editor: EditorEx? = null
    private var inlay: Inlay<*>? = null
    private var shownText: String? = null
    private var reanchorQueued = false

    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) = queueReanchor()
    }

    init {
        // EditorTextField throws its editor away and builds a new one whenever it leaves/re-enters the UI
        // hierarchy (e.g. the tool window is hidden and shown again), and an inlay dies with its editor.
        // The settings provider is called for every editor the field creates from now on...
        editorField.addSettingsProvider { attach(it) }
        // ...but not retroactively, so also pick up an editor that already exists.
        (editorField.editor as? EditorEx)?.let { attach(it) }

        // Keeps the hint pinned to the end of the text as it changes.
        editorField.addDocumentListener(documentListener)

        // Live updates when someone ticks a box in the cog popup or flips the Kudos tool window toggle.
        ApplicationManager.getApplication().messageBus.connect(this).subscribe(
            KudosSettingsState.KUDOS_SETTINGS_TOPIC,
            object : KudosSettingsListener {
                override fun collaboratorsChanged() = refreshOnEdt()
                override fun selectionChanged() = refreshOnEdt()
            }
        )
    }

    private fun attach(newEditor: EditorEx) {
        if (Disposer.isDisposed(this) || newEditor === editor) return

        editor = newEditor
        inlay = null // the previous inlay (if any) went away together with the previous editor
        shownText = null
        refresh()
    }

    /** Brings the inlay in line with the current settings and document. Must run on the EDT. */
    private fun refresh() {
        val target = editor?.takeUnless { it.isDisposed } ?: return

        val text = KudosPlaceholderText.textFor(
            giveKudosEnabled = settings.giveKudosEnabled,
            kudosUiEnabled = settings.kudosUiEnabled,
            names = settings.currentNames(),
        )
        val end = target.document.textLength
        val existing = inlay?.takeIf { it.isValid }

        // Already showing the right thing in the right place.
        if (existing != null && text == shownText && existing.offset == end) return

        existing?.let { Disposer.dispose(it) }
        inlay = null
        shownText = text

        if (text != null) {
            // Block inlay, below the last line of the message. `end` is the document end, so it stays under
            // the text however many lines the message has.
            inlay = target.inlayModel.addBlockElement(end, true, false, 0, KudosHintRenderer(text))
        }
    }

    private fun refreshOnEdt() {
        val app = ApplicationManager.getApplication()
        if (app.isDispatchThread) {
            refresh()
        } else {
            app.invokeLater({ if (!Disposer.isDisposed(this)) refresh() }, ModalityState.any())
        }
    }

    /**
     * Typing at the very end usually carries the inlay along with the text, but Enter or a deletion can leave it
     * on a previous line. Re-anchoring is deferred (and coalesced) rather than done inside the document
     * callback, so we never touch the inlay model while the document is mid-change.
     */
    private fun queueReanchor() {
        if (reanchorQueued) return
        reanchorQueued = true
        ApplicationManager.getApplication().invokeLater({
            reanchorQueued = false
            if (!Disposer.isDisposed(this)) refresh()
        }, ModalityState.any())
    }

    override fun dispose() {
        ACTIVE.remove(this)
        editorField.removeDocumentListener(documentListener)
        // The document belongs to the IDE and outlives us, and it holds our INSTALLED_KEY marker (an instance of
        // our class). Leaving it there would pin this plugin's classloader after an unload/update.
        if (document.getUserData(INSTALLED_KEY) === this) document.putUserData(INSTALLED_KEY, null)
        inlay?.takeIf { it.isValid }?.let { Disposer.dispose(it) }
        inlay = null
        editor = null
    }

    companion object {
        private val LOG = kudosLogger<KudosCommitPlaceholder>()

        /** Marks a commit message field as already handled, so re-created handlers don't stack up hints. */
        private val INSTALLED_KEY = Key.create<KudosCommitPlaceholder>("Kudos.CommitPlaceholder")

        private val ACTIVE: MutableSet<KudosCommitPlaceholder> = ConcurrentHashMap.newKeySet()

        /**
         * Set once the plugin is unloading. An `install` queued just before that (a commit UI opened a moment
         * earlier) would otherwise run afterwards and attach a fresh hint that nothing would ever clean up.
         * It is a static of *this* classloader, so a reloaded plugin starts with it cleared again.
         */
        @Volatile
        private var unloading = false

        /**
         * Finds the commit message field belonging to [panel] and attaches the hint to it (once per field).
         *
         * Deferred to the EDT so it works regardless of which thread the handler is created on, and so the
         * commit UI has finished assembling itself by the time we go looking for the message field.
         * If anything about the platform's commit UI changes and the field can't be found, the hint simply
         * doesn't appear - committing is unaffected.
         */
        fun install(panel: CheckinProjectPanel) {
            ApplicationManager.getApplication().invokeLater({ installNow(panel) }, ModalityState.any())
        }

        /**
         * Takes every hint down. Called when the plugin is about to be unloaded.
         *
         * Note: `EditorTextField` has no way to remove a settings provider, so the (now inert) lambda we
         * registered stays referenced until the field itself is disposed.
         */
        fun disposeAll() {
            unloading = true
            ACTIVE.toList().forEach { Disposer.dispose(it) }
        }

        private fun installNow(panel: CheckinProjectPanel) {
            if (unloading) return
            val root = panel.component ?: return
            val commitMessage = UIUtil.findComponentOfType(root, CommitMessage::class.java)
            if (commitMessage == null) {
                LOG.info("No commit message field found under the commit panel; not showing the hint")
                return
            }
            if (Disposer.isDisposed(commitMessage)) return

            val field = commitMessage.editorField
            if (field.document.getUserData(INSTALLED_KEY) != null) return

            val placeholder = KudosCommitPlaceholder(KudosSettingsState.getInstance(), field)
            field.document.putUserData(INSTALLED_KEY, placeholder)
            ACTIVE.add(placeholder)
            Disposer.register(commitMessage, placeholder)
        }
    }
}

/** Paints [text] in the editor's own font, in grey, so it reads as "ghost" text rather than message content. */
private class KudosHintRenderer(private val text: String) : EditorCustomElementRenderer {

    private fun fontFor(editor: Editor) = editor.colorsScheme.getFont(EditorFontType.PLAIN)

    override fun calcWidthInPixels(inlay: Inlay<*>): Int =
        inlay.editor.contentComponent.getFontMetrics(fontFor(inlay.editor)).stringWidth(text)

    override fun paint(inlay: Inlay<*>, g: Graphics2D, targetRegion: Rectangle2D, textAttributes: TextAttributes) {
        val font = fontFor(inlay.editor)
        val metrics = g.getFontMetrics(font)
        val baseline = targetRegion.y + (targetRegion.height - metrics.height) / 2 + metrics.ascent

        g.font = font
        g.color = HINT_COLOR
        g.drawString(text, targetRegion.x.toFloat(), baseline.toFloat())
    }

    private companion object {
        /** Mid-grey reads as "placeholder" in both light and dark themes. */
        val HINT_COLOR: JBColor = JBColor.GRAY
    }
}