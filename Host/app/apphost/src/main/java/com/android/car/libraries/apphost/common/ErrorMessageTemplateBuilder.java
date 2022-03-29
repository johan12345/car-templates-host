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

import static androidx.car.app.model.CarIcon.ERROR;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.Action;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;

/**
 * Formats {@link CarAppError} into {@link MessageTemplate} to allow displaying the error to the
 * user.
 */
public class ErrorMessageTemplateBuilder {
  private final Context mContext;
  private final HostResourceIds mHostResourceIds;
  private final CarAppError mError;
  private final ComponentName mAppName;
  private final OnClickListener mMainActionOnClickListener;

  private String mAppLabel;

  /** Constructor of an {@link ErrorMessageTemplateBuilder} */
  @SuppressWarnings("nullness")
  public ErrorMessageTemplateBuilder(
      @NonNull Context context,
      @NonNull CarAppError error,
      @NonNull HostResourceIds instance,
      @NonNull OnClickListener listener) {

    if (context == null || error == null || instance == null || listener == null) {
      throw new NullPointerException();
    }

    mContext = context;
    mError = error;
    mAppName = error.getAppName();
    mHostResourceIds = instance;
    mMainActionOnClickListener = listener;
  }

  /** Returns an {@link ErrorMessageTemplateBuilder} with {@link String} appLabel */
  @NonNull
  public ErrorMessageTemplateBuilder setAppLabel(String appLabel) {
    mAppLabel = appLabel;
    return this;
  }

  /** Returns a {@link MessageTemplate} with error message */
  public MessageTemplate build() {
    if (mAppLabel == null) {
      PackageManager pm = mContext.getPackageManager();
      ApplicationInfo applicationInfo = null;
      try {
        applicationInfo = pm.getApplicationInfo(mAppName.getPackageName(), 0);
      } catch (NameNotFoundException e) {
        L.e(LogTags.TEMPLATE, e, "Could not find the application info");
      }
      mAppLabel =
          applicationInfo == null
              ? mAppName.getPackageName()
              : pm.getApplicationLabel(applicationInfo).toString();
    }
    String errorMessage = getErrorMessage(mAppLabel, mError);
    if (errorMessage == null) {
      errorMessage = mContext.getString(mHostResourceIds.getClientErrorText(), mAppLabel);
    }

    // TODO(b/179320446): Note that we use a whitespace as the title to not show anything in
    // the header. We will have to update this to some internal-only template once the
    // whitespace string no longer supperted.
    MessageTemplate.Builder messageTemplateBuilder =
        new MessageTemplate.Builder(errorMessage).setTitle(" ").setIcon(ERROR);

    Throwable cause = mError.getCause();
    if (cause != null) {
      messageTemplateBuilder.setDebugMessage(cause);
    }

    String debugMessage = mError.getDebugMessage();
    if (debugMessage != null) {
      messageTemplateBuilder.setDebugMessage(debugMessage);
    }

    messageTemplateBuilder.addAction(
        new Action.Builder()
            .setTitle(mContext.getString(mHostResourceIds.getExitText()))
            .setOnClickListener(mMainActionOnClickListener)
            .build());

    Action extraAction = getExtraAction(mError.getType(), mError.getExtraAction());
    if (extraAction != null) {
      messageTemplateBuilder.addAction(extraAction);
    }

    return messageTemplateBuilder.build();
  }

  @Nullable
  private String getErrorMessage(String appLabel, @Nullable CarAppError error) {
    CarAppError.Type type = error == null ? null : error.getType();
    if (error == null || type == null) {
      return null;
    }
    switch (type) {
      case ANR_TIMEOUT:
        return mContext.getString(mHostResourceIds.getAnrMessage(), appLabel);
      case ANR_WAITING:
        return mContext.getString(mHostResourceIds.getAnrWaiting());
      case INCOMPATIBLE_CLIENT_VERSION:
        ApiIncompatibilityType apiIncompatibilityType = ApiIncompatibilityType.HOST_TOO_OLD;
        Throwable exception = error.getCause();
        if (exception instanceof IncompatibleApiException) {
          apiIncompatibilityType = ((IncompatibleApiException) exception).getIncompatibilityType();
        }
        return mContext.getString(
            mHostResourceIds.getAppApiIncompatibleText(apiIncompatibilityType), appLabel);
      case MISSING_PERMISSION:
        return mContext.getString(mHostResourceIds.getMissingPermissionText(), appLabel);
    }
    throw new IllegalArgumentException("Unknown error type: " + type);
  }

  @Nullable
  private Action getExtraAction(@Nullable CarAppError.Type type, @Nullable Runnable extraAction) {
    if (type != CarAppError.Type.ANR_TIMEOUT || extraAction == null) {
      return null;
    }
    return new Action.Builder()
        .setTitle(mContext.getString(mHostResourceIds.getAnrWait()))
        .setOnClickListener(extraAction::run)
        .build();
  }
}
