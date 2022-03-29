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

import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.android.car.libraries.apphost.template.view.model.RowListWrapper;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.ContentView;
import com.android.car.libraries.templates.host.view.widgets.common.RowAdapter;
import com.android.car.libraries.templates.host.view.widgets.common.RowHolder;
import com.android.car.libraries.templates.host.view.widgets.common.RowListView;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import java.util.List;

/** Test helper for {@link ContentView} that has the {@link RowListWrapper} content set. */
public class RowListContentViewHelper {
  private static final int LAYOUT_WIDTH = 400;
  private static final int LAYOUT_HEIGHT = 600;

  private final ContentView mContentView;

  public RowListContentViewHelper(ContentView contentView) {
    mContentView = contentView;
  }

  /** Force a measure and layout on the given {@link ContentView}. */
  public void measureAndLayout() {
    measureAndLayout(LAYOUT_WIDTH, LAYOUT_HEIGHT);
  }

  /** Force a measure and layout on the given {@link ContentView} with given width and height. */
  public void measureAndLayout(int width, int height) {
    RowListView pagedListView = getListView();
    if (pagedListView != null) {
      pagedListView.measure(
          MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
      pagedListView.layout(0, 0, width, height);
    }
  }

  /** Returns the {@link RowListView} from the content view. */
  @Nullable
  public RowListView getListView() {
    RowListView listView = getRowListView();
    if (listView == null) {
      return null;
    }

    CarUiRecyclerView recyclerView = listView.findViewById(R.id.list_view);
    return (RowListView) recyclerView.getParent();
  }

  /** Returns a {@link List} of the {@link RowHolder}s in the content view. */
  @Nullable
  public List<RowHolder> getRowHolders() {
    RowListView listView = getListView();
    if (listView != null) {
      RowAdapter adapter = listView.getAdapter();
      if (adapter != null) {
        return adapter.getRowHolders();
      }
    }
    return null;
  }

  /** Returns the row view for a given index from the content view. */
  @Nullable
  public View getRowView(int index) {
    return getListItemView(index, View.class);
  }

  /** Returns the text of the given section header from the content view. */
  @Nullable
  public String getSectionHeaderText(int index) {
    View sectionHeaderView = getSectionHeaderView(index);
    if (sectionHeaderView == null) {
      return null;
    }
    TextView view = sectionHeaderView.findViewById(R.id.row_section_header);
    return view != null ? view.getText().toString() : null;
  }

  /** Returns the {@link TextView} of the given section header from the content view. */
  @Nullable
  public View getSectionHeaderView(int index) {
    return getListItemView(index, View.class);
  }

  /** Returns a {@link RowViewHelper} for the given row index from the content view. */
  @Nullable
  public RowViewHelper getRowViewHelper(int index) {
    View rowView = getRowView(index);
    if (rowView == null) {
      return null;
    }
    return new RowViewHelper(rowView);
  }

  @Nullable
  private <T> T getListItemView(int index, Class<T> clazz) {
    RowListView listView = getListView();
    if (listView != null) {
      return clazz.cast(listView.getRecyclerView().getRecyclerViewChildAt(index));
    }
    return null;
  }

  @Nullable
  private RowListView getRowListView() {
    ViewGroup container = mContentView.findViewById(R.id.container);
    if (container == null) {
      return null;
    }
    return (RowListView) container.getChildAt(0);
  }
}
