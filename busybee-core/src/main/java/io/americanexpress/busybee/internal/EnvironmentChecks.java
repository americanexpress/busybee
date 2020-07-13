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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;

import static io.americanexpress.busybee.internal.Reflection.clazz;
import static io.americanexpress.busybee.internal.Reflection.invokeMethod;
import static io.americanexpress.busybee.internal.Reflection.invokeStaticMethod;
import static java.util.concurrent.TimeUnit.SECONDS;

public class EnvironmentChecks {

    @VisibleForTesting
    private static boolean pretendTestsAreNotRunning = false;

    public static boolean testsAreRunning() {
        if (pretendTestsAreNotRunning) return false;

        // may want to add TestNG or Junit5 support at some point.
        return junit4IsPresent() || androidJunitRunnerIsPresent();
    }

    @VisibleForTesting
    static boolean junit4IsPresent() {
        return Reflection.classIsFound("org.junit.runners.JUnit4");
    }

    @VisibleForTesting
    static boolean androidJunitRunnerIsPresent() {
        return Reflection.classIsFound("androidx.test.runner.AndroidJUnitRunner");
    }

    // can't reference Android types directly, so have to use raw types.
    @SuppressWarnings({"rawtypes"})
    static boolean hasWorkingAndroidMainLooper() {
        FutureTask<Boolean> runnable;
        try {
            Object mainLooper;
            Class looperClass = clazz("android.os.Looper");
            mainLooper = invokeStaticMethod(looperClass, "getMainLooper");
            Class handlerClass = clazz("android.os.Handler");
            Object handler = Reflection.invokeConstructor(handlerClass, looperClass, mainLooper);
            runnable = new FutureTask<>(() -> true);
            invokeMethod(handler, "postAtFrontOfQueue", new Class[]{Runnable.class}, new Object[]{runnable});
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ReflectiveOperationException) {
                // something that we needed doesn't exist, so Android Main Looper won't work
                return false;
            }
            throw e;
        }
        try {
            return runnable.get(5, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }
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
