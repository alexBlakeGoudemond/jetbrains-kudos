package com.kudos.commit

import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.kudos.settings.KudosSettingsState

class KudosCheckinHandler(private val panel: CheckinProjectPanel) : CheckinHandler() {

    private val settings = KudosSettingsState.getInstance()

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent =
        KudosCommitOptionsPanel(settings)

    override fun beforeCheckin(): ReturnResult {
        if (settings.giveKudosEnabled && settings.kudosUiEnabled) {
            settings.currentAttributionOrNull()?.let { appendAttribution(it) }
        }
        return ReturnResult.COMMIT
    }

    private fun appendAttribution(attribution: String) {
        val trailer = "Co-authored-by: $attribution"
        val message = panel.commitMessage

        // Guard against double-adding on amend / re-open-and-recommit.
        if (trailer in message) return

        val trimmed = message.trimEnd('\n')
        val newMessage = if (trimmed.isBlank()) trailer else "$trimmed\n\n$trailer"
        panel.setCommitMessage(newMessage)
    }
}