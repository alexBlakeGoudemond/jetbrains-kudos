package com.kudos.commit

import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import javax.swing.JPanel

class KudosCheckinHandlerTest : BasePlatformTestCase() {

    private class FakeCommitWorkflow(val _commitHandlers: MutableList<CheckinHandler> = mutableListOf())

    private class FakePanelTarget(
        val workflow: FakeCommitWorkflow = FakeCommitWorkflow()
    ) {
        val component = JPanel()
    }

    fun `test disposeAll removes KudosCheckinHandler from workflow commit handlers`() {
        val fakeTarget = FakePanelTarget()
        val panelProxy = Proxy.newProxyInstance(
            CheckinProjectPanel::class.java.classLoader,
            arrayOf(CheckinProjectPanel::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any? {
                    return when (method.name) {
                        "getComponent", "getPreferredFocusedComponent" -> fakeTarget.component
                        "getCommitMessage" -> ""
                        "getProject" -> project
                        "equals" -> proxy === args?.getOrNull(0)
                        "hashCode" -> System.identityHashCode(proxy)
                        "toString" -> "CheckinProjectPanelProxy"
                        else -> {
                            // If looking up workflow from the target
                            try {
                                val targetMethod = fakeTarget.javaClass.getMethod(method.name, *method.parameterTypes)
                                targetMethod.invoke(fakeTarget, *(args ?: emptyArray()))
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                }
            }
        ) as CheckinProjectPanel

        // Let's also attach the workflow to the handler's panel via reflection so findWorkflow finds it
        val handler = KudosCheckinHandler(panelProxy)
        
        // We can attach the handler to fakeTarget.workflow._commitHandlers
        fakeTarget.workflow._commitHandlers.add(handler)

        // Set the field on proxy's handler or directly verify findWorkflow & removeHandlerFromObject
        // Since panelProxy is dynamic proxy, handler.panel holds panelProxy.
        // We can test removeHandlerFromObject on fakeTarget.workflow directly or through a wrapper
        class WrapperPanel(val workflow: FakeCommitWorkflow, val panel: CheckinProjectPanel) : CheckinProjectPanel by panel

        val wrapper = WrapperPanel(fakeTarget.workflow, panelProxy)
        val wrappedHandler = KudosCheckinHandler(wrapper)
        fakeTarget.workflow._commitHandlers.add(wrappedHandler)

        assertTrue(fakeTarget.workflow._commitHandlers.contains(wrappedHandler))

        KudosCheckinHandler.disposeAll()

        assertFalse(
            "KudosCheckinHandler should be detached from workflow commit handlers upon disposeAll",
            fakeTarget.workflow._commitHandlers.contains(wrappedHandler)
        )
    }
}
