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

import org.junit.Test;

import io.americanexpress.busybee.android.internal.AndroidMainThreadExecutor;

import static io.americanexpress.busybee.internal.EnvironmentChecks.isAndroid;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class AndroidMainThreadExecutorTest {

    @Test
    public void whenOnAndroid_thenWeGetAndroidMainThreadExecutor() {
        assertThat(MainThread.singletonExecutor()).isInstanceOf(AndroidMainThreadExecutor.class);
    }

    @Test
    public void whenOnAndroid_thenIsAndroidShouldBeTrue() {
        assertThat(isAndroid()).isTrue();
    }
}