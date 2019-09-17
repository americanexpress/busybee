/*
 * Copyright 2019 American Express Travel Related Services Company, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.americanexpress.busybee

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import io.americanexpress.busybee.sample.BusyBeeActivity
import org.junit.Before
import org.junit.Test

class BusyBeeActivityTest {
    private val activityRule = ActivityTestRule(BusyBeeActivity::class.java)

    @Before
    fun setUp() {
        activityRule.launchActivity(null)
    }

    @Test
    fun whenClickButton_thenEspressoWaitsForResultToBeDisplayed() {
        onView(withText(R.string.press_me)).perform(click())
        onView(withId(R.id.confirmation)).check(matches(withText("Button Pressed")))
    }

}