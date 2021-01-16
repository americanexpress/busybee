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

import androidx.annotation.NonNull;

import io.americanexpress.busybee.BusyBee;

/**
 * This is a version of BusyBee that is used when the tests are not running.
 * It does nothing.
 * This minimizes the overhead of having BusyBee in your app.
 */
public class NoOpBusyBee implements BusyBee {
    NoOpBusyBee() {
    }

    @NonNull
    @Override
    public String getName() {
        return "NO-OP BusyBee";
    }

    @Override
    public void busyWith(@NonNull Object operation) {
    }

    @Override
    public void busyWith(@NonNull Object operation, @NonNull final BusyBee.Category category) {
    }

    @Override
    public void registerNoLongerBusyCallback(@NonNull final BusyBee.NoLongerBusyCallback noLongerBusyCallback) {
    }

    @Override
    public void payAttentionToCategory(@NonNull final BusyBee.Category category) {
    }

    @Override
    public void ignoreCategory(@NonNull final BusyBee.Category category) {
    }

    @Override
    public void completedEverythingInCategory(@NonNull final BusyBee.Category category) {
    }

    @Override
    public void completedEverything() {
    }

    @Override
    public void completedEverythingMatching(@NonNull BusyBee.OperationMatcher matcher) {
    }

    @Override
    public void completed(@NonNull Object operation) {
    }

    @Override
    public boolean isNotBusy() {
        return true;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @NonNull
    @Override
    public String toStringVerbose() {
        return "NO-OP BusyBee";
    }
}
