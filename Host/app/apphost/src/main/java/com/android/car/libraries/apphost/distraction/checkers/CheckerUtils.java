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
package com.android.car.libraries.apphost.distraction.checkers;

import androidx.annotation.Nullable;
import androidx.car.app.model.CarText;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.Item;
import androidx.car.app.model.Row;
import androidx.car.app.model.Toggle;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import java.util.List;
import java.util.Objects;

/** Shared util methods for handling different template checking logic. */
public class CheckerUtils {
  /** Returns whether the sizes and string contents of the two lists of items are equal. */
  public static <T extends Item> boolean itemsHaveSameContent(
      List<T> itemList1, List<T> itemList2) {
    if (itemList1.size() != itemList2.size()) {
      L.d(
          LogTags.DISTRACTION,
          "REFRESH check failed. Different item list sizes. Old: %d. New: %d",
          itemList1.size(),
          itemList2.size());
      return false;
    }

    for (int i = 0; i < itemList1.size(); i++) {
      T itemObj1 = itemList1.get(i);
      T itemObj2 = itemList2.get(i);

      if (itemObj1.getClass() != itemObj2.getClass()) {
        L.d(
            LogTags.DISTRACTION,
            "REFRESH check failed. Different item types at index %d. Old: %s. New: %s",
            i,
            itemObj1.getClass(),
            itemObj2.getClass());
        return false;
      }

      if (itemObj1 instanceof Row) {
        if (!rowsHaveSameContent((Row) itemObj1, (Row) itemObj2, i)) {
          return false;
        }
      } else if (itemObj1 instanceof GridItem) {
        if (!gridItemsHaveSameContent((GridItem) itemObj1, (GridItem) itemObj2, i)) {
          return false;
        }
      }
    }

    return true;
  }

  /** Returns whether the string contents of the two rows are equal. */
  private static boolean rowsHaveSameContent(Row row1, Row row2, int index) {
    // Special case for rows with toggles - if the toggle state has changed, then text updates
    // are allowed.
    if (toggleStateHasChanged(row1.getToggle(), row2.getToggle())) {
      return true;
    }

    if (!carTextsHasSameString(row1.getTitle(), row2.getTitle())) {
      L.d(
          LogTags.DISTRACTION,
          "REFRESH check failed. Different row titles at index %d. Old: %s. New: %s",
          index,
          row1.getTitle(),
          row2.getTitle());
      return false;
    }

    return true;
  }

  /** Returns whether the string contents of the two grid items are equal. */
  private static boolean gridItemsHaveSameContent(
      GridItem gridItem1, GridItem gridItem2, int index) {
    // We only check the item's title - changes in text and image are considered a refresh.
    if (!carTextsHasSameString(gridItem1.getTitle(), gridItem2.getTitle())) {
      L.d(
          LogTags.DISTRACTION,
          "REFRESH check failed. Different grid item titles at index %d. Old: %s. New:" + " %s",
          index,
          gridItem1.getTitle(),
          gridItem2.getTitle());
      return false;
    }

    return true;
  }

  /**
   * Returns whether the strings of the two {@link CarText}s are the same.
   *
   * <p>Spans that are attached to the strings are ignored from the comparison.
   */
  private static boolean carTextsHasSameString(
      @Nullable CarText carText1, @Nullable CarText carText2) {
    // If both carText1 and carText2 are null, return true. If only one of them is null, return
    // false.
    if (carText1 == null || carText2 == null) {
      return carText1 == null && carText2 == null;
    }

    return Objects.equals(carText1.toString(), carText2.toString());
  }

  private static boolean toggleStateHasChanged(@Nullable Toggle toggle1, @Nullable Toggle toggle2) {
    return toggle1 != null && toggle2 != null && toggle1.isChecked() != toggle2.isChecked();
  }

  private CheckerUtils() {}
}
