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

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import io.americanexpress.busybee.BusyBee.Category.NETWORK
import io.americanexpress.busybee.sample.BusyBeeActivity
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class BusyBeeCategoryTest {

    private val activityRule = ActivityTestRule(BusyBeeActivity::class.java)
    private val busyBee = BusyBee.singleton()

    companion object {
        const val idleTimeoutInMillis = 4000L
        const val SOME_BACKGROUND_OPERATION = "Some background Operation"
        const val NETWORK_BACKGROUND_OPERATION = "Network background Operation"

        private val MAIN_THREAD = Handler(Looper.getMainLooper())
    }

    @Before
    fun setUp() {
        val activity = activityRule.launchActivity(null)

        activity.onButtonClick(View.OnClickListener { view: View ->
            when (view) {
                is TextView -> {
                    busyBee.busyWith(SOME_BACKGROUND_OPERATION)
                    busyBee.busyWith(NETWORK_BACKGROUND_OPERATION, NETWORK)
                    Thread(Runnable {
                        try {
                            Thread.sleep(idleTimeoutInMillis / 2)
                            MAIN_THREAD.post { view.text = "All done." }
                        } catch (e: InterruptedException) {
                            throw RuntimeException(e)
                        } finally {
                            busyBee.completed(SOME_BACKGROUND_OPERATION)
                            // Purposefully, don't complete the Network operation
                            // busyBee.completed(NETWORK_BACKGROUND_OPERATION)
                        }
                    }).start()
                }
                else -> throw IllegalArgumentException()
            }
        })

    }

    @Test
    fun whenPayAttentionToNetwork_thenExceptionWhenClick() {
        busyBee.payAttentionToCategory(NETWORK)
        try {
            onView(withText(R.string.press_me)).perform(click())
            fail("should throw a perform exception because the network operation never completes")
        } catch (e: PerformException) {
            // expected
        }
    }

    @Test
    fun whenIgnoreNetwork_thenCanStillClickButton() {
        busyBee.ignoreCategory(NETWORK)

        onView(withText(R.string.press_me)).perform(click())
        onView(withText("All done.")).check(matches(isDisplayed()))
    }
}