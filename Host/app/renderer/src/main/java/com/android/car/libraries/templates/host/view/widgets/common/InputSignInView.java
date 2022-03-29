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

import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.text.InputType.TYPE_CLASS_PHONE;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;
import static android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
import static android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
import static androidx.car.app.model.signin.InputSignInMethod.INPUT_TYPE_PASSWORD;
import static androidx.car.app.model.signin.InputSignInMethod.KEYBOARD_DEFAULT;
import static androidx.car.app.model.signin.InputSignInMethod.KEYBOARD_EMAIL;
import static androidx.car.app.model.signin.InputSignInMethod.KEYBOARD_NUMBER;
import static androidx.car.app.model.signin.InputSignInMethod.KEYBOARD_PHONE;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.car.app.model.CarText;
import androidx.car.app.model.InputCallbackDelegate;
import androidx.car.app.model.signin.InputSignInMethod;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.input.CarEditable;
import com.android.car.libraries.apphost.input.CarEditableListener;
import com.android.car.libraries.apphost.input.InputManager;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.libraries.templates.host.R;
import com.android.car.ui.widget.CarUiTextView;

/** A view that displays {@link InputSignInMethod} UI. */
public class InputSignInView extends LinearLayout implements CarEditable {
  private final int mMaxWidth;

  private CarEditText mSignInEditText;
  private CarUiTextView mSignInEditTextErrorMessage;
  @Nullable private TextWatcher mTextWatcher;

  public InputSignInView(Context context) {
    this(context, null);
  }

