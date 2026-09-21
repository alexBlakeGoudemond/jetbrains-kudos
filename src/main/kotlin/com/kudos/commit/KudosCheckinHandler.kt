package com.kudos.commit

import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.kudos.settings.KudosSettingsState

class KudosCheckinHandler(private val panel: CheckinProjectPanel) : CheckinHandler() {

    private val settings = KudosSettingsState.getInstance()

    init {
        // Draws the grey "Kudos Plugin will mention: ..." hint in the commit message box.
        // Handlers are re-created (e.g. after each commit); installing is idempotent per message box.
        KudosCommitPlaceholder.install(panel)
    }

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent =
        KudosCommitOptionsPanel(settings)

    override fun beforeCheckin(): ReturnResult {
        if (settings.giveKudosEnabled && settings.kudosUiEnabled) {
            val attributions = settings.currentAttributions()
            if (attributions.isNotEmpty()) {
                appendAttributions(attributions)
            }
        }
        return ReturnResult.COMMIT
    }

    private fun appendAttributions(attributions: List<String>) {
        panel.setCommitMessage(KudosCommitMessage.withTrailers(panel.commitMessage, attributions))
    }
}