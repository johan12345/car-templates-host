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

import androidx.car.app.model.CarText;
import androidx.car.app.model.GridItem;
import java.util.ArrayList;
import java.util.List;

/** A host side wrapper for a list of {@link GridItem}s that represent a row of the grid. */
public class GridRowWrapper {
  private final List<GridItemWrapper> mGridRowItems;
  private final int mGridRowIndex;
  private final int mMaxColsPerGridRow;

  private GridRowWrapper(
      List<GridItemWrapper> gridRowItems, int gridRowIndex, int maxColsPerGridRow) {
    mGridRowItems = gridRowItems;
    mGridRowIndex = gridRowIndex;
    mMaxColsPerGridRow = maxColsPerGridRow;
  }

  public List<GridItemWrapper> getGridRowItems() {
    return mGridRowItems;
  }

  public int getGridRowIndex() {
    return mGridRowIndex;
  }

  public int getMaxColsPerGridRow() {
    return mMaxColsPerGridRow;
  }

  /**
   * Creates a list of {@link GridRowWrapper}s from the provided list of {@link GridItemWrapper}s
   * based on the {@code numberOfColumns}.
   */
  public static List<GridRowWrapper> create(
      List<GridItemWrapper> gridItemWrappers, int numberOfColumns) {
    List<GridRowWrapper> gridRowWrappers = new ArrayList<>();

    int itemCount = gridItemWrappers.size();
    int gridRowIndex = 0;
    int beginIndex = 0;
    while (beginIndex < itemCount) {
      gridRowWrappers.add(
          new GridRowWrapper(
              gridItemWrappers.subList(beginIndex, min(itemCount, beginIndex + numberOfColumns)),
              gridRowIndex,
              numberOfColumns));
      gridRowIndex++;
      beginIndex += numberOfColumns;
    }

    return gridRowWrappers;
  }

  /**
   * Returns {@code true} if any of the {@link GridItem}s consisting this grid row has a title set.
   */
  public boolean hasGridItemsWithTitle() {
    for (GridItemWrapper gridItemWrapper : mGridRowItems) {
      CarText carText = gridItemWrapper.getGridItem().getTitle();
      if (carText != null && !carText.isEmpty()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns {@code true} if any of the {@link GridItem}s consisting this grid row has a secondary
   * line of text set.
   */
  public boolean hasGridItemsWithText() {
    for (GridItemWrapper gridItemWrapper : mGridRowItems) {
      CarText carText = gridItemWrapper.getGridItem().getText();
      if (carText != null && !carText.isEmpty()) {
        return true;
      }
    }

    return false;
  }
}
