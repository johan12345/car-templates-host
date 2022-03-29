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
package com.android.car.libraries.templates.host.view.widgets.common.testing;

import static android.view.View.VISIBLE;

import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import com.android.car.libraries.templates.host.view.widgets.common.FabView;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Test helper for the action strip. */
public class ActionStripHelper {
  private static final int LAYOUT_WIDTH = 400;
  private static final int LAYOUT_HEIGHT = 600;

  private final ViewGroup mActionStripView;

  public ActionStripHelper(ViewGroup actionStripView) {
    mActionStripView = actionStripView;
  }

  /** Force a measure and layout for the action strip. */
  public void measureAndLayout() {
    mActionStripView.measure(
        MeasureSpec.makeMeasureSpec(LAYOUT_WIDTH, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(LAYOUT_HEIGHT, MeasureSpec.EXACTLY));
    mActionStripView.layout(0, 0, LAYOUT_WIDTH, LAYOUT_HEIGHT);
  }

  /** Returns a {@link List} of {@link FabView}s from the action strip. */
  @Nullable
  public List<FabView> getFabViews() {
    List<FabView> views = new ArrayList<>();
    for (int i = 0; i < mActionStripView.getChildCount(); i++) {
      FabView fabView = (FabView) mActionStripView.getChildAt(i);
      if (fabView.getVisibility() == VISIBLE) {
        views.add((FabView) mActionStripView.getChildAt(i));
      }
    }
    return views;
  }
}
