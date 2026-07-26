package com.example.repeatersmap

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNavigationBetweenMapAndAboutScreenPreservesState() {
        composeTestRule.setContent {
            MainStructure()
        }

        // start on repeaters map
        composeTestRule.onNodeWithText("Radio Repeaters Map").assertIsDisplayed()

        // open drawer and switch to about app
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("About App").performClick()

        // check info screen
        composeTestRule.onNodeWithText("About Screen").assertIsDisplayed()

        // open drawer and return to repeaters map
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Repeaters Map").performClick()

        // return to map without crash
        composeTestRule.onNodeWithText("Radio Repeaters Map").assertIsDisplayed()
    }
}
