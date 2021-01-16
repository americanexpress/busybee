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

import io.americanexpress.busybee.BusyBee
import io.americanexpress.busybee.BusyBee.NoLongerBusyCallback
import io.americanexpress.busybee.BusyBee.OperationMatcher

/**
 * This is a version of BusyBee that is used when the tests are not running.
 * It does nothing.
 * This minimizes the overhead of having BusyBee in your app.
 */
class NoOpBusyBee internal constructor() : BusyBee {
    override val name: String
        get() = "NO-OP BusyBee"

    override fun busyWith(operation: Any) {}
    override fun busyWith(operation: Any, category: BusyBee.Category) {}
    override fun registerNoLongerBusyCallback(noLongerBusyCallback: NoLongerBusyCallback) {}
    override fun payAttentionToCategory(category: BusyBee.Category) {}
    override fun ignoreCategory(category: BusyBee.Category) {}
    override fun completedEverythingInCategory(category: BusyBee.Category) {}
    override fun completedEverything() {}
    override fun completedEverythingMatching(matcher: OperationMatcher) {}
    override fun completed(operation: Any) {}
    override val isNotBusy: Boolean
        get() = true
    override val isBusy: Boolean
        get() = false

    override fun toStringVerbose(): String {
        return "NO-OP BusyBee"
    }
}