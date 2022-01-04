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
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

repositories {
    google()
    jcenter()
}

android {
    compileSdkVersion = libs.versions.compileSdk.get()
    defaultConfig {
        minSdk = libs.versions.minimumSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        version = 1
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}


dependencies {
    api(project(":busybee-core"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.espresso.idling.resource)

    androidTestImplementation(libs.assertj.core)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.espresso.core)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit4)
}
