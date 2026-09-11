package com.kudos.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class KudosToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = KudosSettingsPanel()
        val content = ContentFactory.getInstance()
            .createContent(panel.component, /* displayName = */ "", /* isLockable = */ false)
        toolWindow.contentManager.addContent(content)
    }
    
}