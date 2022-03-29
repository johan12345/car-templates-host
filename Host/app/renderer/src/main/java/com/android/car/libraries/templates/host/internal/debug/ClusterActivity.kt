/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.car.libraries.templates.host.internal.debug

import android.annotation.SuppressLint
import android.car.Car
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.car.libraries.apphost.common.TemplateContext
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.view.common.CarTextParams
import com.android.car.libraries.templates.host.internal.HostNavState
import com.android.car.libraries.templates.host.internal.NavigationCoordinator
import com.android.car.libraries.templates.host.view.widgets.navigation.CompactStepView
import com.android.car.libraries.templates.host.view.widgets.navigation.DetailedStepView
import com.android.car.libraries.templates.host.view.widgets.navigation.ProgressView
import com.android.car.libraries.templates.host.view.widgets.navigation.TravelEstimateView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.android.car.libraries.templates.host.R

/**
 * This activity will be launched by the system to show the user navigation updates in the
 * instrument cluster.
 */
class ClusterActivity : AppCompatActivity() {
  private lateinit var root: ViewGroup

  // travel estimate card will only be enabled if there's enough room on screen
  private var travelEstimateEnabled = false
  private var travelEstimateView: TravelEstimateView? = null
  private var travelEstimateContainer: ViewGroup? = null
  private lateinit var detailedStepView: DetailedStepView
  private lateinit var compactStepView: CompactStepView
  private lateinit var progressView: ProgressView

  private var carTextParams: CarTextParams? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.cluster_activity)
    root = findViewById(R.id.root)
    travelEstimateContainer = findViewById(R.id.travel_estimate_card_container)
    travelEstimateView = findViewById(R.id.travel_estimate_view)
    detailedStepView = findViewById(R.id.detailed_step_view)
    compactStepView = findViewById(R.id.compact_step_view)
    progressView = findViewById(R.id.progress_view)

    initColors()
    // `root` hasn't finished measuring yet, and will report width=0, so we need to throw this work
    // to end of the MainLooper's queue.
    Handler(Looper.getMainLooper()).post {
      adjustViewport(intent)
      calcTravelEstimateEnabled()
    }

    observeNavigationState()
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    adjustViewport(intent)
  }

  private fun initColors() {
    // Read the fallback color to use with the app-defined card background color.
    @StyleableRes
    val themeAttrs =
      intArrayOf(
        com.android.car.libraries.templates.host.R.attr.templateNavCardFallbackContentColor
      )
    val ta = obtainStyledAttributes(themeAttrs)
    val contentColor = ta.getColor(0, Color.WHITE)
    ta.recycle()
    detailedStepView.setTextColor(contentColor)
    compactStepView.setTextColor(contentColor)
    progressView.setColor(contentColor)
  }

  private fun observeNavigationState() {
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        NavigationCoordinator.getInstance(applicationContext).navigationState.collect { state ->
          when (state) {
            HostNavState.NotNavigating -> {
              detailedStepView.setStepAndDistance(null, null, null, null, Color.TRANSPARENT, false)
              compactStepView.setStep(null, null, null, Color.TRANSPARENT)
              travelEstimateContainer?.visibility = View.GONE
            }
            is HostNavState.Navigating -> {
              renderTrip(state)
            }
          }
        }
      }
    }
  }

  private fun renderTrip(state: HostNavState.Navigating) {
    val trip = state.trip
    val templateContext = state.templateContext
    val step = trip.steps.firstOrNull()
    val travelEstimate = trip.stepTravelEstimates.firstOrNull()
    val nextStep = trip.steps.elementAtOrNull(1)
    val carTextParams =
      carTextParams ?: createStepTextParams(templateContext).also { carTextParams = it }
    detailedStepView.setStepAndDistance(
      templateContext,
      step,
      travelEstimate?.remainingDistance,
      carTextParams,
      Color.TRANSPARENT,
      false
    )
    compactStepView.setStep(templateContext, nextStep, carTextParams, Color.TRANSPARENT)

    progressView.visibility = if (trip.isLoading) View.VISIBLE else View.GONE
    if (travelEstimateEnabled && travelEstimate != null) {
      travelEstimateContainer?.visibility = View.VISIBLE
      travelEstimateView?.setTravelEstimate(templateContext, travelEstimate)
    } else {
      travelEstimateContainer?.visibility = View.GONE
    }
  }

  /**
   * Some of the display might be obscured by either the shape of the physical screen, or other
   * elements in the cluster display. We need to respect this constraint and only display our UI
   * within those bounds.
   */
  private fun adjustViewport(intent: Intent?) {
    intent ?: return
    val bundle = intent.getBundleExtra(Car.CAR_EXTRA_CLUSTER_ACTIVITY_STATE) ?: return
    val viewport = bundle.getParcelable<Rect>("android.car:activityState.unobscured") ?: return

    L.d(LogTags.CLUSTER) { "cluster un-obscured area: $viewport" }
    root.setPadding(
      viewport.left,
      viewport.top,
      root.width - viewport.right,
      root.height - viewport.bottom
    )
  }

  private fun calcTravelEstimateEnabled() {
    val top = root.top + root.paddingTop
    val bottom = root.bottom - root.paddingBottom
    val safeAreaHeight = bottom - top
    val threshold =
      resources.getDimensionPixelSize(R.dimen.travel_estimate_card_min_height_threshold)
    travelEstimateEnabled = safeAreaHeight > threshold
  }

  /**
   * Returns a [CarTextParams] instance to use for the text of a step.
   *
   * Unlike other text elsewhere, image spans are allowed in these strings.
   */
  @SuppressLint("ResourceType")
  private fun createStepTextParams(templateContext: TemplateContext): CarTextParams? {
    @StyleableRes
    val themeAttrs =
      intArrayOf(
        R.attr.templateRoutingImageSpanRatio,
        R.attr.templateRoutingImageSpanBody2MaxHeight,
        R.attr.templateRoutingImageSpanBody3MaxHeight
      )
    val ta = templateContext.obtainStyledAttributes(themeAttrs)
    val imageRatio = ta.getFloat(0, 0f)
    val body2MaxHeight = ta.getDimensionPixelSize(1, 0)
    ta.recycle()
    val maxWidth = (body2MaxHeight * imageRatio).toInt()
    return CarTextParams.builder()
      .setImageBoundingBox(Rect(0, 0, maxWidth, body2MaxHeight))
      .setMaxImages(2)
      .setColorSpanConstraints(CarColorConstraints.NO_COLOR)
      .build()
  }
}
