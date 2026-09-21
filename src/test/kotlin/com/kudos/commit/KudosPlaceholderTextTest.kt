package com.kudos.commit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class KudosPlaceholderTextTest {

    @Test
    fun `lists every selected collaborator as comma separated names`() {
        val text = KudosPlaceholderText.textFor(true, true, listOf("Claude", "ChatGPT"))

        assertEquals("Kudos Plugin will mention: Claude, ChatGPT", text)
    }

    @Test
    fun `single collaborator has no trailing separator`() {
        val text = KudosPlaceholderText.textFor(true, true, listOf("Claude"))

        assertEquals("Kudos Plugin will mention: Claude", text)
    }

    @Test
    fun `keeps the order collaborators were given in`() {
        val text = KudosPlaceholderText.textFor(true, true, listOf("ChatGPT", "Ada Lovelace", "Claude"))

        assertEquals("Kudos Plugin will mention: ChatGPT, Ada Lovelace, Claude", text)
    }

    @Test
    fun `no hint when enabled but nobody is selected`() {
        assertNull(KudosPlaceholderText.textFor(true, true, emptyList()))
    }

    @Test
    fun `no hint when Give Kudos is unticked`() {
        assertNull(KudosPlaceholderText.textFor(false, true, listOf("Claude")))
    }

    @Test
    fun `no hint when the tool window master toggle is off`() {
        assertNull(KudosPlaceholderText.textFor(true, false, listOf("Claude")))
    }

    @Test
    fun `blank names are ignored`() {
        assertNull(KudosPlaceholderText.textFor(true, true, listOf("", "  ")))
        assertEquals("Kudos Plugin will mention: Claude", KudosPlaceholderText.textFor(true, true, listOf("", "Claude")))
    }
}