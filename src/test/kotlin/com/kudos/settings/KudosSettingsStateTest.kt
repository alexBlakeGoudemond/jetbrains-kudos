package com.kudos.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KudosSettingsStateTest : BasePlatformTestCase() {

    private fun freshState(): KudosSettingsState {
        val state = KudosSettingsState.getInstance()
        state.resetToDefaults()
        return state
    }

    fun `test defaults are seeded`() {
        val state = freshState()

        assertTrue(state.giveKudosEnabled)
        assertTrue(state.kudosUiEnabled)
        assertTrue(state.selectedCollaborators.isEmpty())
        assertTrue(state.collaborators.containsKey("Claude"))
        assertTrue(state.collaborators.containsKey("GitHub Copilot"))
        assertTrue(state.collaborators.containsKey("ChatGPT"))
    }

    fun `test attribution formats with email`() {
        val state = freshState()
        state.setCollaborators(mapOf("Ada Lovelace" to "ada@example.com"))

        assertEquals("Ada Lovelace <ada@example.com>", state.formatAttribution("Ada Lovelace"))
    }

    fun `test attribution formats without email`() {
        val state = freshState()
        state.setCollaborators(mapOf("Claude" to null))

        assertEquals("Claude", state.formatAttribution("Claude"))
    }

    fun `test selected collaborators are pruned when their entry is removed`() {
        val state = freshState()
        state.setCollaborators(mapOf("Claude" to null, "Colleague" to null))
        state.selectedCollaborators = setOf("Claude", "Colleague")

        // Replacing the collaborator map without "Claude" in it should drop it from the
        // selection rather than leave a dangling reference - but "Colleague" is untouched.
        state.setCollaborators(mapOf("Colleague" to null, "GitHub Copilot" to null))

        assertEquals(setOf("Colleague"), state.selectedCollaborators)
    }

    fun `test selecting multiple collaborators preserves order for attribution`() {
        val state = freshState()
        state.setCollaborators(mapOf("Colleague" to null, "AI Agent" to null))
        state.selectedCollaborators = setOf("Colleague", "AI Agent")

        assertEquals(listOf("Colleague", "AI Agent"), state.currentAttributions())
    }

    fun `test currentAttributions is empty when nothing is selected`() {
        val state = freshState()

        assertTrue(state.currentAttributions().isEmpty())
    }

    fun `test currentAttributions formats each selected collaborator with its own email`() {
        val state = freshState()
        state.setCollaborators(mapOf("Ada Lovelace" to "ada@example.com", "Claude" to null))
        state.selectedCollaborators = setOf("Ada Lovelace", "Claude")

        assertEquals(listOf("Ada Lovelace <ada@example.com>", "Claude"), state.currentAttributions())
    }

    fun `test state round-trips through persistence serialization`() {
        val state = freshState()
        state.setCollaborators(mapOf("Ada Lovelace" to "ada@example.com", "Claude" to null))
        state.selectedCollaborators = setOf("Ada Lovelace", "Claude")
        state.giveKudosEnabled = false

        // Simulate what the platform does on IDE restart: serialize out, then load back in.
        val savedState = state.state
        state.resetToDefaults()
        state.loadState(savedState)

        assertEquals("Ada Lovelace <ada@example.com>", state.formatAttribution("Ada Lovelace"))
        assertEquals(setOf("Ada Lovelace", "Claude"), state.selectedCollaborators)
        assertFalse(state.giveKudosEnabled)
    }
}