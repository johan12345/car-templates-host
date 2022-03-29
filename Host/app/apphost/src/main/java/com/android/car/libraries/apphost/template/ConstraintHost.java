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
package com.android.car.libraries.apphost.template;

import androidx.car.app.constraints.IConstraintHost;
import com.android.car.libraries.apphost.AbstractHost;
import com.android.car.libraries.apphost.Host;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.ContentLimitQuery;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.TelemetryEvent;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;

/**
 * A {@link Host} implementation that handles constraints enforced on the connecting app.
 *
 * <p>Host services are per app, and live for the duration of a car session.
 */
public final class ConstraintHost extends AbstractHost {
  private final IConstraintHost.Stub mHostStub = new ConstraintHostStub();

  /** Creates a template host service. */
  public static ConstraintHost create(TemplateContext templateContext) {
    return new ConstraintHost(templateContext);
  }

  @Override
  public IConstraintHost.Stub getBinder() {
    assertIsValid();
    return mHostStub;
  }

  private ConstraintHost(TemplateContext templateContext) {
    super(templateContext, LogTags.CONSTRAINT);
  }

  /**
   * A {@link IConstraintHost.Stub} implementation that used to receive calls to the constraint host
   * API from the client.
   */
  private final class ConstraintHostStub extends IConstraintHost.Stub {
    @Override
    public int getContentLimit(int contentType) {
      if (!isValid()) {
        L.w(LogTags.CONSTRAINT, "Accessed getContentLimit after host became invalidated");
      }
      int contentValue = mTemplateContext.getConstraintsProvider().getContentLimit(contentType);
      mTemplateContext
          .getTelemetryHandler()
          .logCarAppTelemetry(
              TelemetryEvent.newBuilder(UiAction.CONTENT_LIMIT_QUERY)
                  .setCarAppContentLimitQuery(
                      ContentLimitQuery.getContentLimitQuery(contentType, contentValue)));
      return contentValue;
    }
  }
}
