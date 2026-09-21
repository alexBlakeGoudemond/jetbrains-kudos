package com.kudos.commit

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.EditorSettingsProvider
import com.intellij.ui.EditorTextField
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import javax.swing.JPanel

class KudosCommitPlaceholderTest : BasePlatformTestCase() {

    private fun getSettingsProviders(field: EditorTextField): List<*>? {
        var clazz: Class<*>? = field.javaClass
        while (clazz != null && clazz != Any::class.java) {
            try {
                val f: Field = clazz.getDeclaredField("mySettingsProviders")
                f.isAccessible = true
                return (f.get(field) as? Collection<*>)?.toList()
            } catch (_: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        return null
    }

    fun `test removeSettingsProviders removes plugin-registered providers`() {
        val editorField = EditorTextField("", project, PlainTextFileType.INSTANCE)
        val provider = EditorSettingsProvider { }

        editorField.addSettingsProvider(provider)
        val providersBefore = getSettingsProviders(editorField)
        assertNotNull(providersBefore)
        assertTrue(providersBefore!!.contains(provider))

        KudosCommitPlaceholder.removeSettingsProviders(editorField, provider)

        val providersAfter = getSettingsProviders(editorField)
        assertNotNull(providersAfter)
        assertFalse(providersAfter!!.contains(provider))
    }

    fun `test disposeAll cleans active placeholder and removes settings providers`() {
        val commitMessage = CommitMessage(project)
        val editorField = commitMessage.editorField
        
        val initialProviderCount = getSettingsProviders(editorField)?.size ?: 0

        // Install placeholder
        val panel = JPanel()
        panel.add(commitMessage)
        
        val fakeCheckinPanel = Proxy.newProxyInstance(
            CheckinProjectPanel::class.java.classLoader,
            arrayOf(CheckinProjectPanel::class.java),
            InvocationHandler { _, method, _ ->
                when (method.name) {
                    "getComponent", "getPreferredFocusedComponent" -> panel
                    "getCommitMessage" -> ""
                    "getProject" -> project
                    else -> null
                }
            }
        ) as CheckinProjectPanel

        KudosCommitPlaceholder.install(fakeCheckinPanel)
        // Flush EDT events
        com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val afterInstallCount = getSettingsProviders(editorField)?.size ?: 0
        assertTrue(afterInstallCount >= initialProviderCount)

        KudosCommitPlaceholder.disposeAll()
        com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val providersAfterDispose = getSettingsProviders(editorField) ?: emptyList<Any>()
        val ourClassLoader = KudosCommitPlaceholder::class.java.classLoader
        val remainingPluginProviders = providersAfterDispose.filter { it?.javaClass?.classLoader === ourClassLoader }
        assertTrue("All plugin settings providers must be removed", remainingPluginProviders.isEmpty())
    }
}
