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

    // --- withTrailers (multiple collaborators) --------------------------------------------

    @Test
    fun `appends one trailer per attribution as a single block`() {
        val result = KudosCommitMessage.withTrailers("Fix the bug", listOf("AI Agent", "Colleague"))
        assertEquals("Fix the bug\n\nCo-authored-by: AI Agent\nCo-authored-by: Colleague", result)
    }

    @Test
    fun `does not put a blank line between multiple trailers`() {
        val result = KudosCommitMessage.withTrailers("Fix the bug", listOf("AI Agent", "Colleague"))
        // Exactly one blank line (body -> trailer block), none within the block itself.
        assertEquals(1, result.split("\n\n").size - 1)
    }

    @Test
    fun `preserves attribution order in the trailer block`() {
        val result = KudosCommitMessage.withTrailers("", listOf("Colleague", "AI Agent"))
        assertEquals("Co-authored-by: Colleague\nCo-authored-by: AI Agent", result)
    }

    @Test
    fun `empty attribution list returns the message unchanged`() {
        val result = KudosCommitMessage.withTrailers("Fix the bug", emptyList())
        assertEquals("Fix the bug", result)
    }

    @Test
    fun `skips attributions already present and only appends the missing ones`() {
        val message = "Fix the bug\n\nCo-authored-by: AI Agent"
        val result = KudosCommitMessage.withTrailers(message, listOf("AI Agent", "Colleague"))
        assertEquals("Fix the bug\n\nCo-authored-by: AI Agent\n\nCo-authored-by: Colleague", result)
    }

    @Test
    fun `does not duplicate a trailer when every attribution is already present`() {
        val message = "Fix the bug\n\nCo-authored-by: AI Agent\nCo-authored-by: Colleague"
        val result = KudosCommitMessage.withTrailers(message, listOf("AI Agent", "Colleague"))
        assertEquals(message, result)
    }

    @Test
    fun `deduplicates a repeated attribution within the same call`() {
        val result = KudosCommitMessage.withTrailers("Fix the bug", listOf("AI Agent", "AI Agent"))
        assertEquals("Fix the bug\n\nCo-authored-by: AI Agent", result)
    }

    @Test
    fun `withTrailer for a single attribution matches withTrailers with a one element list`() {
        val single = KudosCommitMessage.withTrailer("Fix the bug", "Claude")
        val multi = KudosCommitMessage.withTrailers("Fix the bug", listOf("Claude"))
        assertEquals(single, multi)
    }
}