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
package com.android.car.libraries.apphost.template.view.model;

import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import com.android.car.libraries.apphost.template.view.model.ActionWrapper.OnClickListener;
import java.util.ArrayList;
import java.util.List;

/** A host side wrapper for {@link ActionStrip} to allow additional callbacks to the host. */
public final class ActionStripWrapper {
  /**
   * An invalid focused action index.
   *
   * <p>If this value is set, the focus will remain at the user's last focused button.
   */
  public static final int INVALID_FOCUSED_ACTION_INDEX = -1;

  private final List<ActionWrapper> mActions;
  private int mFocusedActionIndex;

  /**
   * Instantiates an {@link ActionStripWrapper}.
   *
   * <p>The optional {@link OnClickListener} allows the host to be notified when an action is
   * clicked.
   */
  public ActionStripWrapper(List<ActionWrapper> actionWrappers, int focusedActionIndex) {
    this.mActions = actionWrappers;
    this.mFocusedActionIndex = focusedActionIndex;
  }

  /** Returns the list of {@link ActionWrapper} in the action strip. */
  public List<ActionWrapper> getActions() {
    return mActions;
  }

  /**
   * Returns the focused action index determined by the host.
   *
   * <p>The value of {@link #INVALID_FOCUSED_ACTION_INDEX} means that the host did not specify any
   * action button to focus, in which case the focus will remain at the user's last focused button.
   */
  public int getFocusedActionIndex() {
    return mFocusedActionIndex;
  }

  /** The builder of {@link ActionStripWrapper}. */
  public static final class Builder {
    private final List<ActionWrapper> mActions;
    private int mFocusedActionIndex = INVALID_FOCUSED_ACTION_INDEX;

    /** Creates an {@link Builder} instance with the given list of {@link ActionWrapper}s. */
    public Builder(List<ActionWrapper> actions) {
      this.mActions = actions;
    }

    /** Creates an {@link Builder} instance with the given {@link ActionStrip}. */
    public Builder(ActionStrip actionStrip) {
      List<ActionWrapper> actions = new ArrayList<>();
      for (Action action : actionStrip.getActions()) {
        actions.add(new ActionWrapper.Builder(action).build());
      }
      this.mActions = actions;
    }

    /** Sets the index of the action button to focus. */
    public Builder setFocusedActionIndex(int index) {
      this.mFocusedActionIndex = index;
      return this;
    }

    /** Constructs an {@link ActionStripWrapper} instance defined by this builder. */
    public ActionStripWrapper build() {
      if (mFocusedActionIndex < 0 || mFocusedActionIndex >= mActions.size()) {
        mFocusedActionIndex = INVALID_FOCUSED_ACTION_INDEX;
      }
      return new ActionStripWrapper(mActions, mFocusedActionIndex);
    }
  }
}
