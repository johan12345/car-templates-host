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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.ColorInt;
import androidx.annotation.StyleableRes;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarText;
import androidx.car.app.model.TemplateWrapper;
import androidx.car.app.model.signin.InputSignInMethod;
import androidx.car.app.model.signin.PinSignInMethod;
import androidx.car.app.model.signin.ProviderSignInMethod;
import androidx.car.app.model.signin.QRCodeSignInMethod;
import androidx.car.app.model.signin.SignInTemplate;
import androidx.car.app.model.signin.SignInTemplate.SignInMethod;
import com.android.car.libraries.apphost.common.EventManager;
import com.android.car.libraries.apphost.common.EventManager.EventType;
import com.android.car.libraries.apphost.common.StatusBarManager.StatusBarState;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints;
import com.android.car.libraries.apphost.input.InputManager;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.AbstractTemplatePresenter;
import com.android.car.libraries.apphost.view.TemplatePresenter;
import com.android.car.libraries.apphost.view.common.ActionButtonListParams;
import com.android.car.libraries.apphost.view.common.CarTextParams;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.ActionButtonListView;
import com.android.car.libraries.templates.host.view.widgets.common.ActionButtonListView.Gravity;
import com.android.car.libraries.templates.host.view.widgets.common.ActionButtonView;
import com.android.car.libraries.templates.host.view.widgets.common.CarUiTextUtils;
import com.android.car.libraries.templates.host.view.widgets.common.ClickableSpanTextContainer;
import com.android.car.libraries.templates.host.view.widgets.common.HeaderView;
import com.android.car.libraries.templates.host.view.widgets.common.InputSignInView;
import com.android.car.libraries.templates.host.view.widgets.common.ParkedOnlyFrameLayout;
import com.android.car.libraries.templates.host.view.widgets.common.PinSignInView;
import com.android.car.libraries.templates.host.view.widgets.common.QRCodeSignInView;
import com.android.car.ui.widget.CarUiTextView;
import java.util.List;

/** A {@link TemplatePresenter} for {@link SignInTemplate} instances. */
public class SignInTemplatePresenter extends AbstractTemplatePresenter {
  // TODO(b/183643108): Use a common MAX_ALLOWED_ACTIONS between AAOS and AAP
  private static final int MAX_ALLOWED_ACTIONS = 2;
  private static final CarTextParams ADDITIONAL_TEXT_PARAMS =
      CarTextParams.builder().setAllowClickableSpans(true).build();

  private final InputManager mInputManager;
  private final ViewGroup mRootView;
  private final HeaderView mHeaderView;
  private final LinearLayout mSignInContainer;
  private final CarUiTextView mInstructionTextView;
  private final ActionButtonView mProviderSignInButton;
  private final InputSignInView mInputSignInView;
  private final PinSignInView mPinSignInView;
  private final QRCodeSignInView mQRCodeSignInView;
  private final ClickableSpanTextContainer mAdditionalTextView;
  private final ActionButtonListView mActionListView;
  private final ParkedOnlyFrameLayout mContentContainer;
  private final ProgressBar mProgressBar;
  private final String mDisabledInputHint;
  private final ActionButtonListParams mActionButtonListParams;
  private final CarTextParams mInstructionTextParams;
  private boolean mInputWasActiveOnLastWindowFocus;

  /** Create a {@link SignInTemplatePresenter} instance. */
  static SignInTemplatePresenter create(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    SignInTemplatePresenter presenter =
        new SignInTemplatePresenter(templateContext, templateWrapper);
    presenter.update();
    return presenter;
  }

  @Override
  protected View getDefaultFocusedView() {
    if (mProviderSignInButton.getVisibility() == VISIBLE) {
      return mProviderSignInButton;
    }
    if (mInputSignInView.getVisibility() == VISIBLE) {
      // Hide the cursor by clearing the edit text focus if input is not active
      if (!mInputManager.isInputActive()) {
        mInputSignInView.clearEditTextFocus();
      }
    }
    if (mPinSignInView.getVisibility() == VISIBLE) {
      return mPinSignInView;
    }
    if (mActionListView.getVisibility() == VISIBLE) {
      return mActionListView;
    }

    return super.getDefaultFocusedView();
  }

