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
package com.android.car.libraries.templates.host.internal

import android.car.cluster.navigation.CueKt.cueElement
import android.car.cluster.navigation.LaneKt.laneDirection
import android.car.cluster.navigation.NavigationState
import android.car.cluster.navigation.NavigationState.Lane.LaneDirection.Shape
import android.car.cluster.navigation.NavigationState.Maneuver.Type as ManeuverType
import android.car.cluster.navigation.NavigationState.NavigationStateProto.ServiceStatus.NORMAL
import android.car.cluster.navigation.NavigationState.NavigationStateProto.ServiceStatus.REROUTING
import android.car.cluster.navigation.cue
import android.car.cluster.navigation.destination
import android.car.cluster.navigation.distance
import android.car.cluster.navigation.lane
import android.car.cluster.navigation.maneuver
import android.car.cluster.navigation.navigationStateProto
import android.car.cluster.navigation.road
import android.car.cluster.navigation.step
import android.car.cluster.navigation.timestamp
import android.car.navigation.CarNavigationStatusManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.Distance
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.model.Lane
import androidx.car.app.navigation.model.LaneDirection
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.Trip
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.android.car.libraries.apphost.common.TemplateContext
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.view.common.DateTimeUtils
import com.android.car.libraries.apphost.view.common.DistanceUtils
import com.android.car.libraries.apphost.view.common.ImageUtils
import com.android.car.libraries.apphost.view.common.ImageViewParams
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.UUID
import kotlinx.coroutines.coroutineScope

/**
 * Convert a [Trip] (from [NavigationManager]) to a [NavigationState.NavigationStateProto] that the
 * Cluster can parse and display (via [CarNavigationStatusManager.sendNavigationStateChange]).
 */
