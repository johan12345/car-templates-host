/*
 * This file is part of the car-templates-host distribution (https://github.com/johan12345/car-templates-host).
 * Copyright (C) 2023 Johan von Forstner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.android.car.templates.host.view.widgets.maps

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceMarker
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.android.car.libraries.apphost.common.LocationMediator
import com.android.car.libraries.apphost.common.TemplateContext
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.view.widget.map.AbstractMapViewContainer
import com.android.car.templates.host.R
import com.google.common.collect.ImmutableList
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style

/** A layout that wraps a single map view and encapsulates the logic to manipulate it.  */
class MaplibreMapViewContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : AbstractMapViewContainer(context, attrs, defStyleAttr, defStyleRes) {
    private val mLifecycleRegistry = LifecycleRegistry(this)
    private val mMapView: MapView

    /** The max zoom level to ever reach in the map view.  */
    private val mMaxZoomLevel: Float
    private val mMarkers = ArrayList<Marker>(NUMBER_OF_MARKERS)
    private var mTemplateContext: TemplateContext? = null
    private var mMap: MapboxMap? = null

    /** `true` when the view is started, `false` when stopped.  */
    private var mIsStarted = false
    private var mIsAnchorDirty = false
    private var mArePlacesDirty = false
    private var mAnchorMarker: Marker? = null
    private var mAnchor: Place? = null

    /** whether the map should show the current location.  */
    private var mCurrentLocationEnabled = false

    /** A list with the places displayed on the map.  */
    private var mPlaces: List<Place> = ImmutableList.of()

    /**
     * Whether the view has ever completed a successful update. We use this to know whether the camera
     * needs to be animated or not.
     */
    private var mHasUpdated = false

    /**
     * Instantiates a new map view container.
     *
     * @see android.content.res.Resources.Theme.obtainStyledAttributes
     */
    /**
     * Instantiates a new map view container.
     *
     * @see android.content.res.Resources.Theme.obtainStyledAttributes
     */
    /**
     * Instantiates a new map view container.
     *
     * @see android.content.res.Resources.Theme.obtainStyledAttributes
     */
    /** Instantiates a new map view container.  */
    init {
        Mapbox.getInstance(context, context.getString(R.string.mapbox_access_token), WellKnownTileServer.Mapbox)
        LayoutInflater.from(context).inflate(R.layout.mapbox_map_view, this)
        mMapView = findViewById(R.id.map_view)
        val outValue = TypedValue()
        resources.getValue(R.dimen.map_max_zoom_level, outValue, true)
        mMaxZoomLevel = outValue.float
        mLifecycleRegistry.addObserver(this)
    }

    override fun setTemplateContext(templateContext: TemplateContext) {
        mTemplateContext = templateContext
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }

    override fun getLifecycleRegistry(): LifecycleRegistry {
        return mLifecycleRegistry
    }

    override fun onCreate(owner: LifecycleOwner) {
        mMapView.getMapAsync { map ->
            mMap = map
            map.setStyle("mapbox://styles/mapbox/streets-v11") { style ->
                map.locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, style)
                        .useDefaultLocationEngine(true)
                        .build()
                )
                if (checkLocationPermission()) {
                    map.locationComponent.isLocationComponentEnabled = mCurrentLocationEnabled
                }
            }

            // Set the maximum zoom level, so that when we update the camera, it doesn't go past
            // this value. The camera update logic we use tries to set the camera at the maximum
            // level of zoom possible given a set of places to bind it to.
            map.setMaxZoomPreference(mMaxZoomLevel.toDouble())

            // Updates the insets of the map to ensure the markers aren't drawn behind any other
            // widgets on the screen.
            updateMapInsets(map)