  public InputSignInView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public InputSignInView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  @SuppressWarnings("ResourceType")
  public InputSignInView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateSignInMethodViewMaxWidth,
    };

    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    mMaxWidth = ta.getDimensionPixelSize(0, 0);
    ta.recycle();
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
    return mSignInEditText.onCreateInputConnection(editorInfo);
  }

  @Override
  public void setCarEditableListener(CarEditableListener listener) {}

  @Override
  public void setInputEnabled(boolean enabled) {}

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
    if (mMaxWidth > 0 && mMaxWidth < measuredWidth) {
      int measureMode = MeasureSpec.getMode(widthMeasureSpec);
      widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, measureMode);
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  /** Sets the {@link InputSignInMethod} for the view. */
  public void setSignInMethod(
      TemplateContext templateContext,
      InputSignInMethod inputSignInMethod,
      InputManager inputManager,
      CharSequence disabledInputHint,
      boolean isRefresh) {
    clearEditTextListeners();

    // TODO(b/183434044): move this logic to CarRestrictedEditText
    setInputHint(templateContext, inputSignInMethod, disabledInputHint);

    // TODO(b/183434044): move this logic to CarRestrictedEditText
    setInitialText(templateContext, inputSignInMethod, isRefresh);

    setErrorMessage(templateContext, inputSignInMethod);

    // TODO(b/183434044): move this logic to CarRestrictedEditText
    setInputType(inputSignInMethod);

    setShowKeyboardByDefault(templateContext, inputSignInMethod, inputManager);

    // Make sure to set these at the end so that setting initial text etc doesn't trigger text
    // change callbacks.
    setEditTextListeners(templateContext, inputSignInMethod, inputManager);
  }

  /** Clears the edit text focus. */
  public void clearEditTextFocus() {
    mSignInEditText.clearFocus();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    mSignInEditText = findViewById(R.id.input_sign_in_box);
    mSignInEditTextErrorMessage = findViewById(R.id.input_sign_in_error_message);
  }

  private void clearEditTextListeners() {
    mSignInEditText.setOnClickListener(null);
    mSignInEditText.setOnEditorActionListener(null);
    if (mTextWatcher != null) {
      mSignInEditText.removeTextChangedListener(mTextWatcher);
      mTextWatcher = null;
    }
  }

  private void setEditTextListeners(
      TemplateContext templateContext,
      InputSignInMethod inputSignInMethod,
      InputManager inputManager) {
    mSignInEditText.setOnEditorActionListener(
        (view, actionId, event) -> {
          inputManager.stopInput();
          String inputText = mSignInEditText.getText().toString().trim();
          if (TextUtils.isEmpty(inputText)) {
            return false;
          } else {
            submitInput(templateContext, inputText, inputSignInMethod);
            return true;
          }
        });
    mTextWatcher =
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence text, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable text) {
            updateInputText(templateContext, text.toString(), inputSignInMethod);
          }
        };
    mSignInEditText.addTextChangedListener(mTextWatcher);
    mSignInEditText.setInputManager(inputManager);
  }

  private void setInputHint(
      TemplateContext templateContext,
      InputSignInMethod inputSignInMethod,
      CharSequence disabledInputHint) {
    CarText inputHint = inputSignInMethod.getHint();
    CharSequence hint = CarTextUtils.toCharSequenceOrEmpty(templateContext, inputHint);
    mSignInEditText.setHint(mSignInEditText.isEnabled() ? hint : disabledInputHint);
  }

  private void setInitialText(
      TemplateContext templateContext, InputSignInMethod inputSignInMethod, boolean isRefresh) {
    CarText initialText = inputSignInMethod.getDefaultValue();

    CharSequence text = mSignInEditText.getText();
    if (!isRefresh || text == null || text.length() == 0) {
      mSignInEditText.setText(CarTextUtils.toCharSequenceOrEmpty(templateContext, initialText));
      mSignInEditText.setSelection(mSignInEditText.getText().length());
    }
  }

  private void setErrorMessage(
      TemplateContext templateContext, InputSignInMethod inputSignInMethod) {
    CarText errorMessage = inputSignInMethod.getErrorMessage();
    if (!CarText.isNullOrEmpty(errorMessage)) {
      mSignInEditTextErrorMessage.setText(
          CarUiTextUtils.fromCarText(
              templateContext, errorMessage, mSignInEditTextErrorMessage.getMaxLines()));
      mSignInEditTextErrorMessage.setVisibility(VISIBLE);
      mSignInEditText.setErrorState(true);
    } else {
      mSignInEditTextErrorMessage.setVisibility(GONE);
      mSignInEditText.setErrorState(false);
    }
  }

  private void setInputType(InputSignInMethod inputSignInMethod) {
    int inputType;
    switch (inputSignInMethod.getKeyboardType()) {
      case KEYBOARD_PHONE:
        inputType = TYPE_CLASS_PHONE;
        break;
      case KEYBOARD_NUMBER:
        inputType = TYPE_CLASS_NUMBER;
        if (inputSignInMethod.getInputType() == INPUT_TYPE_PASSWORD) {
          inputType |= TYPE_NUMBER_VARIATION_PASSWORD;
        }
        break;
      case KEYBOARD_EMAIL:
        inputType =
            TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_EMAIL_ADDRESS | TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        break;
      case KEYBOARD_DEFAULT:
      default:
        inputType = TYPE_CLASS_TEXT | TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        if (inputSignInMethod.getInputType() == INPUT_TYPE_PASSWORD) {
          inputType |= TYPE_TEXT_VARIATION_PASSWORD;
        }
    }
    mSignInEditText.setInputType(inputType);
  }

  private void setShowKeyboardByDefault(
      TemplateContext templateContext,
      InputSignInMethod inputSignInMethod,
      InputManager inputManager) {
    boolean isRestricted = templateContext.getConstraintsProvider().isConfigRestricted();
    if (inputSignInMethod.isShowKeyboardByDefault() && !isRestricted) {
      inputManager.startInput(this);
    }
  }

  private void submitInput(
      TemplateContext templateContext, String inputText, InputSignInMethod inputSignInMethod) {
    InputCallbackDelegate delegate = inputSignInMethod.getInputCallbackDelegate();
    if (delegate != null) {
      templateContext.getAppDispatcher().dispatchInputSubmitted(delegate, inputText);
    } else {
      L.w(LogTags.TEMPLATE, "Input callback is expected on the template but not set");
    }
  }

  private void updateInputText(
      TemplateContext templateContext, String inputText, InputSignInMethod inputSignInMethod) {
    InputCallbackDelegate delegate = inputSignInMethod.getInputCallbackDelegate();
    if (delegate != null) {
      templateContext.getAppDispatcher().dispatchInputTextChanged(delegate, inputText);
    } else {
      L.w(LogTags.TEMPLATE, "Input callback is expected on the template but not set");
    }
  }
}
