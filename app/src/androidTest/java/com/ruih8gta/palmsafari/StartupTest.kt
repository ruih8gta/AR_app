package com.ruih8gta.palmsafari

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsSelected
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun previewStartsWithoutRequestingCameraAndCanSwitchModels() {
        compose.onNodeWithText(compose.activity.getString(R.string.app_name)).assertIsDisplayed()
        compose.onNodeWithText("ARをはじめる").assertIsDisplayed()
        compose.onNodeWithText("キツネ").performClick().assertIsSelected()
        compose.onNodeWithText("停止").performClick().assertIsSelected()
    }
}
