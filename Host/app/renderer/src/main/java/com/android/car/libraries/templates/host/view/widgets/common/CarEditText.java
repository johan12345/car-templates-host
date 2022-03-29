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
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.android.car.libraries.apphost.input.CarEditable;
import com.android.car.libraries.apphost.input.CarEditableListener;
import com.android.car.libraries.apphost.input.InputManager;
import com.android.car.libraries.templates.host.R;

/**
 * A EditText for use in-car. This EditText:
 *
 * <ul>
 *   <li>Disables selection
 *   <li>Disables Cut/Copy/Paste
 *   <li>Force-disables suggestions
 * </ul>
 */
public class CarEditText extends EditText implements CarEditable {
  private static final int[] ERROR_STATE =
      new int[] {R.attr.state_error};
  private static final boolean SELECTION_CLAMPING_ENABLED = false;

  private int mLastSelEnd = 0;
  private int mLastSelStart = 0;
  private boolean mCursorClamped;
  private boolean mInErrorState;

  private CarEditableListener mCarEditableListener;
  private KeyListener mListener;
  private InputManager mInputManager;

  /**
   * Listener for events when the user interacts with the keyboard similar to {@link
   * android.text.method.KeyListener}.
   */
  public interface KeyListener {
    /** Callback when a key is pressed. */
    void onKeyDown(char key);

    /** Callback when a key is released. */
    void onKeyUp(char key);

    /** Callback when text has been changed by another input connection or copy/paste. */
    void onCommitText(String input);

    /** Callback when the user closes the keyboard. */
    void onCloseKeyboard();

    /** Callback when the text field has been cleared. */
    void onDelete();
  }

  @SuppressLint("ClickableViewAccessibility")
  @SuppressWarnings("nullness") // suppress under initialization warning for this
  public CarEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
    setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    setTextIsSelectable(false);
    setLongClickable(false);
    setFocusableInTouchMode(true);
    setSelection(getText().length());
    mCursorClamped = true;
    setOnEditorActionListener(
        new OnEditorActionListener() {
          @Override
          public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (mListener != null && actionId == EditorInfo.IME_ACTION_DONE) {
              mListener.onCloseKeyboard();
            }
            // Return false because we don't want to hijack the default behavior.
            return false;
          }
        });
    setCustomSelectionActionModeCallback(
        new Callback() {
          @Override
          public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
          }

          @Override
          public void onDestroyActionMode(ActionMode mode) {}

          @Override
          public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return false;
          }

          @Override
          public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
          }
        });
    setOnTouchListener(
        (v, event) -> {
          if (MotionEvent.ACTION_UP == event.getAction()) {
            mInputManager.startInput(CarEditText.this);
          }
          return false;
        });
  }

  public void setKeyListener(KeyListener listener) {
    mListener = listener;
  }

  public void setInputManager(InputManager inputManager) {
    mInputManager = inputManager;
  }

  /** Sets whether this edit box is in error state or not */
  public void setErrorState(boolean inErrorState) {
    mInErrorState = inErrorState;
    refreshDrawableState();
  }

  @Override
  protected void onSelectionChanged(int selStart, int selEnd) {
    super.onSelectionChanged(selStart, selEnd);
    if (mCursorClamped && SELECTION_CLAMPING_ENABLED) {
      setSelection(mLastSelStart, mLastSelEnd);
      return;
    }
    if (mCarEditableListener != null) {
      mCarEditableListener.onUpdateSelection(mLastSelStart, mLastSelEnd, selStart, selEnd);
    }
    mLastSelStart = selStart;
    mLastSelEnd = selEnd;
  }

  @Override
  @Nullable
  public ActionMode startActionMode(Callback callback) {
    return null;
  }

  @Override
  protected int[] onCreateDrawableState(int extraSpace) {
    final int[] state;
    if (mInErrorState) {
      state = super.onCreateDrawableState(extraSpace + 1);
      mergeDrawableStates(state, ERROR_STATE);
    } else {
      state = super.onCreateDrawableState(extraSpace);
    }
    return state;
  }

  @Override
  public void setCarEditableListener(CarEditableListener listener) {
    mCarEditableListener = listener;
  }

  @Override
  public void setInputEnabled(boolean enabled) {
    mCursorClamped = !enabled;
  }

  @Override
  public boolean performClick() {
    boolean result = super.performClick();
    mInputManager.startInput(CarEditText.this);
    return result;
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
    return new InputConnectionWrapper(inputConnection, false) {
      @Override
      public boolean sendKeyEvent(KeyEvent event) {
        // TODO(b/208707793): Remove the handleKeyEventNoWindowFocus if found system side fix for R
        if (Build.VERSION.SDK_INT == VERSION_CODES.R) {
          return handleKeyEventNoWindowFocus(event);
        }
        if (mListener != null) {
          if (event.getAction() == KeyEvent.ACTION_DOWN) {
            mListener.onKeyDown((char) event.getKeyCode());
          } else if (event.getAction() == KeyEvent.ACTION_UP) {
            mListener.onKeyUp((char) event.getKeyCode());
          }
          return true;
        } else {
          return super.sendKeyEvent(event);
        }
      }

      private boolean handleKeyEventNoWindowFocus(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
          if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
            return super.deleteSurroundingText(1, 0);
          } else {
            return super.commitText(Character.toString(event.getNumber()), 1);
          }
        }
        return false;
      }

      @Override
      public boolean commitText(CharSequence charSequence, int i) {
        if (mListener != null) {
          mListener.onCommitText(charSequence.toString());
          return true;
        }
        return super.commitText(charSequence, i);
      }

      @Override
      public boolean deleteSurroundingText(int i, int i1) {
        if (mListener != null) {
          mListener.onDelete();
          return true;
        }
        return super.deleteSurroundingText(i, i1);
      }
    };
  }
}
