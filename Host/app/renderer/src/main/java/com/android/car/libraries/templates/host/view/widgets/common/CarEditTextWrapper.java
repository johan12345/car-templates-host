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

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import androidx.annotation.Nullable;
import com.android.car.libraries.apphost.input.CarEditable;
import com.android.car.libraries.apphost.input.CarEditableListener;
import com.android.car.libraries.apphost.input.InputManager;

/** A wrapper for {@link EditText} to make it conform to {@link CarEditable}. */
public class CarEditTextWrapper implements CarEditable {
  private final EditText mEditText;
  private int mLastSelectionEnd = 0;
  private int mLastSelectionStart = 0;
  @Nullable private CarEditableListener mCarEditableListener;

  @SuppressLint("ClickableViewAccessibility")
  @SuppressWarnings("nullness:argument") // Accessing "this" inside click listener.
  public CarEditTextWrapper(EditText editText, InputManager inputManager) {
    mEditText = editText;

    // Setup an accessibility delegate to get the text selection changes. This is required in
    // order
    // to conform to the CarEditable which requires a text selection update listener.
    AccessibilityDelegate accessibilityDelegate =
        new AccessibilityDelegate() {
          @Override
          public void sendAccessibilityEvent(View host, int eventType) {
            super.sendAccessibilityEvent(host, eventType);
            if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
              if (mCarEditableListener != null) {
                mCarEditableListener.onUpdateSelection(
                    mLastSelectionStart,
                    mLastSelectionEnd,
                    mEditText.getSelectionStart(),
                    mEditText.getSelectionEnd());
              }
              mLastSelectionStart = mEditText.getSelectionStart();
              mLastSelectionEnd = mEditText.getSelectionEnd();
            }
          }
        };
    editText.setAccessibilityDelegate(accessibilityDelegate);
    editText.setOnClickListener((view) -> inputManager.startInput(CarEditTextWrapper.this));
    editText.setOnFocusChangeListener(
        (view, hasFocus) -> {
          if (!hasFocus) {
            inputManager.stopInput();
          }
        });
    // Android will dispatch in the following order:
    // onTouch
    // onFocus
    // onClick
    // However if internally the view consumes any it will stop dispatching.  If an EditText
    // does not have focus it will consume the focus and not send the onClick.
    editText.setOnTouchListener(
        (v, event) -> {
          if (MotionEvent.ACTION_UP == event.getAction()) {
            inputManager.startInput(CarEditTextWrapper.this);
          }
          return false;
        });
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    return mEditText.onCreateInputConnection(outAttrs);
  }

  @Override
  public void setCarEditableListener(@Nullable CarEditableListener listener) {
    mCarEditableListener = listener;
  }

  @Override
  public void setInputEnabled(boolean enabled) {
    mEditText.setEnabled(enabled);
  }
}
