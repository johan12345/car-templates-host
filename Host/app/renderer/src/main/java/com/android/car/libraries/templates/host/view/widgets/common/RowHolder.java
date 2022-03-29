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

import static com.android.car.libraries.apphost.template.view.model.RowWrapper.ROW_FLAG_SECTION_HEADER;
import static com.android.car.libraries.apphost.template.view.model.RowWrapper.ROW_FLAG_SHOW_DIVIDERS;
import static com.android.car.libraries.templates.host.view.widgets.common.ActionListUtils.isActionList;

import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.RowConstraints;
import com.android.car.libraries.apphost.distraction.constraints.RowListConstraints;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.template.view.model.RowWrapper;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** A holder of a row instance with its associated metadata. */
public class RowHolder {
  /** Listener of events related to a {@link RowHolder} instance. */
  public interface RowListener {
    /**
     * Notifies that a row has been selected.
     *
     * @param index index of the row in the list it belongs to.
     */
    void onRowClicked(int index);

    /**
     * Notifies that a row's check state has been changed.
     *
     * @param index index of the row in the list it belongs to.
     */
    void onCheckedChange(int index);

    /** Notifies that a row's focus has changed. */
    void onRowFocusChanged(int index, boolean hasFocus);
  }

  private final RowWrapper mRow;

  @Override
  public String toString() {
    return mRow.toString();
  }

  /** Creates a {@link RowHolder} from a row object. */
  public static RowHolder create(RowWrapper row) {
    return new RowHolder(row);
  }

  /** Returns a list of {@link RowHolder} instances from the given rows. */
  static ImmutableList<RowHolder> createHolders(
      TemplateContext templateContext, List<RowWrapper> rows, RowListConstraints constraints) {
    if (rows.isEmpty()) {
      return ImmutableList.of();
    }

    ImmutableList.Builder<RowHolder> listBuilder = ImmutableList.builder();

    int maxRowCount =
        templateContext.getConstraintsProvider().getContentLimit(constraints.getListContentType());
    int nonHeaderRowCount = 0;

    // Cache the last seen header row, and only add it if there is a non-header row underneath
    // it.
    // We don't support consecutive header rows.
    RowWrapper lastHeaderRow = null;
    for (int i = 0; i < rows.size(); ++i) {
      RowWrapper rowWrapper = rows.get(i);
      Object rowObj = rowWrapper.getRow();

      if (isActionList(rowObj)) {
        // Special case for an action list which is only for the first row in PaneTemplate.
        listBuilder.add(RowHolder.create(rowWrapper));
      } else {
        // Ensure we only count the actual rows against the row limit.
        boolean isSectionHeader = (rowWrapper.getRowFlags() & ROW_FLAG_SECTION_HEADER) != 0;
        if (!isSectionHeader) {
          nonHeaderRowCount++;
          if (nonHeaderRowCount > maxRowCount) {
            L.w(
                LogTags.TEMPLATE,
                "Row count exceeds the supported maximum of %d, will drop the"
                    + " remaining excess rows",
                maxRowCount);
            break;
          }

          if (lastHeaderRow != null) {
            listBuilder.add(RowHolder.create(lastHeaderRow));
            lastHeaderRow = null;
          }
          listBuilder.add(RowHolder.create(rowWrapper));
        } else {
          if (lastHeaderRow != null) {
            L.w(
                LogTags.TEMPLATE,
                "Consecutive header rows detected and is not supported, only the"
                    + " last one will be used");
          }

          lastHeaderRow = rowWrapper;
        }
      }
    }

    return listBuilder.build();
  }

  public RowConstraints getConstraints() {
    return mRow.getRowConstraints();
  }

  public RowWrapper getRowWrapper() {
    return mRow;
  }

  boolean isSectionHeader() {
    return 0 != (mRow.getRowFlags() & ROW_FLAG_SECTION_HEADER);
  }

  boolean showDividers() {
    return 0 != (mRow.getRowFlags() & ROW_FLAG_SHOW_DIVIDERS);
  }

  private RowHolder(RowWrapper row) {
    mRow = row;
  }
}