            // Returns true to disable marker click events globally. This disables the default
            // behavior where clicking on a marker centers it on the map.
            //TODO: map.setOnMarkerClickListener(MapStub.OnMarkerClickListener())
            val uiSettings = map.uiSettings
            uiSettings.setAllGesturesEnabled(false)
            update(UPDATE_REASON_ON_CREATE)
        }
    }

    private fun checkLocationPermission() = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    override fun onStart(owner: LifecycleOwner) {
        mIsStarted = true
        update(UPDATE_REASON_ON_START)
    }

    override fun onStop(owner: LifecycleOwner) {
        mIsStarted = false
    }

    override fun setCurrentLocationEnabled(enable: Boolean) {
        mCurrentLocationEnabled = true
        if (checkLocationPermission()) {
            mMap?.locationComponent?.isLocationComponentEnabled = mCurrentLocationEnabled
        }
    }

    /** Sets the map anchor. The camera will be adjusted to include the anchor marker if necessary.  */
    override fun setAnchor(anchor: Place?) {
        mIsAnchorDirty = true
        mAnchor = anchor
        update(UPDATE_REASON_SET_ANCHOR)
    }

    /**
     * Sets the places to display in the map. The camera will be moved to the region that contains all
     * the places.
     */
    override fun setPlaces(places: List<Place>) {
        if (mPlaces.containsAll(places) && places.containsAll(mPlaces)) {
            return
        }
        mArePlacesDirty = true
        mPlaces = places
        update(UPDATE_REASON_SET_PLACES)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun getPlaces(): List<Place> {
        return mPlaces
    }

    private fun update(updateReason: String) {
        // Three conditions need to happen before we update the view:
        // 1. the map needs to be initialized,
        // 2. the view must have gone through a layout pass (so that it can calculate the viewport
        // rect for the camera),
        // 3. the view must be in STARTED state.
        if (mMap == null || !mMapView.isLaidOut || !mIsStarted) {
            return
        }
        Log.d(LogTags.TEMPLATE, "Updating map view, reason: $updateReason")

        // Do not animate the camera the very first time, but animate it in any subsequent updates.
        updateCamera( /* animate= */mHasUpdated)
        updateAnchorMarker()
        updatePlaceMarkers()
        mHasUpdated = true
    }

    private fun updatePlaceMarkers() {
        if (mMap == null || !mArePlacesDirty) {
            return
        }
        Log.d(LogTags.TEMPLATE, "Updating map location markers")

        // Clean up the existing markers.
        for (marker in mMarkers) {
            mMap!!.removeMarker(marker)
        }
        mMarkers.clear()


        // Add the new ones.
        for (place in mPlaces) {
            val marker = place.marker
            val location = place.location

            if (marker != null) {
                val icon = makeIcon(marker)
                mMarkers.add(mMap!!.addMarker(MarkerOptions()
                    .position(location.toLatLng())
                    .icon(icon)))
            }
        }
        mArePlacesDirty = false
    }

    private fun makeIcon(marker: PlaceMarker): Icon? {
        val vd = ContextCompat.getDrawable(context, R.drawable.map_marker)!!
        marker.color?.let { DrawableCompat.setTint(vd, it.color) }
        DrawableCompat.setTintMode(vd, PorterDuff.Mode.MULTIPLY)
        val bm = Bitmap.createBitmap(
            vd.intrinsicWidth, vd.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bm)
        vd.setBounds(0, 0, vd.intrinsicWidth, vd.intrinsicHeight)
        vd.draw(canvas)
        return IconFactory.getInstance(context).fromBitmap(bm)
    }

    private fun updateAnchorMarker() {
        if (!mIsAnchorDirty) return
        val map = mMap ?: return
        Log.d(LogTags.TEMPLATE, "Updating map anchor marker")

        // Clean up the existing marker.
        mAnchorMarker?.let {
            map.removeMarker(it)
        }

        // Add the new one.
        if (mAnchor != null) {
            val marker = mAnchor!!.marker
            val location = mAnchor!!.location
            if (marker != null) {
                val icon = makeIcon(marker)
                mAnchorMarker = map.addMarker(MarkerOptions()
                    .position(location.toLatLng())
                    .icon(icon))
            }
            mIsAnchorDirty = false
        }
    }

    private fun updateCamera(animate: Boolean) {
        var hasPlace = false
        var location = if (mAnchor != null) mAnchor!!.location else null
        if (location != null) {
            hasPlace = true
        } else {
            Log.w(
                LogTags.TEMPLATE,
                "Anchor location is expected but not set, excluding from camera: $mAnchor"
            )
        }
        val mediator = mTemplateContext!!.getAppHostService(
            LocationMediator::class.java
        )!!
        if (!hasPlace) {
            // Try to maintain the previous camera location if available.
            val anchor = mediator.cameraAnchor ?: return
            location = anchor
        }
        mediator.cameraAnchor = location

        val camUpdate = if (mMarkers.size > 0) {
            val bounds =
                LatLngBounds.fromLatLngs(mMarkers.map { it.position } + location!!.toLatLng())
            CameraUpdateFactory.newLatLngBounds(bounds,
                stableArea.left,
                stableArea.top,
                width - stableArea.right,
                height - stableArea.bottom)
        } else {
            CameraUpdateFactory.newLatLngZoom(location!!.toLatLng(), 10.0)
        }
        if (animate) {
            mMap!!.animateCamera(camUpdate)
        } else {
            mMap!!.moveCamera(camUpdate)
        }
    }

    private fun CarLocation.toLatLng(): LatLng {
        return LatLng(
            this.latitude,
            this.longitude
        )
    }

    private var stableArea: Rect = Rect(0, 0, width, height)

    private fun updateMapInsets(map: MapboxMap?) {
        if (mTemplateContext == null) {
            return
        }
        stableArea = mTemplateContext!!.surfaceInfoProvider.stableArea ?: Rect(0, 0, width, height)
        if (map != null) {
            map.moveCamera(CameraUpdateFactory.paddingTo(
                stableArea.left.toDouble(),
                stableArea.top.toDouble(),
                (width - stableArea.right).toDouble(),
                (height - stableArea.bottom).toDouble()
            ))
            update(UPDATE_REASON_MAP_INSETS)
        }
    }

    companion object {
        // Strings indicating the reason for a view update used for logging purposes.
        private const val UPDATE_REASON_SET_PLACES = "set_places"
        private const val UPDATE_REASON_SET_ANCHOR = "set_anchor"
        private const val UPDATE_REASON_MAP_INSETS = "map_insets"
        private const val UPDATE_REASON_ON_CREATE = "on_create"
        private const val UPDATE_REASON_ON_START = "on_start"
        private const val NUMBER_OF_MARKERS = 8

        /** Returns an AOSPMapViewContainer  */
        @JvmStatic
        fun create(context: Context?, theme: Int): AbstractMapViewContainer {
            return inflate(
                ContextThemeWrapper(context, theme), R.layout.mapbox_map_view_container_layout, null
            ) as AbstractMapViewContainer
        }

        /** Returns an AOSPMapViewContainer  */
        fun create(context: Context?): MaplibreMapViewContainer {
            return inflate(
                context,
                R.layout.mapbox_map_view_container_layout,
                null
            ) as MaplibreMapViewContainer
        }
    }
}