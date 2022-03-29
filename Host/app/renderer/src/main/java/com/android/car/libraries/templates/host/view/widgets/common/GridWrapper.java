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
import androidx.car.app.model.CarText;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.Item;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.OnItemVisibilityChangedDelegate;
import androidx.car.app.model.OnSelectedDelegate;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.template.view.model.SelectionGroup;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** A host side wrapper for {@link ItemList} that's part of a {@link GridTemplate}. */
public class GridWrapper {
  private final boolean mIsLoading;
  private final boolean mIsRefresh;
  @Nullable private final CarText mEmptyListText;
  @Nullable private final OnItemVisibilityChangedDelegate mOnItemVisibilityChangedDelegate;
  private final List<GridItemWrapper> mGridItemWrappers;

  /** Converts an {@link ItemList} into a {@link GridWrapper.Builder}. */
  public static Builder wrap(@Nullable ItemList itemList) {
    if (itemList == null) {
      return new Builder();
    }

    List<Item> gridItems = itemList.getItems();
    Builder builder =
        new Builder()
            .setGridItems(gridItems)
            .setEmptyListText(itemList.getNoItemsMessage())
            .setOnItemVisibilityChangedDelegate(itemList.getOnItemVisibilityChangedDelegate());

    OnSelectedDelegate onSelectedDelegate = itemList.getOnSelectedDelegate();
    if (onSelectedDelegate != null) {
      builder.setSelectionGroup(
          SelectionGroup.create(
              0, gridItems.size() - 1, itemList.getSelectedIndex(), onSelectedDelegate));
    }

    return builder;
  }

  private GridWrapper(Builder builder) {
    mIsLoading = builder.mIsLoading;
    mIsRefresh = builder.mIsRefresh;
    mEmptyListText = builder.mEmptyListText;
    mOnItemVisibilityChangedDelegate = builder.mOnItemVisibilityChangedDelegate;
    mGridItemWrappers = buildGridItemWrappers(builder.mGridItems, builder.mSelectionGroup);
  }

  @Nullable
  public OnItemVisibilityChangedDelegate getOnItemVisibilityChangedDelegate() {
    return mOnItemVisibilityChangedDelegate;
  }

  public boolean isEmpty() {
    return mGridItemWrappers.isEmpty();
  }

  @Nullable
  CarText getEmptyListText() {
    return mEmptyListText;
  }

  List<GridItemWrapper> getGridItemWrappers() {
    return mGridItemWrappers;
  }

  boolean isLoading() {
    return mIsLoading;
  }

  boolean isRefresh() {
    return mIsRefresh;
  }

  /** Builds the {@link GridItemWrapper}s for a given list. */
  private static ImmutableList<GridItemWrapper> buildGridItemWrappers(
      @Nullable List<Item> gridItems, @Nullable SelectionGroup selectionGroup) {
    if (gridItems == null || gridItems.isEmpty()) {
      return ImmutableList.of();
    }

    int beginIndex = 0;
    ImmutableList.Builder<GridItemWrapper> gridItemWrapperBuilder = new ImmutableList.Builder<>();
    for (Item item : gridItems) {
      if (!(item instanceof GridItem)) {
        L.w(LogTags.TEMPLATE, "Item in list is not a GridItem, dropping item");
      }
      gridItemWrapperBuilder.add(
          GridItemWrapper.wrap((GridItem) item, beginIndex, selectionGroup).build());
      beginIndex++;
    }

    return gridItemWrapperBuilder.build();
  }

  /** The builder class for {@link GridWrapper}. */
  public static class Builder {
    @Nullable private List<Item> mGridItems;
    @Nullable private OnItemVisibilityChangedDelegate mOnItemVisibilityChangedDelegate;
    private boolean mIsLoading;
    private boolean mIsRefresh;
    @Nullable private CarText mEmptyListText;
    @Nullable private SelectionGroup mSelectionGroup;

    private Builder() {
      mGridItems = null;
    }

    /** Sets the grid items in the {@link Builder} */
    public Builder setGridItems(List<Item> gridItems) {
      mGridItems = gridItems;
      return this;
    }

    /**
     * Sets the {@link OnItemVisibilityChangedDelegate} which can receive callbacks when the
     * visibility of a items changes.
     *
     * <p>If set to {@code null} it will clear the delegate and no callbacks will be received.
     */
    public Builder setOnItemVisibilityChangedDelegate(
        @Nullable OnItemVisibilityChangedDelegate delegate) {
      mOnItemVisibilityChangedDelegate = delegate;
      return this;
    }

    /**
     * Sets whether the list is loading.
     *
     * <p>If set to {@code true}, the UI shows a loading indicator and ignore any grid items added
     * to the list. If set to {@code false}, the UI shows the actual grid item contents.
     */
    public Builder setIsLoading(boolean isLoading) {
      mIsLoading = isLoading;
      return this;
    }

    /**
     * Sets whether the grid is a refresh of the existing grid.
     *
     * <p>If set to {@code true}, the UI will not scroll to top, otherwise it will.
     */
    public Builder setIsRefresh(boolean isRefresh) {
      mIsRefresh = isRefresh;
      return this;
    }

    /** Sets the text to be displayed when there are no items in the list. */
    public Builder setEmptyListText(@Nullable CarText emptyListText) {
      mEmptyListText = emptyListText;
      return this;
    }

    /**
     * Sets the selection group these grid items belong to, or {@code null} if the grid items do not
     * belong to one.
     */
    public Builder setSelectionGroup(@Nullable SelectionGroup selectionGroup) {
      mSelectionGroup = selectionGroup;
      return this;
    }

    /** Builds the {@link GridWrapper}. */
    public GridWrapper build() {
      return new GridWrapper(this);
    }
  }
}
