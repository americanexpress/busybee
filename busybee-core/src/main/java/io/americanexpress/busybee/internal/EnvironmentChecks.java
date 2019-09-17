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

package io.americanexpress.busybee.internal;

import androidx.annotation.VisibleForTesting;

public class EnvironmentChecks {

    @VisibleForTesting
    private static boolean pretendTestsAreNotRunning = false;

    public static boolean testsAreRunning() {
        if (pretendTestsAreNotRunning) return false;

        // may want to add TestNG or Junit5 support at some point.
        return junit4IsPresent() || androidJunitRunnerIsPresent();
    }

    private static boolean junit4IsPresent() {
        return Reflection.classIsFound("org.junit.runners.JUnit4");
    }

    @VisibleForTesting
    static boolean androidJunitRunnerIsPresent() {
        return Reflection.classIsFound("androidx.test.runner.AndroidJUnitRunner");
    }

    static boolean isAndroid() {
        return Reflection.classIsFound("android.app.Application");
    }

    @VisibleForTesting
    static void pretendTestsAreNotRunning() {
        pretendTestsAreNotRunning = true;
    }

    @VisibleForTesting
    static void doNotPretendTestsAreNotRunning() {
        pretendTestsAreNotRunning = false;
    }
}
