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

package com.android.car.libraries.apphost.common;

import android.content.ComponentName;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A class that encapsulates an error message that occurs for an app. */
public class CarAppError {
  /** Error type. Each type corresponds to an specific message to be displayed to the user */
  public enum Type {
    /** The client application is not responding in timely fashion */
    ANR_TIMEOUT,

    /** The user has requested to wait for the application to respond */
    ANR_WAITING,

    /** The client is using a version of the SDK that is not compatible with this host */
    INCOMPATIBLE_CLIENT_VERSION,

    /** The client does not have a required permission */
    MISSING_PERMISSION,
  }

  private final ComponentName mAppName;
  @Nullable private final Type mType;
  @Nullable private final Throwable mCause;
  @Nullable private final String mDebugMessage;
  @Nullable private final Runnable mExtraAction;
  private final boolean mLogVerbose;

  /** Returns a {@link Builder} for the given {@code appName}. */
  public static Builder builder(ComponentName appName) {
    return new Builder(appName);
  }

  /** Returns the {@link ComponentName} representing an app. */
  public ComponentName getAppName() {
    return mAppName;
  }

  /**
   * Returns the error type or {@code null} to show a generic error message.
   *
   * @see Builder#setType
   */
  @Nullable
  public Type getType() {
    return mType;
  }

  /**
   * Returns the debug message for displaying in the DHU or any head unit on debug builds.
   *
   * @see Builder#setDebugMessage
   */
  @Nullable
  public String getDebugMessage() {
    return mDebugMessage;
  }

  /**
   * Returns the debug message for displaying in the DHU or any head unit on debug builds.
   *
   * @see Builder#setCause
   */
  @Nullable
  public Throwable getCause() {
    return mCause;
  }

  /**
   * Returns the {@code action} for the error screen shown to the user, on top of the exit which is
   * default.
   *
   * @see Builder#setExtraAction
   */
  @Nullable
  public Runnable getExtraAction() {
    return mExtraAction;
  }

  /**
   * Returns whether to log this {@link CarAppError} as a verbose log.
   *
   * <p>The default is to log as error, but can be overridden via {@link Builder#setLogVerbose}
   */
  public boolean logVerbose() {
    return mLogVerbose;
  }

  @Override
  public String toString() {
    return "[app: "
        + mAppName
        + ", type: "
        + mType
        + ", cause: "
        + (mCause != null
            ? mCause.getClass().getCanonicalName() + ": " + mCause.getMessage()
            : null)
        + ", debug msg: "
        + mDebugMessage
        + "]";
  }

  private CarAppError(Builder builder) {
    mAppName = builder.mAppName;
    mType = builder.mType;
    mCause = builder.mCause;
    mDebugMessage = builder.mDebugMessage;
    mExtraAction = builder.mExtraAction;
    mLogVerbose = builder.mLogVerbose;
  }

  /** A builder for {@link CarAppError}. */
  public static class Builder {
    private final ComponentName mAppName;
    @Nullable private Type mType;
    @Nullable private Throwable mCause;
    @Nullable private String mDebugMessage;
    @Nullable private Runnable mExtraAction;
    public boolean mLogVerbose;

    private Builder(ComponentName appName) {
      mAppName = appName;
    }

    /** Sets the error type, or {@code null} to show a generic error message. */
    public Builder setType(Type type) {
      mType = type;
      return this;
    }

    /** Sets the exception for displaying in the DHU or any head unit on debug builds. */
    public Builder setCause(Throwable cause) {
      mCause = cause;
      return this;
    }

    /** Sets the debug message for displaying in the DHU or any head unit on debug builds. */
    public Builder setDebugMessage(String debugMessage) {
      mDebugMessage = debugMessage;
      return this;
    }

    /**
     * Adds the {@code action} to the error screen shown to the user, on top of the exit which is
     * default.
     */
    public Builder setExtraAction(Runnable extraAction) {
      mExtraAction = extraAction;
      return this;
    }

    /** Sets whether to log the {@link CarAppError} as verbose only. */
    public Builder setLogVerbose(boolean logVerbose) {
      mLogVerbose = logVerbose;
      return this;
    }

    /** Constructs the {@link CarAppError} instance. */
    public CarAppError build() {
      return new CarAppError(this);
    }
  }
}
