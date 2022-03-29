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

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.ContentView;
import com.android.car.libraries.templates.host.view.widgets.common.GridAdapter;
import com.android.car.libraries.templates.host.view.widgets.common.GridItemView;
import com.android.car.libraries.templates.host.view.widgets.common.GridRowWrapper;
import com.android.car.libraries.templates.host.view.widgets.common.GridView;
import com.android.car.libraries.templates.host.view.widgets.common.GridWrapper;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Test helper for {@link ContentView} that has the {@link GridWrapper} content set. */
public class GridContentViewHelper {
  private static final int LAYOUT_WIDTH = 400;
  private static final int LAYOUT_HEIGHT = 600;

  private final ContentView mContentView;

  public GridContentViewHelper(ContentView contentView) {
    mContentView = contentView;
  }

  /** Force a measure and layout for the content view. */
  public void measureAndLayout() {
    CarUiRecyclerView pagedListView = getRecyclerView();
    if (pagedListView != null) {
      pagedListView
          .getView()
          .measure(
              MeasureSpec.makeMeasureSpec(LAYOUT_WIDTH, MeasureSpec.EXACTLY),
              MeasureSpec.makeMeasureSpec(LAYOUT_HEIGHT, MeasureSpec.EXACTLY));
      pagedListView.getView().layout(0, 0, LAYOUT_WIDTH, LAYOUT_HEIGHT);
    }
  }

  /** Returns the {@link RecyclerView} from the content view. */
  @Nullable
  public CarUiRecyclerView getRecyclerView() {
    GridView gridView = getGridView();
    if (gridView == null) {
      return null;
    }
    return (CarUiRecyclerView) gridView.findViewById(R.id.grid_paged_list_view);
  }

  /** Returns a {@link List} of {@link GridRowWrapper}s from the content view. */
  @Nullable
  public List<GridRowWrapper> getGridRowWrappers() {
    CarUiRecyclerView listView = getRecyclerView();
    if (listView != null) {
      GridAdapter adapter = (GridAdapter) listView.getAdapter();
      if (adapter != null) {
        return adapter.getRowWrappers();
      }
    }
    return null;
  }

  /** Returns a specified {@link GridItemView} from the content view. */
  @Nullable
  public GridItemView getGridItemView(int index) {
    CarUiRecyclerView listView = getRecyclerView();
    if (listView == null) {
      return null;
    }

    return (GridItemView) listView.getRecyclerViewChildAt(index);
  }

  /** Returns a specified {@link GridItemViewHelper} from the content view. */
  @Nullable
  public GridItemViewHelper getGridItemViewHelper(int index) {
    GridItemView gridItemView = getGridItemView(index);
    if (gridItemView == null) {
      return null;
    }
    return new GridItemViewHelper(gridItemView);
  }

  @Nullable
  private GridView getGridView() {
    ViewGroup container = mContentView.findViewById(R.id.container);
    if (container == null) {
      return null;
    }
    return (GridView) container.getChildAt(0);
  }
}
