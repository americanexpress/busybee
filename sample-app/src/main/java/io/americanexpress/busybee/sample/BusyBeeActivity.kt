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
package io.americanexpress.busybee.sample

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.americanexpress.busybee.BusyBee
import io.americanexpress.busybee.R
import java.util.concurrent.Executors

class BusyBeeActivity : AppCompatActivity() {
    private val busyBee = BusyBee.singleton()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_busy_bee)
        val confirmation = findViewById<TextView>(R.id.confirmation)
        onButtonClick(View.OnClickListener {
            val foo = "FOO"
            busyBee.busyWith(foo)
            Executors.newSingleThreadExecutor().execute {
                Thread.sleep(1000)
                confirmation.post {
                    confirmation.text = getString(R.string.button_pressed)
                    busyBee.completed(foo)
                }
            }
        })
    }

    fun onButtonClick(clickListener: View.OnClickListener) {
        val button = findViewById<Button>(R.id.press_me_button)
        button.setOnClickListener(clickListener)
    }
}
