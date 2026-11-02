package com.aio.tracker

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Launches the real app and visits every screen to catch crashes during composition. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String, substring: Boolean = false) {
        rule.waitUntil(15_000) {
            rule.onAllNodes(hasText(text, substring = substring), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun clickTab(label: String) {
        rule.onAllNodes(hasText(label) and hasClickAction()).onFirst().performClick()
        rule.waitForIdle()
    }

    @Test
    fun visitAllScreens() {
        waitForText("Assalamu alaikum", substring = true)
        waitForText("NEXT PRAYER")

        clickTab("Salat")
        waitForText("Today's prayers")

        clickTab("Fitness")
        waitForText("This week")

        clickTab("Money")
        waitForText("NET BALANCE")
        clickTab("Activity")
        clickTab("Lending")
        waitForText("Owed to you")

        clickTab("Planner")
        waitForText("To-Do")
        clickTab("Notes")
        waitForText("Search notes")

        clickTab("Home")
        waitForText("AI life summary")
        rule.onAllNodes(hasText("AI life summary")).onFirst().performClick()
        waitForText("AI Insights")
    }
}
