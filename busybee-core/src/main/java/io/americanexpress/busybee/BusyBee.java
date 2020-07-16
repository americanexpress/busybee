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

package io.americanexpress.busybee;

import androidx.annotation.NonNull;

import io.americanexpress.busybee.internal.BusyBeeSingleton;

public interface BusyBee {

    /**
     * Generally, you want just one instance of BusyBee for your whole process.
     * <p>
     * For release apps with no tests running, this will return a instance of BusyBee
     * that does nothing, so there is minimal overhead for your release builds.
     *
     * @return the single global instance of BusyBee
     */
    static @NonNull
    BusyBee singleton() {
        return BusyBeeSingleton.singleton();
    }

    @NonNull
    String getName();

    /**
     * Tell BusyBee that a new operation is now keeping the app "busy".
     * The operation that is passed in represents the ongoing operation.
     * Espresso will wait until `operation` is {@link #completed(Object)} (or an object that
     * is {@link Object#equals(Object)} to `operation` is completed.
     *
     * This operation will be placed in the {@link Category#defaultCategory()}.
     *
     * @param operation An object that identifies the operation that is keeping the app "busy".
     *                  Must have a correct implementation of {@link Object#equals(Object)}/{@link Object#hashCode()}.
     *                  Also, should have a meaningful {@link Object#toString()}.
     */
    void busyWith(@NonNull Object operation);

    /**
     * Record the start of an async operation.
     *
     * @param operation An object that identifies the request. Must have a correct equals()/hashCode().
     * @param category Which {@link Category} the given operation will be associated with
     */
    void busyWith(@NonNull Object operation, @NonNull BusyBee.Category category);

    /**
     * Get notified every time this BusyBee instance goes from being `busyWith` to no longer busy.
     *
     * @param noLongerBusyCallback callback
     */
    void registerNoLongerBusyCallback(@NonNull BusyBee.NoLongerBusyCallback noLongerBusyCallback);

    /**
     * If there are any operations in progress in the given category, then BusyBee will be in the busy state.
     * By default, BusyBee "pays attention" to all categories
     *
     * @param category - Category to stop ignoring
     * @see BusyBee#ignoreCategory(Category)
     */
    void payAttentionToCategory(@NonNull BusyBee.Category category);

    /**
     * If there are any operations in progress in the given category, then BusyBee will ignore those operations.
     * By default, BusyBee DOES NOT ignore any categories.
     * <p>
     * You can reverse this by calling {@link BusyBee#payAttentionToCategory(Category)}
     *
     * @param category - Category to be ignored
     * @see BusyBee#payAttentionToCategory(Category)
     */
    void ignoreCategory(@NonNull BusyBee.Category category);

    /**
     * All operations in progress in this category will be marked as completed (i.e. no longer busy)
     *
     * @param category Everything in this category will be completed.
     * @see BusyBee#busyWith(Object)
     */
    void completedEverythingInCategory(@NonNull BusyBee.Category category);

    /**
     * All operations in progress in all categories will be marked as completed (i.e. no longer busy)
     *
     * @see BusyBee#busyWith(Object)
     */
    void completedEverything();

    /**
     * `complete` all operations for which {@link OperationMatcher#matches(Object)} returns true
     *
     * @param matcher - Logic for selecting which operations will be completed
     * @see OperationMatcher
     */
    void completedEverythingMatching(@NonNull BusyBee.OperationMatcher matcher);

    /**
     * Marks an operation as complete, but the completion is done asynchronously (returns immediately) on the MainThread.
     * This means completion of the operation won't happen until this makes it to the front of the main thread queue.
     * <p>
     * The `operation` passed into this method should be something that was previously passed to {@link #busyWith(Object)}
     * If BusyBee wasn't already tracking the `operation` (either because it was already passed to completed()
     * or it was never {@link #busyWith(Object)} in the first place), then this method will have no effect.
     *
     * More info about the Android "main thread": https://www.youtube.com/watch?v=eAtMon8ndfk
     *
     * @param operation - Must have compliant implementations of `equals` and `hashcode`
     */
    void completed(@NonNull Object operation);

    /**
     * No operations that we are paying attention to is currently in progress
     *
     * @return true if and only if we are not busyWith an
     */
    boolean isNotBusy();

    /**
     * @return true if and only if there are operations in progress (i.e. `busyWith`) for categories
     *         we are "paying attention" to.
     * @see BusyBee#payAttentionToCategory(Category)
     * @see BusyBee#ignoreCategory(Category)
     */
    boolean isBusy();

    /**
     * Dumps the state of this BusyBee instance, that can be used for debugging
     *
     * @return String with internal state of the BusyBee instance.
     */
    @NonNull
    String toStringVerbose();

    interface NoLongerBusyCallback {
        /**
         * Called when there are no more operations in progress (that we are paying attention to)
         */
        void noLongerBusy();
    }

    interface OperationMatcher {
        boolean matches(@NonNull Object o);
    }

    enum Category {
        GENERAL,
        NETWORK,
        DIALOG;

        public static @NonNull
        Category defaultCategory() {
            return GENERAL;
        }
    }
}
