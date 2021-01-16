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
package io.americanexpress.busybee.internal

import androidx.annotation.VisibleForTesting
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object EnvironmentChecks {
    @VisibleForTesting
    private var pretendTestsAreNotRunning = false
    fun testsAreRunning(): Boolean {
        return if (pretendTestsAreNotRunning) false else junit4IsPresent() || androidJunitRunnerIsPresent()

        // may want to add TestNG or Junit5 support at some point.
    }

    @VisibleForTesting
    fun junit4IsPresent(): Boolean {
        return Reflection.classIsFound("org.junit.runners.JUnit4")
    }

    @VisibleForTesting
    fun androidJunitRunnerIsPresent(): Boolean {
        return Reflection.classIsFound("androidx.test.runner.AndroidJUnitRunner")
    }

    // can't reference Android types directly, so have to use raw types.
    fun hasWorkingAndroidMainLooper(): Boolean {
        val runnable: FutureTask<Boolean>
        try {
            val mainLooper: Any?
            val looperClass: Class<*> = clazz("android.os.Looper")
            mainLooper = Reflection.invokeStaticMethod(looperClass, "getMainLooper")
            val handlerClass: Class<*> = clazz("android.os.Handler")
            val handler = Reflection.invokeConstructor(handlerClass, looperClass, mainLooper)
            runnable = FutureTask { true }
            Reflection.invokeMethod(
                handler, "postAtFrontOfQueue", arrayOf<Class<*>?>(
                    Runnable::class.java
                ), arrayOf<Any>(runnable)
            )
        } catch (e: RuntimeException) {
            if (e.cause is ReflectiveOperationException) {
                // something that we needed doesn't exist, so Android Main Looper won't work
                return false
            }
            throw e
        }
        return try {
            runnable[5, TimeUnit.SECONDS]
        } catch (e: InterruptedException) {
            false
        } catch (e: ExecutionException) {
            false
        } catch (e: TimeoutException) {
            false
        }
    }

    @VisibleForTesting
    fun pretendTestsAreNotRunning() {
        pretendTestsAreNotRunning = true
    }

    @VisibleForTesting
    fun doNotPretendTestsAreNotRunning() {
        pretendTestsAreNotRunning = false
    }
}