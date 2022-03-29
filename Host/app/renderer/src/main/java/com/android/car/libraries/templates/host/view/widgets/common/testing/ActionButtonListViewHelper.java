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

import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import com.android.car.libraries.templates.host.view.widgets.common.ActionButtonListView;
import com.android.car.libraries.templates.host.view.widgets.common.ActionButtonView;

/** Test helper for the action button list view. */
public class ActionButtonListViewHelper {
  private static final int LAYOUT_WIDTH = 400;
  private static final int LAYOUT_HEIGHT = 600;

  private final ViewGroup mActionButtonListView;

  public ActionButtonListViewHelper(ViewGroup actionButtonListView) {
    mActionButtonListView = actionButtonListView;
  }

  /** Force a measure and layout for the action strip. */
  public void measureAndLayout() {
    mActionButtonListView.measure(
        MeasureSpec.makeMeasureSpec(LAYOUT_WIDTH, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(LAYOUT_HEIGHT, MeasureSpec.EXACTLY));
    mActionButtonListView.layout(0, 0, LAYOUT_WIDTH, LAYOUT_HEIGHT);
  }

  /** Returns a {@link ActionButtonView} at {@code index} in the {@code mActionButtonListView} */
  public ActionButtonView getAction(int index) {
    return (ActionButtonView) mActionButtonListView.getChildAt(index);
  }

  /** Returns an {@link ActionButtonListView} instance of the {@code mActionButtonListView} */
  public ActionButtonListView getActionButtonListView() {
    return (ActionButtonListView) mActionButtonListView;
  }
}
