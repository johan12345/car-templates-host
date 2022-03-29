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

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.CarToast;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceMarker;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import com.android.car.libraries.apphost.common.LocationMediator;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.widget.map.AbstractMapViewContainer;
import com.android.car.templates.host.view.widgets.maps.MapStub.Marker;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.ArrayList;
import java.util.List;

/** A layout that wraps a single map view and encapsulates the logic to manipulate it. */
public class MapViewStubContainer extends AbstractMapViewContainer {
  // Strings indicating the reason for a view update used for logging purposes.
  private static final String UPDATE_REASON_SET_PLACES = "set_places";
  private static final String UPDATE_REASON_SET_ANCHOR = "set_anchor";
  private static final String UPDATE_REASON_MAP_INSETS = "map_insets";
  private static final String UPDATE_REASON_ON_CREATE = "on_create";
  private static final String UPDATE_REASON_ON_START = "on_start";
  private static final int NUMBER_OF_MARKERS = 8;

  @SuppressWarnings("nullness")
  private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

  private final MapViewStub mMapView;
  /** The max zoom level to ever reach in the map view. */
  private final float mMaxZoomLevel;

  private final ArrayList<Marker> mMarkers = new ArrayList<>(NUMBER_OF_MARKERS);

  private TemplateContext mTemplateContext;
  private MapStub mMap;

  /** {@code true} when the view is started, {@code false} when stopped. */
  private boolean mIsStarted;

  private boolean mIsAnchorDirty;
  private boolean mArePlacesDirty;

  @Nullable private Marker mAnchorMarker;
  private Place mAnchor;

  /** whether the map should show the current location. */
  private boolean mCurrentLocationEnabled = false;

  /** A list with the places displayed on the map. */
  private List<Place> mPlaces = ImmutableList.of();

  /**
   * Whether the view has ever completed a successful update. We use this to know whether the camera
   * needs to be animated or not.
   */
  private boolean mHasUpdated = false;

  /**
   * Instantiates a new map view container.
   *
   * @see android.content.res.Resources.Theme#obtainStyledAttributes
   */
  @SuppressWarnings("nullness")
  public MapViewStubContainer(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    LayoutInflater.from(context).inflate(com.android.car.templates.host.R.layout.map_view_stub, this);
    mMapView = findViewById(com.android.car.templates.host.R.id.map_view);

    TypedValue outValue = new TypedValue();
    getResources().getValue(com.android.car.templates.host.R.dimen.map_max_zoom_level, outValue, true);
    mMaxZoomLevel = outValue.getFloat();

    mLifecycleRegistry.addObserver(this);
  }

  /** Instantiates a new map view container. */
  public MapViewStubContainer(@ApplicationContext Context context) {
    this(context, null);
  }