  @Override
  public void onStart() {
    super.onStart();
    EventManager eventManager = getTemplateContext().getEventManager();
    eventManager.subscribeEvent(
        this,
        EventType.WINDOW_FOCUS_CHANGED,
        () -> {
          if (hasWindowFocus()) {
            // If the input was active the last time the window was focused, it means
            // that the user just dismissed the car screen keyboard. In this case, focus
            // on the action button list.
            if (mInputWasActiveOnLastWindowFocus) {
              mActionListView.requestFocus();
            }
          }
          mInputWasActiveOnLastWindowFocus = mInputManager.isInputActive();
        });
  }

  @Override
  public void onStop() {
    // TODO(b/182232738): Reenable keyboard listener
    // LocationManager locationManager = LocationManager.getInstance();
    // locationManager.removeKeyboardEnabledListener(driveStatusEventListener);
    getTemplateContext().getEventManager().unsubscribeEvent(this, EventType.WINDOW_FOCUS_CHANGED);
    super.onStop();
  }

  @Override
  public void onPause() {
    mInputManager.stopInput();
    super.onPause();
  }

  @Override
  public void onTemplateChanged() {
    update();
  }

  @Override
  public View getView() {
    return mRootView;
  }

  /** Updates the view with current values in the {@link SignInTemplate}. */
  private void update() {
    TemplateContext templateContext = getTemplateContext();
    SignInTemplate template = (SignInTemplate) getTemplate();

    setHeaderView(templateContext, template);
    setProgressBar(template);
    setInstructionText(templateContext, template);
    setSignInView(templateContext, template);
    setAdditionalText(templateContext, template);
    setActionListButtons(templateContext, template);
  }

  private void setHeaderView(TemplateContext templateContext, SignInTemplate template) {
    // If we have a title or a header action, show the header; hide it otherwise.
    CarText title = template.getTitle();
    Action headerAction = template.getHeaderAction();
    if (!CarText.isNullOrEmpty(title) || headerAction != null) {
      mHeaderView.setContent(templateContext, title, headerAction);
    } else {
      mHeaderView.setContent(templateContext, null, null);
    }
    mHeaderView.setActionStrip(
        template.getActionStrip(), ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE);
  }

  private void setProgressBar(SignInTemplate template) {
    if (template.isLoading()) {
      mProgressBar.setVisibility(VISIBLE);
      mSignInContainer.setVisibility(GONE);
    } else {
      mProgressBar.setVisibility(GONE);
      mSignInContainer.setVisibility(VISIBLE);
    }
  }

  private void setInstructionText(TemplateContext templateContext, SignInTemplate template) {
    CarText instructions = template.getInstructions();
    if (!CarText.isNullOrEmpty(instructions)) {
      mInstructionTextView.setText(
          CarUiTextUtils.fromCarText(
              templateContext,
              instructions,
              mInstructionTextParams,
              mInstructionTextView.getMaxLines()));
      mInstructionTextView.setVisibility(VISIBLE);
    } else {
      mInstructionTextView.setVisibility(GONE);
    }
  }

  private void setSignInView(TemplateContext templateContext, SignInTemplate template) {
    // Reset the sign-in view
    mProviderSignInButton.setVisibility(GONE);
    mInputSignInView.setVisibility(GONE);
    mPinSignInView.setVisibility(GONE);
    mQRCodeSignInView.setVisibility(GONE);
    SignInMethod signInMethod = template.getSignInMethod();
    if (signInMethod instanceof ProviderSignInMethod) {
      ProviderSignInMethod providerSignInMethod = (ProviderSignInMethod) signInMethod;
      Action providerSignInAction = providerSignInMethod.getAction();
      // OEMs cannot overwrite provider method button in Sign in template
      mProviderSignInButton.setAction(
          templateContext,
          providerSignInAction,
          ActionButtonListParams.builder().setAllowAppColor(true).build());
      mProviderSignInButton.setVisibility(VISIBLE);
    } else if (signInMethod instanceof InputSignInMethod) {
      InputSignInMethod inputSignInMethod = (InputSignInMethod) signInMethod;
      mInputSignInView.setSignInMethod(
          templateContext,
          inputSignInMethod,
          mInputManager,
          mDisabledInputHint,
          getTemplateWrapper().isRefresh());
      mInputSignInView.setVisibility(VISIBLE);
    } else if (signInMethod instanceof PinSignInMethod) {
      PinSignInMethod pinSignInMethod = (PinSignInMethod) signInMethod;
      mPinSignInView.setText(
          CarUiTextUtils.fromCarText(
              templateContext, pinSignInMethod.getPinCode(), mPinSignInView.getMaxLines()));
      mPinSignInView.setVisibility(VISIBLE);
    } else if (signInMethod instanceof QRCodeSignInMethod) {
      QRCodeSignInMethod qrCodeSignInMethod = (QRCodeSignInMethod) signInMethod;
      mQRCodeSignInView.setQRCodeSignInMethod(templateContext, qrCodeSignInMethod);
      mQRCodeSignInView.setVisibility(VISIBLE);
    } else {
      L.w(LogTags.TEMPLATE, "Unknown sign in method: %s", signInMethod);
    }
  }

