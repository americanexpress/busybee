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

import io.americanexpress.busybee.internal.EnvironmentChecks.androidJunitRunnerIsPresent
import io.americanexpress.busybee.internal.EnvironmentChecks.doNotPretendTestsAreNotRunning
import io.americanexpress.busybee.internal.EnvironmentChecks.pretendTestsAreNotRunning
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test

class BusyBeeSingletonTest {
    @Test
    fun whenInJvmTest_espressoTestsAreNotRunning() {
        assertThat(androidJunitRunnerIsPresent()).isFalse()
    }

    @Test
    fun whenInJvmTest_useRealBusyBee() {
        assertThat(BusyBeeSingleton.create()).isInstanceOf(RealBusyBee::class.java)
    }

    @Test
    fun whenTestsAreNotRunning_useNoOpBusyBee() {
        pretendTestsAreNotRunning()
        assertThat(BusyBeeSingleton.create()).isInstanceOf(NoOpBusyBee::class.java)
        doNotPretendTestsAreNotRunning()
    }
}