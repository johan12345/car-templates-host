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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.car.libraries.apphost.common.EventManager;
import com.android.car.libraries.apphost.common.EventManager.EventType;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.templates.host.R;
import com.android.car.ui.widget.CarUiTextView;

/**
 * Custom view that hides content and shows an appropriate message when car is driving. This view
 * will hide all children views while the driving message is being shown. Usage of this layout
 * should use a single container layout as a child, and visibility of that child should not be
 * modified outside of this layout. This layout does not maintain any visibility states of children
 * views before or after drive state changes. This means that if the visibility of children views
 * are updated directly the visibility may not be consistent after the driving message disappears.
 */
public class ParkedOnlyFrameLayout extends FrameLayout {

  private View mDrivingMessageView;

  private boolean mIsLockedOut;
  private TemplateContext mTemplateContext;
  private EventManager mEventManager;

  public ParkedOnlyFrameLayout(@NonNull Context context) {
    super(context);
  }

  public ParkedOnlyFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public ParkedOnlyFrameLayout(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public ParkedOnlyFrameLayout(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    View rootView = LayoutInflater.from(getContext()).inflate(R.layout.driving_message_view, this);
    mDrivingMessageView = rootView.findViewById(R.id.driving_message_view);
  }

  @Override
  protected void onDetachedFromWindow() {
    if (mEventManager != null) {
      mEventManager.unsubscribeEvent(this, EventType.CONSTRAINTS);
    }
    super.onDetachedFromWindow();
  }

  /** Set the template context used to start listening for uxr constraints. */
  public void setTemplateContext(TemplateContext templateContext) {
    mTemplateContext = templateContext;
    mEventManager = templateContext.getEventManager();

    mEventManager.subscribeEvent(this, EventType.CONSTRAINTS, this::update);

    CarUiTextView drivingMessageText = mDrivingMessageView.findViewById(R.id.driving_message_text);
    drivingMessageText.setText(
        CarUiTextUtils.fromCharSequence(
            templateContext,
            templateContext.getString(
                templateContext.getHostResourceIds().getDrivingStateMessageText()),
            drivingMessageText.getMaxLines()));
    update();
  }

  /** Get whether the content view is being hidden, and the driving message is being shown. */
  public boolean isLockedOut() {
    return mIsLockedOut;
  }

  private void update() {
    boolean isRestricted = mTemplateContext.getConstraintsProvider().isConfigRestricted();
    if (isRestricted == mIsLockedOut) {
      return;
    }
    mIsLockedOut = isRestricted;

    // Hide IME if ParkedOnlyFrameLayout is visible
    if (mIsLockedOut) {
      mTemplateContext.getInputManager().stopInput();
    }

    // Toggle visibility of all children views; the driving message will be shown if locked out,
    // content views are shown otherwise.
    for (int i = 0; i < this.getChildCount(); i++) {
      View view = getChildAt(i);
      if (view.getId() == mDrivingMessageView.getId()) {
        view.setVisibility(mIsLockedOut ? VISIBLE : GONE);
      } else {
        view.setVisibility(mIsLockedOut ? GONE : VISIBLE);
      }
    }
  }
}
