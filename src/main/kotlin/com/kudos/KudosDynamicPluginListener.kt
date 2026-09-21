package com.kudos

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.IconLoader
import com.kudos.commit.KudosCommitPlaceholder
import com.kudos.infrastructure.kudosLogger

class KudosDynamicPluginListener : DynamicPluginListener {

    companion object {
        private val LOG = kudosLogger<KudosDynamicPluginListener>()
        private val PLUGIN_ID: PluginId? = PluginManager.getPluginByClass(KudosDynamicPluginListener::class.java)?.pluginId
    }

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        LOG.info("beforePluginUnload fired for ${pluginDescriptor.pluginId.idString}")
        // This listener fires for every plugin's unload, not just ours — filter it.
        val targetPluginId = PLUGIN_ID ?: PluginId.getId("Kudos")
        if (pluginDescriptor.pluginId != targetPluginId) {
            return
        }

        // Our hint lives inside the IDE's own commit message editors, so it has to be taken down
        // explicitly or those editors would keep our classes alive after the plugin is unloaded.
        KudosCommitPlaceholder.disposeAll()

        LOG.info("Clearing IconLoader cache ahead of unload (isUpdate=$isUpdate)")
        IconLoader.clearCache()
    }
}