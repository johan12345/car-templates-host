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
package com.android.car.libraries.templates.host.view.widgets.common;

import android.view.View;
import androidx.annotation.Nullable;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarText;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.toolbar.ToolbarController;

/** A view that displays the header for the templates. */
public class HeaderView extends AbstractHeaderView {

  private HeaderView(TemplateContext templateContext, ToolbarController toolbarController) {
    super(templateContext, toolbarController);
  }

  /**
   * Set or clear the content of the view.
   *
   * <p>If the {@code title} is {@code null} then the view is hidden.
   */
  public void setContent(
      TemplateContext templateContext, @Nullable CarText title, @Nullable Action action) {
    mToolbarController.setTitle(CarTextUtils.toCharSequenceOrEmpty(templateContext, title));
    setAction(action);
  }

  /** Installs a {@link HeaderView} around the given container view. */
  @SuppressWarnings("nullness:argument") // InsetsChangedListener is nullable.
  public static HeaderView install(TemplateContext mTemplateContext, View container) {
    ToolbarController toolbarController = CarUi.installBaseLayoutAround(container, null, true);
    if (toolbarController == null) {
      throw new NullPointerException("Toolbar Controller could not be created.");
    }
    return new HeaderView(mTemplateContext, toolbarController);
  }
}
