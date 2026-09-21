package com.kudos.commit

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.kudos.infrastructure.kudosLogger
import com.kudos.settings.KudosSettingsState
import java.util.concurrent.ConcurrentHashMap

class KudosCheckinHandler(private val panel: CheckinProjectPanel) : CheckinHandler() {

    private val settings = KudosSettingsState.getInstance()

    init {
        ACTIVE_HANDLERS.add(this)
        // Draws the grey "Kudos Plugin will mention: ..." hint in the commit message box.
        // Handlers are re-created (e.g. after each commit); installing is idempotent per message box.
        KudosCommitPlaceholder.install(panel)
    }

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent {
        val optionsPanel = KudosCommitOptionsPanel(settings)
        (panel as? Disposable)?.let { Disposer.tryRegister(it, optionsPanel) }
        return optionsPanel
    }

    override fun beforeCheckin(): ReturnResult {
        if (settings.giveKudosEnabled && settings.kudosUiEnabled) {
            val attributions = settings.currentAttributions()
            if (attributions.isNotEmpty()) {
                appendAttributions(attributions)
            }
        }
        return ReturnResult.COMMIT
    }

    private fun appendAttributions(attributions: List<String>) {
        panel.setCommitMessage(KudosCommitMessage.withTrailers(panel.commitMessage, attributions))
    }

    companion object {
        private val LOG = kudosLogger<KudosCheckinHandler>()
        private val ACTIVE_HANDLERS = ConcurrentHashMap.newKeySet<KudosCheckinHandler>()

        /**
         * Cleans up active handlers from long-lived commit workflows when the plugin is unloading.
         * The non-modal Commit tool window keeps workflow instances alive across project sessions;
         * leaving our handler registered in `_commitHandlers` pins this plugin's classloader.
         */
        fun disposeAll() {
            LOG.info("KudosCheckinHandler.disposeAll() started")
            for (handler in ACTIVE_HANDLERS) {
                try {
                    detachFromWorkflow(handler)
                } catch (e: Throwable) {
                    LOG.warn("Failed to detach KudosCheckinHandler during unload", e)
                }
            }
            ACTIVE_HANDLERS.clear()
            LOG.info("KudosCheckinHandler.disposeAll() completed")
        }

        private fun detachFromWorkflow(handler: KudosCheckinHandler) {
            val panel = handler.panel
            removeHandlerFromObject(panel, handler)
            val workflow = findWorkflow(panel) ?: return
            removeHandlerFromObject(workflow, handler)
        }

        private fun findWorkflow(panel: CheckinProjectPanel): Any? {
            var clazz: Class<*>? = panel.javaClass
            while (clazz != null && clazz != Any::class.java) {
                for (name in listOf("workflow", "myWorkflow", "commitWorkflow")) {
                    try {
                        val field = clazz.getDeclaredField(name)
                        field.isAccessible = true
                        val value = field.get(panel)
                        if (value != null) return value
                    } catch (_: NoSuchFieldException) {
                    } catch (_: Exception) {
                    }
                }
                for (methodName in listOf("getWorkflow", "getCommitWorkflow")) {
                    try {
                        val method = clazz.getDeclaredMethod(methodName)
                        method.isAccessible = true
                        val value = method.invoke(panel)
                        if (value != null) return value
                    } catch (_: NoSuchMethodException) {
                    } catch (_: Exception) {
                    }
                }
                clazz = clazz.superclass
            }
            return null
        }

        private fun removeHandlerFromObject(target: Any, handler: KudosCheckinHandler) {
            var clazz: Class<*>? = target.javaClass
            while (clazz != null && clazz != Any::class.java) {
                for (field in clazz.declaredFields) {
                    if (field.name.contains("commitHandlers", ignoreCase = true) ||
                        field.name.contains("handlers", ignoreCase = true)
                    ) {
                        try {
                            field.isAccessible = true
                            val list = field.get(target) as? MutableCollection<*>
                            if (list != null && list.remove(handler)) {
                                LOG.info("Successfully detached KudosCheckinHandler from ${clazz.simpleName}.${field.name}")
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
                clazz = clazz.superclass
            }
        }
    }
}