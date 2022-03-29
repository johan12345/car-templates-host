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
package com.android.car.libraries.templates.host.internal;

import android.content.ComponentName;
import android.content.Context;
import androidx.car.app.CarContext;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.TemplateWrapper;
import com.android.car.libraries.apphost.CarHost;
import com.android.car.libraries.apphost.common.CarAppError;
import com.android.car.libraries.apphost.common.CarAppManager;
import com.android.car.libraries.apphost.common.ErrorHandler;
import com.android.car.libraries.apphost.common.ErrorMessageTemplateBuilder;
import com.android.car.libraries.apphost.common.HostResourceIds;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.template.AppHost;

/**
 * Handles error cases, allowing classes that do not handle ui to be able to display an error screen
 * to the user.
 */
public class ErrorHandlerImpl implements ErrorHandler {
  private final Context mContext;
  private final ComponentName mAppName;
  private final CarAppManager mCarAppManager;
  private final HostResourceIds mHostResourceIdsImpl;

  /** Returns a {@link ErrorHandlerImpl} to show an error screen */
  public static ErrorHandlerImpl create(
      Context context,
      ComponentName appName,
      CarAppManager carAppManager,
      HostResourceIds hostResourceIds) {
    return new ErrorHandlerImpl(context, appName, carAppManager, hostResourceIds);
  }

  private ErrorHandlerImpl(
      Context context,
      ComponentName appName,
      CarAppManager carAppManager,
      HostResourceIds hostResourceIds) {
    mContext = context;
    mAppName = appName;
    mCarAppManager = carAppManager;
    mHostResourceIdsImpl = hostResourceIds;
  }

  @Override
  public void showError(CarAppError error) {
    Throwable cause = error.getCause();
    if (cause != null) {
      if (error.logVerbose()) {
        L.v(LogTags.TEMPLATE, cause, "Error: %s", error);
      } else {
        L.e(LogTags.TEMPLATE, cause, "Error: %s", error);
      }
    } else {
      if (error.logVerbose()) {
        L.v(LogTags.TEMPLATE, "Error: %s", error);
      } else {
        L.e(LogTags.TEMPLATE, "Error: %s", error);
      }
    }

    MessageTemplate errorMessageTemplate =
        new ErrorMessageTemplateBuilder(
                mContext,
                error,
                mHostResourceIdsImpl,
                // TODO(b/183145188): finish car app should not kill the host, just
                //  the activity
                mCarAppManager::finishCarApp)
            .build();

    CarHost carHost = CarHostRepository.INSTANCE.get(mAppName);
    AppHost apphost = (AppHost) carHost.getHostOrThrow(CarContext.APP_SERVICE);
    apphost.getUIController().setTemplate(mAppName, TemplateWrapper.wrap(errorMessageTemplate));
  }
}
