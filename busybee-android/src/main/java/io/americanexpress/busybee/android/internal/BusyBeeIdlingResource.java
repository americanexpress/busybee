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
package io.americanexpress.busybee.android.internal;

import androidx.test.espresso.IdlingResource;

import io.americanexpress.busybee.BusyBee;

/**
 * This class is a bridge between espresso's IdlingResource and the app, so the app doesn't have to depend on espresso.
 * <p>
 * You must register it with Espresso:
 * `IdlingRegistry.getInstance().register(new BusyBeeIdlingResource(BusyBee.singleton()));`
 */
public class BusyBeeIdlingResource implements IdlingResource {

    private final BusyBee busyBee;

    BusyBeeIdlingResource(final BusyBee busyBee) {
        this.busyBee = busyBee;
    }

    @Override
    public String getName() {
        return busyBee.getName();
    }

    @Override
    public boolean isIdleNow() {
        return busyBee.isNotBusy();
    }

    @Override
    public void registerIdleTransitionCallback(final ResourceCallback resourceCallback) {
        busyBee.registerNoLongerBusyCallback(resourceCallback::onTransitionToIdle);
    }
}
