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

import static java.lang.Math.min;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.constraints.ConstraintManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.templates.host.R;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** A grid adapter for {@link GridItemWrapper}s. */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class GridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements CarUiRecyclerView.ItemCap {

  private final Context mContext;
  private final int mItemsPerRow;
  private List<GridRowWrapper> mRowWrappers;
  private List<GridItemWrapper> mItemWrappers;

  private TemplateContext mTemplateContext;
  private int mMaxItemCount;

  static GridAdapter create(Context context, int itemsPerRow) {
    return new GridAdapter(context, itemsPerRow);
  }

  void setGridItems(TemplateContext templateContext, List<GridItemWrapper> gridItemWrappers) {
    mTemplateContext = templateContext;
    mMaxItemCount =
        mTemplateContext
            .getConstraintsProvider()
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID);
    mItemWrappers = gridItemWrappers;
    mRowWrappers = GridRowWrapper.create(gridItemWrappers, mItemsPerRow);

    notifyDataSetChanged();
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
    return new RecyclerView.ViewHolder(
        LayoutInflater.from(mContext).inflate(R.layout.grid_item_view, viewGroup, false)) {};
  }

  @Override
  public void onBindViewHolder(ViewHolder viewHolder, int index) {
    GridItemWrapper gridItemWrapper = mItemWrappers.get(index);
    GridRowWrapper gridRowWrapper = findGridRowWrapperForItemAt(index);
    ((GridItemView) viewHolder.itemView)
        .setGridItem(
            mTemplateContext,
            gridItemWrapper,
            gridRowWrapper.hasGridItemsWithTitle(),
            gridRowWrapper.hasGridItemsWithText());
  }

  @Override
  public int getItemCount() {
    if (mMaxItemCount == CarUiRecyclerView.ItemCap.UNLIMITED) {
      return mItemWrappers.size();
    } else {
      return min(mItemWrappers.size(), mMaxItemCount);
    }
  }

  @Override
  public void setMaxItems(int maxItems) {
    TemplateContext templateContext = mTemplateContext;
    if (templateContext == null) {
      return;
    }

    int gridMaxLength =
        templateContext
            .getConstraintsProvider()
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID);
    if (maxItems == CarUiRecyclerView.ItemCap.UNLIMITED) {
      mMaxItemCount = gridMaxLength;
    } else {
      mMaxItemCount = min(maxItems, gridMaxLength);
    }
  }

  @VisibleForTesting
  public List<GridRowWrapper> getRowWrappers() {
    return mRowWrappers;
  }

  @VisibleForTesting
  public List<GridItemWrapper> getItemWrappers() {
    return mItemWrappers;
  }

  /** Returns the {@link GridRowWrapper} associated with the item at given index. */
  private GridRowWrapper findGridRowWrapperForItemAt(int index) {
    int currentIndex = index;
    for (GridRowWrapper gridRowWrapper : mRowWrappers) {
      int rowItemsCount = gridRowWrapper.getGridRowItems().size();
      if (currentIndex < rowItemsCount) {
        return gridRowWrapper;
      }
      currentIndex -= rowItemsCount;
    }

    throw new IndexOutOfBoundsException(
        String.format("index = %d >= %d = count", index, getItemCount()));
  }

  private GridAdapter(Context context, int itemsPerRow) {
    mContext = context;
    mItemWrappers = ImmutableList.of();
    mRowWrappers = ImmutableList.of();
    mItemsPerRow = itemsPerRow;
  }
}
