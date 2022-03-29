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
package com.android.car.libraries.templates.host.view.widgets.navigation;

import static android.graphics.Color.TRANSPARENT;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.TravelEstimate;
import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.common.DateTimeUtils;
import com.android.car.libraries.apphost.view.common.DistanceUtils;
import com.android.car.libraries.templates.host.R;
import com.android.car.ui.widget.CarUiTextView;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;

/**
 * A view that displays a travel estimate for the navigation trip.
 *
 * <p>This view tries to display elements from the {@link TravelEstimate} data. For example if
 * available, it would show the estimated time of arrival and distance to destination.
 */
public class TravelEstimateView extends LinearLayout {
  private static final String INTERPUNCT = "\u00b7";
  private static final String TIME_AND_DISTANCE_SEPARATOR = " " + INTERPUNCT + " ";

  private CarUiTextView mArrivalTimeText;
  private CarUiTextView mTimeAndDistanceText;
  @Nullable private TravelEstimate mTravelEstimate;

  public TravelEstimateView(Context context) {
    this(context, null);
  }

  public TravelEstimateView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TravelEstimateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public TravelEstimateView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mArrivalTimeText = findViewById(R.id.arrival_time_text);
    mTimeAndDistanceText = findViewById(R.id.time_and_distance_text);
  }

  /** Sets the {@link TravelEstimate} or hides the view if set to {@code null} */
  @SuppressWarnings("NewApi") // java.time APIs are OK through de-sugaring.
  public void setTravelEstimate(
      TemplateContext templateContext, @Nullable TravelEstimate travelEstimate) {
    L.v(LogTags.TEMPLATE, "Setting travel estimate view: %s", travelEstimate);

    mTravelEstimate = travelEstimate;
    if (travelEstimate == null) {
      setVisibility(GONE);
      return;
    }

    // Display the arrival time.
    DateTimeWithZone arrivalTime = travelEstimate.getArrivalTimeAtDestination();
    if (arrivalTime != null) {
      mArrivalTimeText.setText(
          DateTimeUtils.formatArrivalTimeString(
              templateContext, arrivalTime, ZoneId.systemDefault()));
    } else {
      // This shouldn't happen since the API should enforce a non-null arrival time.
      mArrivalTimeText.setText(new ArrayList<>());
    }

    // Display the remaining trip time.
    // The destination travel estimate's duration should not be unknown, but if it is, use an
    // empty
    // string.
    long remainingTimeSeconds = travelEstimate.getRemainingTimeSeconds();
    String timeString =
        remainingTimeSeconds == TravelEstimate.REMAINING_TIME_UNKNOWN
            ? ""
            : DateTimeUtils.formatDurationString(
                templateContext, Duration.ofSeconds(remainingTimeSeconds));
    Distance distance = travelEstimate.getRemainingDistance();
    String distanceString;
    if (distance != null) {
      distanceString = DistanceUtils.convertDistanceToDisplayString(templateContext, distance);
    } else {
      distanceString = "";
      L.w(LogTags.TEMPLATE, "Remaining distance for the travel estimate is expected but not set");
    }
    String timeAndDistanceString = timeString + TIME_AND_DISTANCE_SEPARATOR + distanceString;

    // If we have a valid custom text color, use it.
    SpannableString timeAndDistanceSpannable = new SpannableString(timeAndDistanceString);

    @ColorInt
    int remainingTimeColor =
        CarColorUtils.resolveColor(
            templateContext,
            travelEstimate.getRemainingTimeColor(),
            /* isDark= */ false,
            /* defaultColor= */ TRANSPARENT,
            CarColorConstraints.STANDARD_ONLY);
    setStringColorSpan(remainingTimeColor, timeAndDistanceSpannable, 0, timeString.length());

    @ColorInt
    int remainingDistanceColor =
        CarColorUtils.resolveColor(
            templateContext,
            travelEstimate.getRemainingDistanceColor(),
            /* isDark= */ false,
            /* defaultColor= */ TRANSPARENT,
            CarColorConstraints.STANDARD_ONLY);
    setStringColorSpan(
        remainingDistanceColor,
        timeAndDistanceSpannable,
        timeString.length() + TIME_AND_DISTANCE_SEPARATOR.length(),
        timeAndDistanceString.length());

    mTimeAndDistanceText.setText(timeAndDistanceSpannable);
  }

  /** Sets a color span in the given {@link SpannableString}. */
  private static void setStringColorSpan(
      @ColorInt int color, SpannableString spannable, int start, int end) {
    if (color != TRANSPARENT) {
      spannable.setSpan(
          new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  @VisibleForTesting
  @Nullable
  public TravelEstimate getTravelEstimate() {
    return mTravelEstimate;
  }
}
