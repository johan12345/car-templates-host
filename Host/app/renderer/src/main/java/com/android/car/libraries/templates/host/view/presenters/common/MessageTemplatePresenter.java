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

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.annotation.StyleableRes;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.TemplateWrapper;
import com.android.car.libraries.apphost.common.StatusBarManager.StatusBarState;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.view.AbstractTemplatePresenter;
import com.android.car.libraries.apphost.view.common.ActionButtonListParams;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.libraries.apphost.view.common.ImageUtils;
import com.android.car.libraries.apphost.view.common.ImageViewParams;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.internal.CommonUtils;
import com.android.car.libraries.templates.host.view.widgets.common.ActionButtonListView;
import com.android.car.libraries.templates.host.view.widgets.common.ActionButtonListView.Gravity;
import com.android.car.libraries.templates.host.view.widgets.common.CarUiTextUtils;
import com.android.car.libraries.templates.host.view.widgets.common.HeaderView;
import com.android.car.libraries.templates.host.view.widgets.common.ViewUtils;
import com.android.car.ui.widget.CarUiTextView;
import java.util.List;

/** An {@link AbstractTemplatePresenter} that shows an alert message, some actions, and an icon. */
public class MessageTemplatePresenter extends AbstractTemplatePresenter {
  // TODO(b/183643108): Use a common MAX_ALLOWED_ACTIONS between AAOS and AAP
  private static final int MAX_ALLOWED_ACTIONS = 2;

  private final ViewGroup mRootView;
  private final HeaderView mHeaderView;
  private final ViewGroup mProgressContainer;
  private final CarUiTextView mMessageTextView;
  private final ViewGroup mStackTraceContainer;
  private final CarUiTextView mStackTraceView;
  private final ImageView mIconView;
  private final ActionButtonListView mActionListView;
  private final ImageViewParams mImageViewParams;
  private final ActionButtonListParams mActionButtonListParams;
  private final boolean mIsDebugEnabled;
  private final ViewGroup mMessageContainer;

  /** Create a MessageTemplatePresenter */
  public static MessageTemplatePresenter create(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    MessageTemplatePresenter presenter =
        new MessageTemplatePresenter(templateContext, templateWrapper);
    presenter.update();
    return presenter;
  }

  @Override
  protected View getDefaultFocusedView() {
    if (mActionListView.getVisibility() == VISIBLE) {
      return mActionListView;
    }
    return super.getDefaultFocusedView();
  }

  @Override
  public void onTemplateChanged() {
    update();
  }

  @Override
  public View getView() {
    return mRootView;
  }

  /** Updates the view with current values in the {@link MessageTemplate}. */
  private void update() {
    TemplateContext templateContext = getTemplateContext();
    MessageTemplate template = (MessageTemplate) getTemplate();

    // If we have a title or a header action, show the header; hide it otherwise.
    CarText title = template.getTitle();
    Action headerAction = template.getHeaderAction();
    if (!CarText.isNullOrEmpty(title) || headerAction != null) {
      mHeaderView.setContent(getTemplateContext(), title, headerAction);
    } else {
      mHeaderView.setContent(getTemplateContext(), null, null);
    }

    mHeaderView.setActionStrip(
        template.getActionStrip(), ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE);

    // Show a message if we have it, hide it otherwise.
    CarText message = template.getMessage();
    if (!CarText.isNullOrEmpty(message)) {
      mMessageTextView.setText(
          CarUiTextUtils.fromCarText(templateContext, message, mMessageTextView.getMaxLines()));
      mMessageTextView.setVisibility(VISIBLE);

      // Allow focus on the message view if there are no actions available.
      mMessageTextView.setFocusable(template.getActions().isEmpty());
    } else {
      mMessageTextView.setVisibility(GONE);
    }

    // The icon and progress indicator are mutually exclusive, next we choose which one to
    // display.
    boolean isLoading = template.isLoading();
    if (isLoading) {
      // If in loading state, show the progress container and hide the icon.
      mProgressContainer.setVisibility(VISIBLE);
      mIconView.setVisibility(GONE);
    } else {
      // Not in loading state: hide the progress container and show the icon, if we have one.
      mProgressContainer.setVisibility(GONE);

      CarIcon icon = template.getIcon();
      boolean showIcon = icon != null;
      if (showIcon) {
        showIcon = ImageUtils.setImageSrc(templateContext, icon, mIconView, mImageViewParams);
      }
      mIconView.setVisibility(showIcon ? VISIBLE : GONE);
    }

    // Show the action list if we have it, hide it otherwise.
    List<Action> actionList = template.getActions();
    if (!actionList.isEmpty()) {
      mActionListView.setActionList(getTemplateContext(), actionList, mActionButtonListParams);
      mActionListView.setVisibility(VISIBLE);
    } else {
      mActionListView.setVisibility(GONE);
    }

    // If we can show the debug information, add a button to the action strip that toggles it
    // on and off when tapped.
    CarText debugMessage = template.getDebugMessage();
    if (mIsDebugEnabled && !CarText.isNullOrEmpty(debugMessage)) {
      mStackTraceView.setText(CarTextUtils.toCharSequenceOrEmpty(templateContext, debugMessage));
      mStackTraceContainer.setVisibility(VISIBLE);
      addDebugToggle(templateContext);
    } else {
      mStackTraceContainer.setVisibility(GONE);
    }
  }