class NavigationStateConverterImpl(private val templateContext: TemplateContext) :
  NavigationStateConverter {

  /** If the Provider gave an error once, we don't want to keep hitting it */
  private var skipIcons = false

  override suspend fun tripToNavigationState(trip: Trip) = coroutineScope {
    navigationStateProto {
      serviceStatus = if (trip.isLoading) REROUTING else NORMAL
      trip.currentRoad?.let { currentRoad = road { name = it.toString() } }
      steps += trip.getNavigationStateSteps()
      destinations += trip.getNavigationStateDestinations()
    }
  }

  private fun Trip.getNavigationStateDestinations(): List<NavigationState.Destination> =
    destinations.zip(destinationTravelEstimates).map { (destination, estimate) ->
      destination {
        destination.name?.toString()?.let { title = it }
        destination.address?.toString()?.let { address = it }
        distance = estimate.toNavStateDistance()
        estimate.arrivalTimeAtDestination?.timeSinceEpochMillis?.let { epochMillis ->
          estimatedTimeAtArrival = timestamp { seconds = epochMillis / 1000 }
        }
        formattedDurationUntilArrival = estimate.getFormattedRemainingDuration(templateContext)
        estimate.arrivalTimeAtDestination?.zoneShortName?.let { zoneId = it }
      }
    }

  private fun Trip.getNavigationStateSteps(): List<NavigationState.Step> {
    return steps.zip(stepTravelEstimates).map { (step, estimate) ->
      step {
        step.maneuver?.let { maneuver = it.toNavStateManeuver() }
        distance = estimate.toNavStateDistance()
        step.cue?.let { cue = it.toNavStateCue() }
        lanes += step.lanes.map { it.toNavStateLane() }
        step.lanesImage?.toImageReference()?.let { lanesImage = it }
      }
    }
  }

  private fun Lane.toNavStateLane() = lane {
    laneDirections +=
      directions.map { laneDirection ->
        laneDirection {
          shape = laneDirection.shape.toNavStateShape()
          isHighlighted = laneDirection.isRecommended
        }
      }
  }

  private fun Int.toNavStateShape() =
    when (this) {
      LaneDirection.SHAPE_UNKNOWN -> Shape.UNKNOWN
      LaneDirection.SHAPE_STRAIGHT -> Shape.STRAIGHT
      LaneDirection.SHAPE_SLIGHT_LEFT -> Shape.SLIGHT_LEFT
      LaneDirection.SHAPE_SLIGHT_RIGHT -> Shape.SLIGHT_RIGHT
      LaneDirection.SHAPE_NORMAL_LEFT -> Shape.NORMAL_LEFT
      LaneDirection.SHAPE_NORMAL_RIGHT -> Shape.NORMAL_RIGHT
      LaneDirection.SHAPE_SHARP_LEFT -> Shape.SHARP_LEFT
      LaneDirection.SHAPE_SHARP_RIGHT -> Shape.SHARP_RIGHT
      else -> Shape.UNRECOGNIZED
    }

  private fun CarText.toNavStateCue() = cue {
    val cueText = this@toNavStateCue.toString()
    alternateText = cueText
    elements += cueElement { text = cueText }
  }

  private fun TravelEstimate.toNavStateDistance() = distance {
    meters = DistanceUtils.getMeters(remainingDistance)
    remainingDistance?.let {
      displayValue = DistanceUtils.convertDistanceToDisplayStringNoUnit(templateContext, it)
    }
    displayUnits =
      when (remainingDistance?.displayUnit) {
        Distance.UNIT_METERS -> NavigationState.Distance.Unit.METERS
        Distance.UNIT_KILOMETERS, Distance.UNIT_KILOMETERS_P1 ->
          NavigationState.Distance.Unit.KILOMETERS
        Distance.UNIT_MILES, Distance.UNIT_MILES_P1 -> NavigationState.Distance.Unit.MILES
        Distance.UNIT_FEET -> NavigationState.Distance.Unit.FEET
        Distance.UNIT_YARDS -> NavigationState.Distance.Unit.YARDS
        else -> NavigationState.Distance.Unit.UNKNOWN
      }
  }

  /**
   * Only Resource/Bitmap icons are supported. Will return [null] for all other types of [CarIcon]
   */
  private fun CarIcon.toImageReference(): NavigationState.ImageReference? {
    if (skipIcons) return null

    val iconId = hash(this).toString()

    // Don't extract a Drawable unless needed
    ClusterIconContentProvider.queryIconData(iconId, templateContext)?.let {
      (contentUri, aspectRatio) ->
      return NavigationState.ImageReference.newBuilder()
        .setContentUri(contentUri)
        .setAspectRatio(aspectRatio)
        .build()
    }

    // No cache for icon, get Drawable and cache it
    val drawable =
      ImageUtils.getIconDrawable(templateContext, this, ImageViewParams.DEFAULT)
        ?: run {
          L.d(LogTags.NAVIGATION) {
            "Couldn't obtain Drawable from CarIcon (uri icons not supported): $this"
          }
          return null
        }

    val aspectRatio =
      if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
        drawable.intrinsicWidth.toDouble() / drawable.intrinsicHeight.toDouble()
      } else {
        L.w(LogTags.NAVIGATION) {
          "Drawable has no intrinsic dimensions aspect ratio. carIcon=$this"
        }
        return null
      }

    val contentUri =
      runCatching {
          val bytes = drawable.toByteArray()
          ClusterIconContentProvider.addToCache(iconId, bytes, templateContext)
        }
        .onFailure {
          skipIcons = true
          L.w(LogTags.NAVIGATION, it) {
            "Failed to cache icon in provider." +
              " Disabling cluster icons for ${templateContext.appPackageName}"
          }
        }
        .getOrNull()
        ?.toString()
        ?: return null

    return NavigationState.ImageReference.newBuilder()
      .setContentUri(contentUri)
      .setAspectRatio(aspectRatio)
      .build()
  }

  private fun Drawable.toByteArray(): ByteArray {
    val bitmap = this.toBitmap()
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
  }

  private fun hash(icon: CarIcon): Int {
    return when (icon.icon?.type) {
      IconCompat.TYPE_RESOURCE, IconCompat.TYPE_URI, null -> icon.hashCode()
      else -> {
        // For any iconCompat type that exists and isn't URI or Resource, we don't really
        // know how to tell if two instances represent the same set of pixels.
        // So we just consider them unique.
        UUID.randomUUID().hashCode()
      }
    }
  }

  private fun Maneuver.toNavStateManeuver() = maneuver {
    val maneuver = this@toNavStateManeuver
    type = maneuver.getNavStateType()
    roundaboutExitNumber = maneuver.roundaboutExitNumber
    maneuver.icon?.toImageReference()?.let { icon = it }
  }

  private fun Maneuver.getNavStateType(): ManeuverType =
    when (type) {
      Maneuver.TYPE_UNKNOWN -> ManeuverType.UNKNOWN
      Maneuver.TYPE_DEPART -> ManeuverType.DEPART
      Maneuver.TYPE_NAME_CHANGE -> ManeuverType.NAME_CHANGE
      Maneuver.TYPE_KEEP_LEFT -> ManeuverType.KEEP_LEFT
      Maneuver.TYPE_KEEP_RIGHT -> ManeuverType.KEEP_RIGHT
      Maneuver.TYPE_TURN_SLIGHT_LEFT -> ManeuverType.TURN_SLIGHT_LEFT
      Maneuver.TYPE_TURN_SLIGHT_RIGHT -> ManeuverType.TURN_SLIGHT_RIGHT
      Maneuver.TYPE_TURN_NORMAL_LEFT -> ManeuverType.TURN_NORMAL_LEFT
      Maneuver.TYPE_TURN_NORMAL_RIGHT -> ManeuverType.TURN_NORMAL_RIGHT
      Maneuver.TYPE_TURN_SHARP_LEFT -> ManeuverType.TURN_SHARP_LEFT
      Maneuver.TYPE_TURN_SHARP_RIGHT -> ManeuverType.TURN_SHARP_RIGHT
      Maneuver.TYPE_U_TURN_LEFT -> ManeuverType.U_TURN_LEFT
      Maneuver.TYPE_U_TURN_RIGHT -> ManeuverType.U_TURN_RIGHT
      Maneuver.TYPE_ON_RAMP_SLIGHT_LEFT -> ManeuverType.ON_RAMP_SLIGHT_LEFT
      Maneuver.TYPE_ON_RAMP_SLIGHT_RIGHT -> ManeuverType.ON_RAMP_SLIGHT_RIGHT
      Maneuver.TYPE_ON_RAMP_NORMAL_LEFT -> ManeuverType.ON_RAMP_NORMAL_LEFT
      Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT -> ManeuverType.ON_RAMP_NORMAL_RIGHT
      Maneuver.TYPE_ON_RAMP_SHARP_LEFT -> ManeuverType.ON_RAMP_SHARP_LEFT
      Maneuver.TYPE_ON_RAMP_SHARP_RIGHT -> ManeuverType.ON_RAMP_SHARP_RIGHT
      Maneuver.TYPE_ON_RAMP_U_TURN_LEFT -> ManeuverType.ON_RAMP_U_TURN_LEFT
      Maneuver.TYPE_ON_RAMP_U_TURN_RIGHT -> ManeuverType.ON_RAMP_U_TURN_RIGHT
      Maneuver.TYPE_OFF_RAMP_SLIGHT_LEFT -> ManeuverType.OFF_RAMP_SLIGHT_LEFT
      Maneuver.TYPE_OFF_RAMP_SLIGHT_RIGHT -> ManeuverType.OFF_RAMP_SLIGHT_RIGHT
      Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT -> ManeuverType.OFF_RAMP_NORMAL_LEFT
      Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT -> ManeuverType.OFF_RAMP_NORMAL_RIGHT
      Maneuver.TYPE_FORK_LEFT -> ManeuverType.FORK_LEFT
      Maneuver.TYPE_FORK_RIGHT -> ManeuverType.FORK_RIGHT
      Maneuver.TYPE_MERGE_LEFT -> ManeuverType.MERGE_LEFT
      Maneuver.TYPE_MERGE_RIGHT -> ManeuverType.MERGE_RIGHT
      Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED -> ManeuverType.MERGE_SIDE_UNSPECIFIED
      Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CW
      Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE -> {
        when (roundaboutExitAngle) {
          in 6..45 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CW_SHARP_LEFT
          in 46..135 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CW_NORMAL_LEFT
          in 136..170 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CW_SLIGHT_LEFT
          in 171..189 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CW_STRAIGHT
          in 190..224 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CW_SLIGHT_RIGHT
          in 225..314 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CW_NORMAL_RIGHT
          in 315..354 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CW_SHARP_RIGHT
          in 1..5, in 355..360 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CW_U_TURN
          else -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CW
        }
      }
      Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CCW
      Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE -> {
        when (roundaboutExitAngle) {
          in 6..45 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CCW_SHARP_RIGHT
          in 46..135 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CCW_NORMAL_RIGHT
          in 136..170 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CCW_SLIGHT_RIGHT
          in 171..189 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CCW_STRAIGHT
          in 190..224 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CCW_SLIGHT_LEFT
          in 225..314 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CCW_NORMAL_LEFT
          in 315..354 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CCW_SHARP_LEFT
          in 1..5, in 355..360 -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CCW_U_TURN
          else -> ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CCW
        }
      }
      Maneuver.TYPE_STRAIGHT -> ManeuverType.STRAIGHT
      Maneuver.TYPE_FERRY_BOAT -> ManeuverType.FERRY_BOAT
      Maneuver.TYPE_FERRY_TRAIN -> ManeuverType.FERRY_TRAIN
      Maneuver.TYPE_DESTINATION -> ManeuverType.DESTINATION
      Maneuver.TYPE_DESTINATION_STRAIGHT -> ManeuverType.DESTINATION_STRAIGHT
      Maneuver.TYPE_DESTINATION_LEFT -> ManeuverType.DESTINATION_LEFT
      Maneuver.TYPE_DESTINATION_RIGHT -> ManeuverType.DESTINATION_RIGHT
      Maneuver.TYPE_ROUNDABOUT_ENTER_CW, Maneuver.TYPE_ROUNDABOUT_EXIT_CW ->
        ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CW
      Maneuver.TYPE_ROUNDABOUT_ENTER_CCW, Maneuver.TYPE_ROUNDABOUT_EXIT_CCW ->
        ManeuverType.ROUNDABOUT_ENTER_AND_EXIT_CCW
      Maneuver.TYPE_FERRY_BOAT_LEFT,
      Maneuver.TYPE_FERRY_BOAT_RIGHT,
      Maneuver.TYPE_FERRY_TRAIN_LEFT,
      Maneuver.TYPE_FERRY_TRAIN_RIGHT -> ManeuverType.FERRY_TRAIN
      else -> ManeuverType.UNKNOWN
    }

  private fun TravelEstimate.getFormattedRemainingDuration(templateContext: TemplateContext) =
    if (remainingTimeSeconds == TravelEstimate.REMAINING_TIME_UNKNOWN) ""
    else
      DateTimeUtils.formatDurationString(templateContext, Duration.ofSeconds(remainingTimeSeconds))
}

/** Just a convenience to get the Client package name */
private val TemplateContext.appPackageName
  get() = carAppPackageInfo.componentName.packageName
