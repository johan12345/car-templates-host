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

import android.annotation.SuppressLint;
import com.android.car.libraries.apphost.view.AbstractTemplatePresenter;
import com.android.car.libraries.templates.host.view.TemplateView;

/** Test helper for {@link TemplateView}. */
public final class TemplateViewHelper {

  private static final int LAYOUT_WIDTH = 400;
  private static final int LAYOUT_HEIGHT = 600;

  /** Force a measure and layout on the given {@link TemplateView}. */
  @SuppressLint("RestrictedApi")
  public static void measureAndLayout(TemplateView templateView) {
    templateView.measure(LAYOUT_WIDTH, LAYOUT_HEIGHT);
    templateView.layout(0, 0, LAYOUT_WIDTH, LAYOUT_HEIGHT);

    // Robolectric creates views without giving it size, causing the view to fail to take input
    // focus.
    // Set the content view size and restore focus.
    AbstractTemplatePresenter presenter =
        (AbstractTemplatePresenter) templateView.getCurrentPresenter();
    if (presenter != null) {
      presenter.restoreFocus();
    }
  }

  private TemplateViewHelper() {}
}
