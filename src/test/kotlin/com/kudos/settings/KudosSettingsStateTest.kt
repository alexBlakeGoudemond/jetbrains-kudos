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
        assertEquals(null, state.selectedCollaborator)
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

    fun `test selected collaborator falls back when removed`() {
        val state = freshState()
        state.setCollaborators(mapOf("Claude" to null))
        state.selectedCollaborator = "Claude"

        // Replacing the collaborator map without "Claude" in it should
        // move selection off a now-nonexistent entry, not leave it dangling.
        state.setCollaborators(mapOf("GitHub Copilot" to null))

        assertEquals("GitHub Copilot", state.selectedCollaborator)
    }

    fun `test state round-trips through persistence serialization`() {
        val state = freshState()
        state.setCollaborators(mapOf("Ada Lovelace" to "ada@example.com"))
        state.selectedCollaborator = "Ada Lovelace"
        state.giveKudosEnabled = false

        // Simulate what the platform does on IDE restart: serialize out, then load back in.
        val savedState = state.state
        state.resetToDefaults()
        state.loadState(savedState)

        assertEquals("Ada Lovelace <ada@example.com>", state.formatAttribution("Ada Lovelace"))
        assertEquals("Ada Lovelace", state.selectedCollaborator)
        assertFalse(state.giveKudosEnabled)
    }
}