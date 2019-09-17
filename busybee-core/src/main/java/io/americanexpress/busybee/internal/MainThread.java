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

import java.lang.reflect.Field;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static io.americanexpress.busybee.internal.EnvironmentChecks.isAndroid;

public class MainThread {
    // Double check locking https://errorprone.info/bugpattern/DoubleCheckedLocking
    private static volatile Executor INSTANCE;

    private static Executor jvmExecutor() {
        Executor instance = INSTANCE;
        if (instance == null) {
            synchronized (MainThread.class) {
                instance = INSTANCE;
                if (instance == null) {
                    INSTANCE = Executors.newSingleThreadExecutor();
                }
            }
        }
        return INSTANCE;
    }

    static Executor singletonExecutor() {
        /*
         * Can only load AndroidMainThreadExecutor if we are on Android (not JVM)
         * else will we get class not found errors.
         */
        if (isAndroid()) {
            Class<?> androidExecutorClass1;
            try {
                androidExecutorClass1 = Class.forName("io.americanexpress.busybee.android.internal.AndroidMainThreadExecutor");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Must load busybee-android when running on Android", e);
            }
            Field instance = Reflection.getField(androidExecutorClass1, "INSTANCE");
            return (Executor) Reflection.getValue(instance);
        } else {
            return jvmExecutor();
        }
    }

}
