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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.Action;

/** A host side wrapper for {@link Action} to allow additional callbacks to the host. */
public class ActionWrapper {
  /** A host-side on-click listener. */
  public interface OnClickListener {
    /** Called when the user clicks the action. */
    void onClick();
  }

  private final Action mAction;
  @Nullable private final OnClickListener mOnClickListener;

  /** Returns the wrapped action. */
  @NonNull
  public Action get() {
    return mAction;
  }

  /** Returns the host-side on-click listener. */
  @Nullable
  public OnClickListener getOnClickListener() {
    return mOnClickListener;
  }

  /** Instantiates an {@link ActionWrapper}. */
  private ActionWrapper(Action action, @Nullable OnClickListener onClickListener) {
    this.mAction = action;
    this.mOnClickListener = onClickListener;
  }

  /** The builder of {@link ActionWrapper}. */
  public static final class Builder {
    private final Action mAction;
    @Nullable private OnClickListener mOnClickListener;

    /** Creates an {@link Builder} instance with the given {@link Action}. */
    public Builder(Action action) {
      this.mAction = action;
    }

    /** Sets the host-side {@link OnClickListener}. */
    public Builder setOnClickListener(@Nullable OnClickListener onClickListener) {
      this.mOnClickListener = onClickListener;
      return this;
    }

    /** Constructs an {@link ActionWrapper} instance defined by this builder. */
    public ActionWrapper build() {
      return new ActionWrapper(mAction, mOnClickListener);
    }
  }
}
