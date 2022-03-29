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
package com.android.car.libraries.templates.host.view.presenters.common;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.StyleableRes;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.TemplateWrapper;
import com.android.car.libraries.apphost.common.StatusBarManager.StatusBarState;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.template.view.model.RowListWrapper;
import com.android.car.libraries.apphost.template.view.model.RowListWrapperTemplate;
import com.android.car.libraries.apphost.view.AbstractTemplatePresenter;
import com.android.car.libraries.apphost.view.TemplatePresenter;
import com.android.car.libraries.apphost.view.common.ActionButtonListParams;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.ActionButtonListView;
import com.android.car.libraries.templates.host.view.widgets.common.ContentView;
import com.android.car.libraries.templates.host.view.widgets.common.HeaderView;
import java.util.List;

/** A {@link TemplatePresenter} for {@link RowListWrapperTemplate} instances. */
public class RowListWrapperTemplatePresenter extends AbstractTemplatePresenter {
  // TODO(b/183643108): Use a common value for this constant
  private static final int MAX_ALLOWED_ACTIONS = 2;

  private final ViewGroup mRootView;
  private final HeaderView mHeaderView;
  private final ContentView mContentView;
  private final ActionButtonListView mStickyActionButtonListView;
  private final ActionButtonListParams mActionButtonListParams;

  /** Create a RowListWrapperTemplatePresenter */
  public static RowListWrapperTemplatePresenter create(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    RowListWrapperTemplatePresenter presenter =
        new RowListWrapperTemplatePresenter(templateContext, templateWrapper);
    presenter.update();
    return presenter;
  }

  @Override
  public View getView() {
    return mRootView;
  }

  @Override
  public void onTemplateChanged() {
    update();
  }

  @Override
  protected View getDefaultFocusedView() {
    if (mContentView.getVisibility() == VISIBLE) {
      return mContentView;
    }
    return super.getDefaultFocusedView();
  }

  private void update() {
    RowListWrapperTemplate template = (RowListWrapperTemplate) getTemplate();
    ActionStrip actionStrip = template.getActionStrip();
    RowListWrapper list = template.getList();

    List<Action> actionList = template.getActionList();
    if (actionList != null && !actionList.isEmpty()) {
      mStickyActionButtonListView.setVisibility(VISIBLE);
      mStickyActionButtonListView.setActionList(
          getTemplateContext(), actionList, mActionButtonListParams);
    } else {
      mStickyActionButtonListView.setVisibility(GONE);
    }

    mHeaderView.setActionStrip(actionStrip, template.getActionsConstraints());
    mHeaderView.setContent(getTemplateContext(), template.getTitle(), template.getHeaderAction());

    mContentView.setRowListContent(getTemplateContext(), list);
  }

  @SuppressLint("InflateParams")
  @SuppressWarnings({"methodref.receiver.bound.invalid"})
  private RowListWrapperTemplatePresenter(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    super(templateContext, templateWrapper, StatusBarState.LIGHT);

    mRootView =
        (ViewGroup)
            LayoutInflater.from(templateContext)
                .inflate(R.layout.row_list_wrapper_template_layout, null);
    mContentView = mRootView.findViewById(R.id.content_view);
    View contentContainer = mRootView.findViewById(R.id.content_container);
    mHeaderView = HeaderView.install(templateContext, contentContainer);

    @StyleableRes final int[] themeAttrs = {R.attr.templatePlainContentBackgroundColor};

    TypedArray ta = templateContext.obtainStyledAttributes(themeAttrs);
    @ColorInt int surroundingColor = ta.getColor(0, 0);
    ta.recycle();

    mActionButtonListParams =
        ActionButtonListParams.builder()
            .setMaxActions(MAX_ALLOWED_ACTIONS)
            .setOemReorderingAllowed(true)
            .setOemColorOverrideAllowed(true)
            .setSurroundingColor(surroundingColor)
            .build();

    mStickyActionButtonListView = mRootView.requireViewById(R.id.sticky_action_button_list_view);
  }
}
