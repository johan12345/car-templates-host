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

import static com.android.car.libraries.apphost.template.view.model.RowListWrapper.LIST_FLAGS_HIDE_ROW_DIVIDERS;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.template.view.model.RowListWrapper;
import com.android.car.libraries.templates.host.R;

/** A view that displays content such as a list, a pane, or an error screen. */
public class ContentView extends LinearLayout {
  private ViewGroup mViewGroup;

  public ContentView(Context context) {
    this(context, null);
  }

  public ContentView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ContentView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  @SuppressWarnings({"ResourceType", "method.invocation.invalid", "argument.type.incompatible"})
  public ContentView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mViewGroup = findViewById(R.id.container);
  }

  /** Sets a {@link GridWrapper} as the content for this view. */
  public void setGridContent(TemplateContext templateContext, GridWrapper gridWrapper) {
    View view = mViewGroup.getChildCount() > 0 ? mViewGroup.getChildAt(0) : null;
    if (view != null) {
      if (!(view instanceof GridView)) {
        removeView(view);
        view = null;
      }
    }

    if (view == null) {
      view = LayoutInflater.from(getContext()).inflate(R.layout.grid_view, mViewGroup, false);
      mViewGroup.addView(view);
    }

    ((GridView) view).setGrid(templateContext, gridWrapper);
  }

  /** Sets a {@link RowListWrapper} as the content for this view. */
  public void setRowListContent(TemplateContext templateContext, RowListWrapper rowList) {
    View view = mViewGroup.getChildCount() > 0 ? mViewGroup.getChildAt(0) : null;
    if (view != null) {
      if (!(view instanceof RowListView)) {
        removeView(view);
        view = null;
      }
    }

    if (view == null) {
      boolean hasRowDividers = (rowList.getListFlags() & LIST_FLAGS_HIDE_ROW_DIVIDERS) == 0;
      int layout =
          rowList.isHalfList()
              ? R.layout.half_list_view
              : hasRowDividers ? R.layout.full_list_view : R.layout.full_list_no_divider_view;
      view = LayoutInflater.from(getContext()).inflate(layout, mViewGroup, false);
      mViewGroup.addView(view);
    }

    ((RowListView) view).setRowList(templateContext, rowList);
  }
}
