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

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

repositories {
    jcenter()
}

group = "io.americanexpress.busybee.app"

android {
    compileSdkVersion = libs.versions.compileSdk.get()
    defaultConfig {
        minSdk = libs.versions.minimumSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        version = 1
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        isWarningsAsErrors = true
        disable("GoogleAppIndexingWarning", // not needed for a sample app
            "MissingApplicationIcon")  // not needed for a sample app
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":busybee-android"))
    implementation(libs.androidx.material)
    implementation(libs.kotlin.stdlib)

    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.idling.resource)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.assertj.core)
}
