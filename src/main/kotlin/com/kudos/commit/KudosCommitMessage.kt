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
    fun withTrailer(message: String, attribution: String): String = withTrailers(message, listOf(attribution))

    /**
     * Returns [message] with one trailer line appended per entry in [attributions], so that
     * pairing with multiple collaborators (e.g. a colleague *and* an AI agent) produces:
     *
     * ```
     * Co-authored-by: Ada Lovelace <ada@example.com>
     * Co-authored-by: Claude
     * ```
     *
     * Trailers are joined with a single newline (no blank lines between them) to read as one
     * conventional trailer block, matching how git/GitHub expect multiple `Co-authored-by`
     * lines to be grouped. The block as a whole is still separated from the message body by
     * a blank line, same as [withTrailer] always did.
     *
     * Any attribution whose trailer is already present in [message] is skipped, so calling
     * this repeatedly (e.g. each time the commit dialog reopens) never duplicates a trailer.
     * Order of [attributions] is preserved for the newly-added trailers.
     */
    fun withTrailers(message: String, attributions: List<String>): String {
        val newTrailers = attributions
            .map { trailerFor(it) }
            .distinct()
            .filterNot { it in message }

        if (newTrailers.isEmpty()) return message

        val trimmed = message.trimEnd('\n')
        val trailerBlock = newTrailers.joinToString("\n")
        return if (trimmed.isBlank()) trailerBlock else "$trimmed\n\n$trailerBlock"
    }
}