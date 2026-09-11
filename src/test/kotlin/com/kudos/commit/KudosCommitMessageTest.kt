package com.kudos.commit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KudosCommitMessageTest {

    @Test
    fun `trailer includes attribution string`() {
        val trailer = KudosCommitMessage.trailerFor("Claude <noreply@anthropic.com>")
        assertEquals("Co-authored-by: Claude <noreply@anthropic.com>", trailer)
    }

    @Test
    fun `appends trailer to a blank message`() {
        val result = KudosCommitMessage.withTrailer("", "Claude")
        assertEquals("Co-authored-by: Claude", result)
    }

    @Test
    fun `appends trailer after existing message with a blank line separator`() {
        val result = KudosCommitMessage.withTrailer("Fix the bug", "Claude")
        assertEquals("Fix the bug\n\nCo-authored-by: Claude", result)
    }

    @Test
    fun `trims trailing newlines before appending`() {
        val result = KudosCommitMessage.withTrailer("Fix the bug\n\n\n", "Claude")
        assertEquals("Fix the bug\n\nCo-authored-by: Claude", result)
    }

    @Test
    fun `does not duplicate an already-present trailer`() {
        val message = "Fix the bug\n\nCo-authored-by: Claude"
        val result = KudosCommitMessage.withTrailer(message, "Claude")
        assertEquals(message, result)
    }

    @Test
    fun `formats attribution with email when the collaborator has one`() {
        val result = KudosCommitMessage.withTrailer("Fix the bug", "Ada Lovelace <ada@example.com>")
        assertEquals("Fix the bug\n\nCo-authored-by: Ada Lovelace <ada@example.com>", result)
    }
}