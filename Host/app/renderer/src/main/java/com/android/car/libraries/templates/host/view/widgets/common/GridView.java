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

import android.content.Context;
import android.content.res.TypedArray;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.car.app.model.CarText;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.OnItemVisibilityChangedDelegate;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.libraries.templates.host.R;
import com.android.car.ui.recyclerview.CarUiLayoutStyle;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.recyclerview.CarUiRecyclerView.CarUiRecyclerViewLayout;
import com.android.car.ui.widget.CarUiTextView;
import java.util.Objects;

/** A view that can render a grid of {@link GridItem}s wrapped inside a {@link GridWrapper}. */
public class GridView extends FrameLayout {
  private final AdapterDataObserver mAdapterDataObserver =
      new AdapterDataObserver() {
        // call to update() not allowed on the given receiver.
        @SuppressWarnings("nullness:method.invocation")
        @Override
        public void onChanged() {
          super.onChanged();
          update();
        }
      };

  /** The number of items in a grid row. */
  private final int mItemsPerRow;

  private GridAdapter mGridRowAdapter;

  private ViewGroup mProgressContainer;
  private CarUiTextView mEmptyListTextView;
  private CarUiRecyclerView mRecyclerView;
  private RowVisibilityObserver mRowVisibilityObserver;
  private boolean mIsLoading;

  public GridView(Context context) {
    this(context, null);
  }

  public GridView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public GridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  @SuppressWarnings({"ResourceType", "nullness:method.invocation", "nullness:argument"})
  public GridView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateGridItemsPerRow,
    };

    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    mItemsPerRow = ta.getInteger(0, 0);
    ta.recycle();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    mProgressContainer = findViewById(R.id.progress_container);
    mEmptyListTextView = findViewById(R.id.list_no_items_text);
    mRecyclerView = findViewById(R.id.grid_paged_list_view);
    mRecyclerView.setLayoutStyle(
        new CarUiLayoutStyle() {
          @Override
          public int getSpanCount() {
            return mItemsPerRow;
          }

          @Override
          public int getLayoutType() {
            return CarUiRecyclerViewLayout.GRID;
          }

          @Override
          public int getOrientation() {
            return CarUiLayoutStyle.VERTICAL;
          }

          @Override
          public boolean getReverseLayout() {
            return false;
          }

          @Override
          public int getSize() {
            return CarUiRecyclerView.SIZE_LARGE;
          }
        });
    mRowVisibilityObserver = RowVisibilityObserver.create(Objects.requireNonNull(mRecyclerView));
    mGridRowAdapter = GridAdapter.create(getContext(), mItemsPerRow);
    mRecyclerView.setAdapter(mGridRowAdapter);
    mGridRowAdapter.registerAdapterDataObserver(mAdapterDataObserver);
    update();
  }

  void setGrid(TemplateContext templateContext, GridWrapper gridWrapper) {
    boolean isLoading = gridWrapper.isLoading();
    if (mIsLoading != isLoading) {
      // Trigger a visibility update if the loading state has changed.
      mIsLoading = isLoading;
      update();

      if (mIsLoading) {
        // Do not update the GridPagedListView/GridRowAdapter, as we want to maintain the
        // grid items list size during the loading phase until the new content is populated.
        return;
      }
    }

    CarText emptyListCarText = gridWrapper.getEmptyListText();
    CharSequence emptyText;
    if (emptyListCarText != null && !emptyListCarText.isEmpty()) {
      emptyText =
          CarTextUtils.toCharSequenceOrEmpty(templateContext, gridWrapper.getEmptyListText());
    } else {
      emptyText =
          templateContext.getText(
              templateContext.getHostResourceIds().getTemplateListNoItemsText());
    }
    mEmptyListTextView.setText(
        CarUiTextUtils.fromCharSequence(
            templateContext, emptyText, mEmptyListTextView.getMaxLines()));
    mRowVisibilityObserver.setOnItemVisibilityChangedListener(
        (startIndexInclusive, endIndexExclusive) -> {
          OnItemVisibilityChangedDelegate onItemVisibilityChangedDelegate =
              gridWrapper.getOnItemVisibilityChangedDelegate();
          if (onItemVisibilityChangedDelegate != null) {
            templateContext
                .getAppDispatcher()
                .dispatchItemVisibilityChanged(
                    onItemVisibilityChangedDelegate, startIndexInclusive, endIndexExclusive);
          }
        });

    mGridRowAdapter.setGridItems(templateContext, gridWrapper.getGridItemWrappers());

    if (!gridWrapper.isRefresh()) {
      mRecyclerView.scrollToPosition(0);
    }

    ViewUtils.logCarAppTelemetry(
        templateContext, UiAction.GRID_ITEM_LIST_SIZE, gridWrapper.getGridItemWrappers().size());
  }

  private void update() {
    boolean isLoading = mIsLoading;
    if (isLoading) {
      mProgressContainer.setVisibility(VISIBLE);

      // Mark the content views as invisible so that the size of the container remains the
      // same
      // while the progress bar is showing.
      mEmptyListTextView.setVisibility(INVISIBLE);
      mRecyclerView.setVisibility(INVISIBLE);
      return;
    }

    mProgressContainer.setVisibility(GONE);

    // If the grid item list is empty, hide it and display a message instead.
    boolean isEmpty = mGridRowAdapter.getItemCount() == 0;
    if (isEmpty) {
      mEmptyListTextView.setVisibility(VISIBLE);
      mRecyclerView.setVisibility(GONE);

      // When the empty list text view is displayed, show the focus ring by not clipping
      // children.
      setClipChildren(false);
      mEmptyListTextView.setFocusable(true);
    } else {
      mEmptyListTextView.setVisibility(GONE);
      mRecyclerView.setVisibility(VISIBLE);

      // When the grid view is displayed, clip its rows that get out of the view boundary.
      setClipChildren(true);
    }
  }
}
