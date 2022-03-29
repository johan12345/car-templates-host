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

import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.GridItemView;
import com.android.car.ui.widget.CarUiTextView;

/** Test helper for {@link GridItemView}. */
public class GridItemViewHelper {
  private final GridItemView mGridItemView;

  public GridItemViewHelper(GridItemView gridItemView) {
    mGridItemView = gridItemView;
  }

  /** Returns the title as a {@link String} for the {@link GridItemView}. */
  @Nullable
  public String getTitle() {
    return getTextById(R.id.grid_item_title);
  }

  /** Returns the title as the raw {@link CharSequence} for the {@link GridItemView}. */
  @Nullable
  public CharSequence getText() {
    return getTextById(R.id.grid_item_text);
  }

  @Nullable
  private String getTextById(int id) {
    CarUiTextView carUiTextView = mGridItemView.findViewById(id);
    if (carUiTextView.getVisibility() == VISIBLE) {
      CharSequence title = carUiTextView.getText();
      if (title != null) {
        return title.toString();
      }
    }

    return null;
  }

  /** Returns the {@link ImageView} for the {@link GridItemView}. */
  @Nullable
  public ImageView getImage() {
    ImageView imageView = mGridItemView.findViewById(R.id.grid_item_image);
    if (imageView.getVisibility() == VISIBLE) {
      return imageView;
    }
    return null;
  }

  /** Returns the {@link ProgressBar} for the {@link GridItemView}. */
  @Nullable
  public ProgressBar getLoadingView() {
    ProgressBar loadingView = mGridItemView.findViewById(R.id.grid_item_progress_bar);
    if (loadingView.getVisibility() == VISIBLE) {
      return loadingView;
    }
    return null;
  }
}
