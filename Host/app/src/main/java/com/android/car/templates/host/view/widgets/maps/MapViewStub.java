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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** A Map view that holds mock map. */
public class MapViewStub extends FrameLayout {
  private MapStub mMap;
  private TextView mTextView;

  public MapViewStub(@NonNull Context context) {
    super(context);
  }

  public MapViewStub(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  /**
   * Instantiates a new map view.
   *
   * @see android.content.res.Resources.Theme#obtainStyledAttributes
   */
  public MapViewStub(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  /** Instantiates a new map view. */
  @SuppressWarnings("nullness:argument")
  public MapViewStub(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    LayoutInflater.from(context).inflate(com.android.car.templates.host.R.layout.map_stub_layout, this);
    mTextView = findViewById(com.android.car.templates.host.R.id.map_info);
    mMap = new MapStub();
    mMap.setOnMapUpdateListener(() -> mTextView.setText(mMap.toString()));
  }

  /**
   * Return a mock map. This method is a substitute of getMapAsync() in other map implementation.
   */
  public MapStub getMap() {
    return mMap;
  }
}