  private void addDebugToggle(TemplateContext templateContext) {
    Drawable icon = templateContext.getDrawable(R.drawable.ic_bug_report_grey600_24dp);
    mHeaderView.addToggle(icon, this::showTraceView);
  }

  private void showTraceView(boolean show) {
    mStackTraceContainer.setVisibility(show ? VISIBLE : GONE);
    mMessageContainer.setVisibility(show ? GONE : VISIBLE);
  }

  @SuppressWarnings("method.invocation.invalid")
  private MessageTemplatePresenter(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    super(templateContext, templateWrapper, StatusBarState.LIGHT);

    mRootView =
        (ViewGroup)
            LayoutInflater.from(templateContext).inflate(R.layout.message_template_layout, null);
    mMessageTextView = mRootView.findViewById(R.id.message_text);
    mStackTraceContainer = mRootView.findViewById(R.id.stack_trace_container);
    mStackTraceView = mRootView.findViewById(R.id.stack_trace);
    mProgressContainer = mRootView.findViewById(R.id.progress_container);
    mIconView = mRootView.findViewById(R.id.message_icon);
    ViewGroup contentContainer = mRootView.findViewById(R.id.content_container);
    mHeaderView = HeaderView.install(templateContext, contentContainer);
    mMessageContainer = mRootView.findViewById(R.id.message_container);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateMessageDefaultIconTint,
      R.attr.templateLargeImageSizeMin,
      R.attr.templateLargeImageSizeMax,
      R.attr.templateActionButtonListGravity,
      R.attr.templatePlainContentBackgroundColor
    };

    TypedArray ta = templateContext.obtainStyledAttributes(themeAttrs);
    @ColorInt int defaultIconTint = ta.getColor(0, 0);
    int largeImageSizeMin = ta.getDimensionPixelSize(1, 0);
    int largeImageSizeMax = ta.getDimensionPixelSize(2, Integer.MAX_VALUE);
    ActionButtonListView.Gravity actionButtonListGravity =
        ActionButtonListView.Gravity.values()[ta.getInt(3, 0)];
    @ColorInt int backgroundColor = ta.getColor(4, 0);
    ta.recycle();

    mActionListView =
        actionButtonListGravity == Gravity.CENTER
            ? mRootView.findViewById(R.id.action_button_list_view)
            : mRootView.findViewById(R.id.sticky_action_button_list_view);

    // Progress container size is OEM-customizable. Enforce the size limit here.
    ViewUtils.enforceViewSizeLimit(mProgressContainer, largeImageSizeMin, largeImageSizeMax);

    mImageViewParams =
        ImageViewParams.builder()
            .setDefaultTint(defaultIconTint)
            .setBackgroundColor(backgroundColor)
            .build();
    mActionButtonListParams =
        ActionButtonListParams.builder()
            .setMaxActions(MAX_ALLOWED_ACTIONS)
            .setOemReorderingAllowed(true)
            .setOemColorOverrideAllowed(true)
            .setSurroundingColor(backgroundColor)
            .build();
    mIsDebugEnabled = CommonUtils.INSTANCE.isDebugEnabled(templateContext);
  }
}
