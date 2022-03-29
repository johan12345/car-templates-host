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

import androidx.annotation.Nullable;
import androidx.car.app.model.GridItem;
import com.android.car.libraries.apphost.template.view.model.SelectionGroup;

/** A host side wrapper for {@link GridItem}. */
public class GridItemWrapper {
  private final GridItem mGridItem;
  private final int mGridItemIndex;

  /**
   * The selection group this grid item belongs to, or {@code null} if the grid item does not belong
   * to one.
   *
   * <p>Selection groups are used to establish mutually-exclusive scopes of grid item selection.
   */
  @Nullable private final SelectionGroup mSelectionGroup;

  /** Returns a {@link Builder} that wraps a grid item with the provided index. */
  public static Builder wrap(
      GridItem gridItem, int gridItemIndex, @Nullable SelectionGroup selectionGroup) {
    Builder builder = new Builder(gridItem, gridItemIndex);
    if (selectionGroup != null) {
      builder.setSelectionGroup(selectionGroup);
    }
    return builder;
  }

  private GridItemWrapper(Builder builder) {
    mGridItem = builder.mGridItem;
    mGridItemIndex = builder.mGridItemIndex;
    mSelectionGroup = builder.mSelectionGroup;
  }

  @Override
  public String toString() {
    return "[" + mGridItem + ", group: " + mSelectionGroup + "]";
  }

  /** Returns the actual {@link GridItem} object that this instance is wrapping. */
  public GridItem getGridItem() {
    return mGridItem;
  }

  /** Returns the absolute index of the grid item in the flattened container list. */
  public int getGridItemIndex() {
    return mGridItemIndex;
  }

  @Nullable
  SelectionGroup getSelectionGroup() {
    return mSelectionGroup;
  }

  /** The builder class for {@link GridItemWrapper}. */
  public static class Builder {
    private final GridItem mGridItem;
    private final int mGridItemIndex;
    @Nullable private SelectionGroup mSelectionGroup;

    private Builder(GridItem gridItem, int gridItemIndex) {
      mGridItem = gridItem;
      mGridItemIndex = gridItemIndex;
    }

    /**
     * Sets the selection group this grid item belongs to, or {@code null} if the grid item does not
     * belong to one.
     */
    public Builder setSelectionGroup(@Nullable SelectionGroup selectionGroup) {
      mSelectionGroup = selectionGroup;
      return this;
    }

    /** Build the {@link GridItemWrapper}. */
    public GridItemWrapper build() {
      return new GridItemWrapper(this);
    }
  }
}
