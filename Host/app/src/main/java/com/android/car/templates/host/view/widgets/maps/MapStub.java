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
package com.android.car.templates.host.view.widgets.maps;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.car.app.CarToast;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarLocation;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.LogTags;
import java.util.ArrayList;
import java.util.Arrays;

/** A mock map used to simulate all the APIs of a actual map. */
public class MapStub {
  /** A listener that is called when any parameter of this map changes. */
  private OnMapUpdateListener mOnMapUpdateListener;

  private final UiSettings mUiSettings;
  private final ArrayList<Marker> mMarkers = new ArrayList<>();

  private OnMarkerClickListener mOnMarkerClickListener;
  private CarLocation mCarLocation;

  private boolean mIsLocationEnabled;
  private boolean mHasAnimation;

  private float mMaxZoomPreference;
  private int mAnimationDuration;
  private int mPaddingLeft;
  private int mPaddingRight;
  private int mPaddingTop;
  private int mPaddingBottom;

  /** Instantiates a new mock map. */
  public MapStub() {
    mUiSettings = new UiSettings();
  }

  /** sets OnMapUpdateListener for this map. */
  public void setOnMapUpdateListener(OnMapUpdateListener onMapUpdateListener) {
    mOnMapUpdateListener = onMapUpdateListener;
  }

  /** sets MaxZoomPreference for this map. */
  public void setMaxZoomPreference(float maxZoomPreference) {
    Log.d(LogTags.TEMPLATE, "MaxZoomPreference is updated, " + maxZoomPreference);
    mMaxZoomPreference = maxZoomPreference;
    update();
  }

  /** sets OnMarkerClickListener for this map. */
  public void setOnMarkerClickListener(MapStub.OnMarkerClickListener listener) {
    Log.d(LogTags.TEMPLATE, "setOnMarkerClickListener");
    mOnMarkerClickListener = listener;
    update();
  }

  /** Enables location. */
  public void setMyLocationEnabled(boolean enabled) {
    Log.d(LogTags.TEMPLATE, "setMyLocationEnabled: " + enabled);
    mIsLocationEnabled = enabled;
    update();
  }

  /** Sets paddings. */
  public void setPadding(int left, int top, int right, int bottom) {
    Log.d(LogTags.TEMPLATE, "setPaddings: " + left + ", " + top + ", " + right + ", " + bottom);
    mPaddingLeft = left;
    mPaddingRight = right;
    mPaddingTop = top;
    mPaddingBottom = bottom;
    update();
  }

  /** Add a marker. */
  public void addMarker(Marker marker) {
    if (marker == null) {
      return;
    }
    Log.d(LogTags.TEMPLATE, "add a marker" + marker);
    mMarkers.add(marker);
    update();
  }

  /** Remove a marker. */
  public boolean removeMarker(Marker marker) {
    if (marker == null || !mMarkers.contains(marker)) {
      return false;
    }
    Log.d(LogTags.TEMPLATE, "remove a marker" + marker);
    mMarkers.remove(marker);
    update();
    return true;
  }

  /** Returns UiSettings. */
  public UiSettings getUiSettings() {
    return mUiSettings;
  }

  /** Updates the camera. */
  public void animateCamera(CarLocation location, int animationDuration) {
    Log.d(LogTags.TEMPLATE, "move camera with animation to " + location);
    mCarLocation = location;
    mHasAnimation = true;
    mAnimationDuration = animationDuration;
    update();
  }

  /** Updates the camera. */
  public void moveCamera(CarLocation location) {
    Log.d(LogTags.TEMPLATE, "Move Camera to: " + location);
    mCarLocation = location;
    mHasAnimation = false;
    mAnimationDuration = 0;
    update();
  }

  private void update() {
    mOnMapUpdateListener.onMapUpdate();
  }

  @Override
  public String toString() {
    StringBuilder mapInfo = new StringBuilder();
    String newLine = System.lineSeparator();
    mapInfo
        .append(String.format("UiSettings: %s", mUiSettings))
        .append(newLine)
        .append(String.format("setMaxZoomPreference to be %f", mMaxZoomPreference))
        .append(newLine)
        .append(String.format("setOnMarkerClickListener %s", mOnMarkerClickListener))
        .append(newLine)
        .append(String.format("setMyLocationEnabled: %s", mIsLocationEnabled))
        .append(newLine)
        .append(
            String.format(
                "setPaddings: %d, %d, %d, %d",
                mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom))
        .append(newLine)
        .append(String.format("Markers: %s", Arrays.toString(mMarkers.toArray())))
        .append(newLine)
        .append(String.format("move camera to %s", mCarLocation))
        .append(newLine)
        .append(
            (mHasAnimation
                ? String.format("with animation (duration: %d)", mAnimationDuration)
                : "without animation"));
    return mapInfo.toString();
  }

  /** A listener that is called when any parameter of this map changes. */
  public interface OnMapUpdateListener {
    /** Called when map is updated. */
    void onMapUpdate();
  }

  /** A mock Marker class. */
  public static class Marker {
    private final CarLocation mLocation;
    @Nullable private final CarColor mMarkerColor;

    /** Instantiates a marker */
    public Marker(CarLocation location, @Nullable CarColor color) {
      mLocation = location;
      mMarkerColor = color;
    }

    @Override
    public String toString() {
      return "Marker location: " + mLocation + ", " + "color: " + mMarkerColor;
    }
  }

  /** A mock OnMarkerClickListener class. */
  public static class OnMarkerClickListener implements View.OnTouchListener {

    /** Called when a touch event is dispatched. */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
      float positionX = event.getX();
      float positionY = event.getY();
      Log.d(LogTags.TEMPLATE, "OnMarkerClickListener: X: " + positionX + ", Y: " + positionY);
      ((TemplateContext) v.getContext())
          .getToastController()
          .showToast(
              "OnMarkerClickListener: X: " + positionX + ", Y: " + positionY, CarToast.LENGTH_LONG);
      return true;
    }
  }

  /** A mock class for UiSettings. */
  public static class UiSettings {
    private boolean mIsMyLocationButtonEnabled;
    private boolean mIsAllGesturesEnabled;

    public void setMyLocationButtonEnabled(boolean enabled) {
      mIsMyLocationButtonEnabled = enabled;
    }

    public void setAllGesturesEnabled(boolean enabled) {
      mIsAllGesturesEnabled = enabled;
    }

    @Override
    public String toString() {
      return "IsMyLocationButtonEnabled: "
          + mIsMyLocationButtonEnabled
          + ", IsAllGesturesEnabled: "
          + mIsAllGesturesEnabled;
    }
  }
}
