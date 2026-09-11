package com.kudos.commit

/**
 * Pure logic for building and appending the Kudos attribution trailer to a commit message.
 * Deliberately kept free of any IntelliJ Platform / CheckinProjectPanel dependency so it's
 * trivial to unit test without a platform test fixture.
 */
object KudosCommitMessage {

    fun trailerFor(attribution: String): String = "Co-authored-by: $attribution"

    /**
     * Returns [message] with the attribution trailer appended, unless it's already present
     * (e.g. reopening the commit dialog on the same changelist, or amending a commit that
     * already has it).
     */
    fun withTrailer(message: String, attribution: String): String {
        val trailer = trailerFor(attribution)
        if (trailer in message) return message

        val trimmed = message.trimEnd('\n')
        return if (trimmed.isBlank()) trailer else "$trimmed\n\n$trailer"
    }
}