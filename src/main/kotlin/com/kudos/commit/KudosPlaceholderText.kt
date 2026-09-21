package com.kudos.commit

/**
 * Pure logic for the grey hint shown in the commit message box, e.g.
 * `Kudos Plugin will mention: Claude, ChatGPT`.
 *
 * Deliberately free of any IntelliJ Platform dependency so it can be unit tested without a
 * platform fixture (same approach as [KudosCommitMessage]).
 */
object KudosPlaceholderText {

    const val PREFIX = "Kudos Plugin will mention: "

    /**
     * Returns the hint text, or `null` when nothing should be shown:
     *
     * - "Give Kudos" is unticked, or the Kudos tool window master toggle is off, or
     * - nobody is selected (nothing would be credited, so there is nothing to warn about).
     *
     * These are the same conditions under which `KudosCheckinHandler.beforeCheckin` adds trailers,
     * so the hint is shown exactly when a trailer will be added.
     *
     * [names] are collaborator names only - never emails - and keep the order they were given in.
     */
    fun textFor(giveKudosEnabled: Boolean, kudosUiEnabled: Boolean, names: Collection<String>): String? {
        if (!giveKudosEnabled || !kudosUiEnabled) return null

        val shown = names.filter { it.isNotBlank() }
        if (shown.isEmpty()) return null

        return PREFIX + shown.joinToString(", ")
    }
}