  /**
   * Instantiates a new map view container.
   *
   * @see android.content.res.Resources.Theme#obtainStyledAttributes
   */
  public MapViewStubContainer(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  /**
   * Instantiates a new map view container.
   *
   * @see android.content.res.Resources.Theme#obtainStyledAttributes
   */
  public MapViewStubContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  /** Returns an AOSPMapViewContainer */
  public static AbstractMapViewContainer create(Context context, int theme) {
    return (AbstractMapViewContainer)
        View.inflate(
            new ContextThemeWrapper(context, theme), com.android.car.templates.host.R.layout.map_view_stub_container_layout, null);
  }

  /** Returns an AOSPMapViewContainer */
  public static MapViewStubContainer create(Context context) {
    return (MapViewStubContainer)
        View.inflate(context, com.android.car.templates.host.R.layout.map_view_stub_container_layout, null);
  }

  @Override
  public void setTemplateContext(TemplateContext templateContext) {
    mTemplateContext = templateContext;
  }

  @NonNull
  @Override
  public Lifecycle getLifecycle() {
    return mLifecycleRegistry;
  }

  @NonNull
  @Override
  public LifecycleRegistry getLifecycleRegistry() {
    return mLifecycleRegistry;
  }

  @Override
  public void onCreate(LifecycleOwner owner) {
    mMap = mMapView.getMap();

    // Set the maximum zoom level, so that when we update the camera, it doesn't go past
    // this value. The camera update logic we use tries to set the camera at the maximum
    // level of zoom possible given a set of places to bind it to.
    mMap.setMaxZoomPreference(mMaxZoomLevel);

    // Updates the insets of the map to ensure the markers aren't drawn behind any other
    // widgets on the screen.
    updateMapInsets(mMap);

    // Returns true to disable marker click events globally. This disables the default
    // behavior where clicking on a marker centers it on the map.
    mMap.setOnMarkerClickListener(new MapStub.OnMarkerClickListener());

    MapStub.UiSettings uiSettings = mMap.getUiSettings();
    uiSettings.setMyLocationButtonEnabled(false);
    uiSettings.setAllGesturesEnabled(false);

    mMap.setMyLocationEnabled(mCurrentLocationEnabled);
    update(UPDATE_REASON_ON_CREATE);
  }

  @Override
  public void onStart(LifecycleOwner owner) {
    mIsStarted = true;
    update(UPDATE_REASON_ON_START);
  }

  @Override
  public void onStop(LifecycleOwner owner) {
    mIsStarted = false;
  }

  @Override
  public void setCurrentLocationEnabled(boolean enable) {
    mCurrentLocationEnabled = true;
    mMap.setMyLocationEnabled(mCurrentLocationEnabled);
  }

  /** Sets the map anchor. The camera will be adjusted to include the anchor marker if necessary. */
  @Override
  @SuppressWarnings("nullness:assignment")
  public void setAnchor(@Nullable Place anchor) {
    mIsAnchorDirty = true;
    mAnchor = anchor;
    update(UPDATE_REASON_SET_ANCHOR);
  }

  /**
   * Sets the places to display in the map. The camera will be moved to the region that contains all
   * the places.
   */
  @Override
  public void setPlaces(List<Place> places) {
    if (mPlaces.containsAll(places) && places.containsAll(mPlaces)) {
      return;
    }
    mArePlacesDirty = true;
    mPlaces = places;
    update(UPDATE_REASON_SET_PLACES);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public List<Place> getPlaces() {
    return mPlaces;
  }

  private void update(String updateReason) {
    // Three conditions need to happen before we update the view:
    // 1. the map needs to be initialized,
    // 2. the view must have gone through a layout pass (so that it can calculate the viewport
    // rect for the camera),
    // 3. the view must be in STARTED state.
    if (mMap == null || !mMapView.isLaidOut() || !mIsStarted) {
      return;
    }

    Log.d(LogTags.TEMPLATE, "Updating map view, reason: " + updateReason);

    // Do not animate the camera the very first time, but animate it in any subsequent updates.
    updateCamera(/* animate= */ mHasUpdated);
    updateAnchorMarker();
    updatePlaceMarkers();
    mHasUpdated = true;
  }

  private void updatePlaceMarkers() {
    if (mMap == null || !mArePlacesDirty) {
      return;
    }

    Log.d(LogTags.TEMPLATE, "Updating map location markers");

    // Clean up the existing markers.
    for (MapStub.Marker marker : mMarkers) {
      mMap.removeMarker(marker);
    }
    mMarkers.clear();

    // Add the new ones.
    for (Place place : mPlaces) {
      PlaceMarker marker = place.getMarker();
      CarLocation location = place.getLocation();

      if (location != null) {
        // Skip null (invisible) markers.
        if (marker != null) {
          mMap.addMarker(new MapStub.Marker(location, marker.getColor()));
        }
      } else {
        Log.w(LogTags.TEMPLATE, "Place location is expected but not set: " + place);
      }
    }

    mArePlacesDirty = false;
  }

  private void updateAnchorMarker() {
    if (mMap == null || !mIsAnchorDirty) {
      return;
    }
    MapStub map = mMap;

    Log.d(LogTags.TEMPLATE, "Updating map anchor marker");

    // Clean up the existing marker.
    if (mAnchorMarker != null) {
      map.removeMarker(mAnchorMarker);
    }

    // Add the new one.
    if (mAnchor != null) {
      PlaceMarker marker = mAnchor.getMarker();
      CarLocation location = mAnchor.getLocation();

      if (location != null) {
        if (marker != null) {
          map.addMarker(new MapStub.Marker(location, requireNonNull(marker.getColor())));
        }
      } else {
        Log.w(LogTags.TEMPLATE, "Anchor location is expected but not set: " + mAnchor);
      }

      mIsAnchorDirty = false;
    }
  }

  private void updateCamera(boolean animate) {
    boolean hasPlace = false;
    CarLocation location = mAnchor != null ? mAnchor.getLocation() : null;

    if (location != null) {
      hasPlace = true;
    } else {
      Log.w(
          LogTags.TEMPLATE,
          "Anchor location is expected but not set, excluding from camera: " + mAnchor);
    }

    LocationMediator mediator =
        requireNonNull(mTemplateContext.getAppHostService(LocationMediator.class));
    if (!hasPlace) {
      // Try to maintain the previous camera location if available.
      CarLocation anchor = mediator.getCameraAnchor();
      if (anchor == null) {
        return;
      }

      location = anchor;
    }

    mediator.setCameraAnchor(location);
    if (animate) {
      mMap.animateCamera(requireNonNull(location), CarToast.LENGTH_SHORT);
    } else {
      mMap.moveCamera(requireNonNull(location));
    }
  }

  private void updateMapInsets(MapStub map) {
    if (mTemplateContext == null) {
      return;
    }
    Rect stableArea = mTemplateContext.getSurfaceInfoProvider().getStableArea();
    stableArea = stableArea != null ? stableArea : new Rect(0, 0, getWidth(), getHeight());

    if (map != null) {
      map.setPadding(
          stableArea.left,
          stableArea.top,
          getWidth() - stableArea.right,
          getHeight() - stableArea.bottom);
      update(UPDATE_REASON_MAP_INSETS);
    }
  }
}
