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

import io.americanexpress.busybee.internal.BusyBeeSingleton

interface BusyBee {
    fun getName(): String

    /**
     * Tell BusyBee that a new operation is now keeping the app "busy".
     * The operation that is passed in represents the ongoing operation.
     * Espresso will wait until `operation` is [.completed] (or an object that
     * is [Object.equals] to `operation` is completed.
     *
     * This operation will be placed in the [Category.defaultCategory].
     *
     * @param operation An object that identifies the operation that is keeping the app "busy".
     * Must have a correct implementation of [Object.equals]/[Object.hashCode].
     * Also, should have a meaningful [Object.toString].
     */
    fun busyWith(operation: Any)

    /**
     * Record the start of an async operation.
     *
     * @param operation An object that identifies the request. Must have a correct equals()/hashCode().
     * @param category Which [Category] the given operation will be associated with
     */
    fun busyWith(operation: Any, category: Category)

    /**
     * Get notified every time this BusyBee instance goes from being `busyWith` to no longer busy.
     *
     * @param noLongerBusyCallback callback
     */
    fun registerNoLongerBusyCallback(noLongerBusyCallback: NoLongerBusyCallback)

    /**
     * If there are any operations in progress in the given category, then BusyBee will be in the busy state.
     * By default, BusyBee "pays attention" to all categories
     *
     * @param category - Category to stop ignoring
     * @see BusyBee.ignoreCategory
     */
    fun payAttentionToCategory(category: Category)

    /**
     * If there are any operations in progress in the given category, then BusyBee will ignore those operations.
     * By default, BusyBee DOES NOT ignore any categories.
     *
     *
     * You can reverse this by calling [BusyBee.payAttentionToCategory]
     *
     * @param category - Category to be ignored
     * @see BusyBee.payAttentionToCategory
     */
    fun ignoreCategory(category: Category)

    /**
     * All operations in progress in this category will be marked as completed (i.e. no longer busy)
     *
     * @param category Everything in this category will be completed.
     * @see BusyBee.busyWith
     */
    fun completedEverythingInCategory(category: Category)

    /**
     * All operations in progress in all categories will be marked as completed (i.e. no longer busy)
     *
     * @see BusyBee.busyWith
     */
    fun completedEverything()

    /**
     * `complete` all operations for which [OperationMatcher.matches] returns true
     *
     * @param matcher - Logic for selecting which operations will be completed
     * @see OperationMatcher
     */
    fun completedEverythingMatching(matcher: OperationMatcher)

    /**
     * Marks an operation as complete, but the completion is done asynchronously (returns immediately) on the MainThread.
     * This means completion of the operation won't happen until this makes it to the front of the main thread queue.
     *
     *
     * The `operation` passed into this method should be something that was previously passed to [.busyWith]
     * If BusyBee wasn't already tracking the `operation` (either because it was already passed to completed()
     * or it was never [.busyWith] in the first place), then this method will have no effect.
     *
     * More info about the Android "main thread": https://www.youtube.com/watch?v=eAtMon8ndfk
     *
     * @param operation - Must have compliant implementations of `equals` and `hashcode`
     */
    fun completed(operation: Any)

    /**
     * No operations that we are paying attention to is currently in progress
     *
     * @return true if and only if we are not busyWith an
     */
    fun isNotBusy(): Boolean

    /**
     * @return true if and only if there are operations in progress (i.e. `busyWith`) for categories
     * we are "paying attention" to.
     * @see BusyBee.payAttentionToCategory
     * @see BusyBee.ignoreCategory
     */
    fun isBusy(): Boolean

    /**
     * Dumps the state of this BusyBee instance, that can be used for debugging
     *
     * @return String with internal state of the BusyBee instance.
     */
    fun toStringVerbose(): String
    fun interface NoLongerBusyCallback {
        /**
         * Called when there are no more operations in progress (that we are paying attention to)
         */
        fun noLongerBusy()
    }

    fun interface OperationMatcher {
        fun matches(o: Any): Boolean
    }

    enum class Category {
        GENERAL, NETWORK, DIALOG;

        companion object {
            fun defaultCategory(): Category {
                return GENERAL
            }
        }
    }

    companion object {
        /**
         * Generally, you want just one instance of BusyBee for your whole process.
         *
         *
         * For release apps with no tests running, this will return a instance of BusyBee
         * that does nothing, so there is minimal overhead for your release builds.
         *
         * @return the single global instance of BusyBee
         */
        fun singleton(): BusyBee {
            return BusyBeeSingleton.singleton()
        }
    }
}