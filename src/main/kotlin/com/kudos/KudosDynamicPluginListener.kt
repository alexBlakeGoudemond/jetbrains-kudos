package com.kudos

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.util.IconLoader
import com.kudos.infrastructure.kudosLogger

class KudosDynamicPluginListener : DynamicPluginListener {

    companion object {
        private val LOG = kudosLogger<KudosDynamicPluginListener>()
    }

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        // This listener fires for every plugin's unload, not just ours — filter it.
        if (pluginDescriptor.pluginId.idString != "Kudos") {
            return
        }

        LOG.info("Clearing IconLoader cache ahead of unload (isUpdate=$isUpdate)")
        IconLoader.clearCache()
    }
}