/*
 * Copyright (C) 2021 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.alpha.editor

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.android.wearable.alpha.data.watchface.ColorStyleIdAndResourceIds
import com.example.android.wearable.alpha.databinding.ActivityWatchFaceConfigBinding
import com.example.android.wearable.alpha.editor.WatchFaceConfigViewModel.Companion.MINUTE_HAND_LENGTH_DEFAULT_FOR_SLIDER
import com.example.android.wearable.alpha.editor.WatchFaceConfigViewModel.Companion.MINUTE_HAND_LENGTH_MAXIMUM_FOR_SLIDER
import com.example.android.wearable.alpha.editor.WatchFaceConfigViewModel.Companion.MINUTE_HAND_LENGTH_MINIMUM_FOR_SLIDER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Allows user to edit certain parts of the watch face (color style, ticks displayed, minute arm
 * length) by using the [WatchFaceConfigViewModel]. (All widgets are disabled until data is loaded.)
 */
class WatchFaceConfigActivity : ComponentActivity() {
    private val viewModel: WatchFaceConfigViewModel by viewModels {
        WatchFaceConfigViewModel.WatchFaceConfigViewModelFactory(
            this@WatchFaceConfigActivity,
            intent
        )
    }

    private lateinit var binding: ActivityWatchFaceConfigBinding

    private var initialDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        binding = ActivityWatchFaceConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disable widgets until data loads and values are set.
        binding.colorStylePickerButton.isEnabled = false
        binding.ticksEnabledSwitch.isEnabled = false
        binding.minuteHandLengthSlider.isEnabled = false

        // Set max and min.
        binding.minuteHandLengthSlider.valueTo = MINUTE_HAND_LENGTH_MAXIMUM_FOR_SLIDER
        binding.minuteHandLengthSlider.valueFrom = MINUTE_HAND_LENGTH_MINIMUM_FOR_SLIDER
        binding.minuteHandLengthSlider.value = MINUTE_HAND_LENGTH_DEFAULT_FOR_SLIDER

        binding.minuteHandLengthSlider.addOnChangeListener { slider, value, fromUser ->
            Log.d(TAG, "addOnChangeListener(): $slider, $value, $fromUser")
            viewModel.setMinuteHandArmLength(value)
        }

        lifecycleScope.launch(Dispatchers.Main.immediate) {
            viewModel.uiState.collect { uiState: WatchFaceConfigViewModel.EditWatchFaceUiState ->
                when (uiState) {
                    is WatchFaceConfigViewModel.EditWatchFaceUiState.Loading -> {
                        Log.d(TAG, "StateFlow Loading: ${uiState.message}")
                    }
                    is WatchFaceConfigViewModel.EditWatchFaceUiState.Success -> {
                        Log.d(TAG, "StateFlow Success.")
                        updateWatchFacePreview(uiState.userStylesAndPreview)
                    }
                    is WatchFaceConfigViewModel.EditWatchFaceUiState.Error -> {
                        Log.e(TAG, "Flow error: ${uiState.exception}")
                    }
                }
            }
        }
    }

    private fun updateWatchFacePreview(
        userStylesAndPreview: WatchFaceConfigViewModel.UserStylesAndPreview
    ) {
        Log.d(TAG, "updateWatchFacePreview: $userStylesAndPreview")

        val colorStyleId: String = userStylesAndPreview.colorStyleId
        Log.d(TAG, "\tselected color style: $colorStyleId")

        binding.ticksEnabledSwitch.isChecked = userStylesAndPreview.ticksEnabled
        binding.minuteHandLengthSlider.value = userStylesAndPreview.minuteHandLength
        binding.preview.watchFaceBackground.setImageBitmap(userStylesAndPreview.previewImage)

        if (!initialDataLoaded) {
            initialDataLoaded = true
            enabledWidgets()
        }
    }

    private fun enabledWidgets() {
        binding.colorStylePickerButton.isEnabled = true
        binding.ticksEnabledSwitch.isEnabled = true
        binding.minuteHandLengthSlider.isEnabled = true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        // Makes sure the activity closes.
        finish()
        super.onDestroy()
    }

    fun onClickColorStylePickerButton(view: View) {
        Log.d(TAG, "onClickColorStylePickerButton() $view")

        // TODO (codingjeremy): Replace with a RecyclerView to choose color style (next CL)
        // Selects a random color style from list.
        val colorStyleIdAndResourceIdsList = enumValues<ColorStyleIdAndResourceIds>()
        val numbers: IntRange = colorStyleIdAndResourceIdsList.indices

        val random = numbers.random()
        val newColorStyle = colorStyleIdAndResourceIdsList[random]
        val newColorStyleId = newColorStyle.id

        Log.d(TAG, "\tnewColorStyleId (random): $newColorStyleId")

        val newColorStyleValue = ColorStyleIdAndResourceIds.toOption(
            applicationContext,
            newColorStyleId
        )

        viewModel.setColorStyle(newColorStyleValue)
    }

    fun onClickTicksEnabledSwitch(view: View) {
        Log.d(TAG, "onClickTicksEnabledSwitch() $view")
        viewModel.setDrawPips(binding.ticksEnabledSwitch.isChecked)
    }

    companion object {
        const val TAG = "WatchFaceConfigActivity"
    }
}
