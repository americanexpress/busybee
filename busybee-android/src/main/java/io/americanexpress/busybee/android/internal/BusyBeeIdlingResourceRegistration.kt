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
package io.americanexpress.busybee.android.internal

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.test.espresso.IdlingRegistry
import io.americanexpress.busybee.BusyBee
import io.americanexpress.busybee.internal.EnvironmentChecks

/**
 * We register BusyBeeIdlingResource with espresso when the application loads.
 *
 *
 * Auto-registration inspired by LeakCanary
 * https://github.com/square/leakcanary/blob/934174edd0c04f2937733aae4a01f836c67b5b52/leakcanary-object-watcher-android/src/main/java/leakcanary/internal/AppWatcherInstaller.kt
 */
class BusyBeeIdlingResourceRegistration : ContentProvider() {
    override fun onCreate(): Boolean {
        if (EnvironmentChecks.testsAreRunning()) {
            IdlingRegistry.getInstance().register(BusyBeeIdlingResource(BusyBee.singleton()))
        }
        return true
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ) = 0

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? = null

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ) = 0
}