  private void setAdditionalText(TemplateContext templateContext, SignInTemplate template) {
    CarText additionalText = template.getAdditionalText();

    if (!CarText.isNullOrEmpty(additionalText)) {
      mAdditionalTextView.setText(
          CarTextUtils.toCharSequenceOrEmpty(
              templateContext, additionalText, ADDITIONAL_TEXT_PARAMS));
      mAdditionalTextView.setVisibility(VISIBLE);
    } else {
      mAdditionalTextView.setVisibility(GONE);
    }
  }

  private void setActionListButtons(TemplateContext templateContext, SignInTemplate template) {
    List<Action> actionList = template.getActions();
    if (!actionList.isEmpty()) {
      mActionListView.setActionList(templateContext, actionList, mActionButtonListParams);
      mActionListView.setVisibility(VISIBLE);
    } else {
      mActionListView.setVisibility(GONE);
    }
  }

  @SuppressWarnings("nullness:method.invocation")
  private SignInTemplatePresenter(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    super(templateContext, templateWrapper, StatusBarState.LIGHT);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateActionButtonListGravity, R.attr.templatePlainContentBackgroundColor
    };

    TypedArray ta = templateContext.obtainStyledAttributes(themeAttrs);
    ActionButtonListView.Gravity actionButtonListGravity =
        ActionButtonListView.Gravity.values()[ta.getInt(0, 0)];
    @ColorInt int backgroundColor = ta.getColor(1, 0);
    ta.recycle();

    mInputManager = templateContext.getInputManager();
    mDisabledInputHint =
        templateContext.getString(templateContext.getHostResourceIds().getSearchHintDisabledText());
    mActionButtonListParams =
        ActionButtonListParams.builder()
            .setMaxActions(MAX_ALLOWED_ACTIONS)
            .setOemReorderingAllowed(false)
            .setOemColorOverrideAllowed(false)
            .setSurroundingColor(backgroundColor)
            .build();
    mInstructionTextParams =
        CarTextParams.builder()
            .setColorSpanConstraints(CarColorConstraints.STANDARD_ONLY)
            .setBackgroundColor(backgroundColor)
            .build();
    mRootView =
        (ViewGroup)
            LayoutInflater.from(templateContext).inflate(R.layout.sign_in_template_layout, null);
    mSignInContainer = mRootView.findViewById(R.id.sign_in_container);
    mInstructionTextView = mRootView.findViewById(R.id.instruction_text);
    mProviderSignInButton = mRootView.findViewById(R.id.provider_sign_in_button);
    mInputSignInView = mRootView.findViewById(R.id.input_sign_in_view);
    mPinSignInView = mRootView.findViewById(R.id.pin_sign_in_view);
    mQRCodeSignInView = mRootView.findViewById(R.id.qr_code_sign_in_view);
    mAdditionalTextView = mRootView.findViewById(R.id.additional_text);
    mContentContainer = mRootView.findViewById(R.id.park_only_container);
    mHeaderView = HeaderView.install(templateContext, mContentContainer);
    mContentContainer.setTemplateContext(templateContext);
    mProgressBar = mRootView.findViewById(R.id.sign_in_progress_bar);
    mActionListView =
        actionButtonListGravity == Gravity.CENTER
            ? mRootView.findViewById(R.id.action_button_list_view)
            : mRootView.findViewById(R.id.sticky_action_button_list_view);
  }
}
