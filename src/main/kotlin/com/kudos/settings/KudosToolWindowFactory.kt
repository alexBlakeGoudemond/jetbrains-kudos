package com.kudos.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class KudosToolWindowFactory : ToolWindowFactory, Disposable {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = KudosSettingsPanel()
        val content = ContentFactory.getInstance()
            .createContent(panel.component, /* displayName = */ "", /* isLockable = */ false)
        content.setDisposer(this)
        toolWindow.contentManager.addContent(content)
    }

    override fun dispose() {
    }
}