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
package com.android.car.libraries.apphost;

import static com.android.car.libraries.apphost.common.EventManager.EventType.APP_DISCONNECTED;
import static com.android.car.libraries.apphost.common.EventManager.EventType.APP_UNBOUND;

import android.content.Intent;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.L;
import com.google.common.base.Preconditions;
import java.io.PrintWriter;

/**
 * Abstract base class for {@link Host}s which implements some of the common host service
 * functionality.
 */
public abstract class AbstractHost implements Host {
  protected TemplateContext mTemplateContext;
  private boolean mIsValid = true;
  private final String mName;

  @SuppressWarnings("nullness")
  protected AbstractHost(TemplateContext templateContext, String name) {
    mTemplateContext = templateContext;
    mName = name;
    addEventSubscriptions();
  }

  @Override
  public void setTemplateContext(TemplateContext templateContext) {
    removeEventSubscriptions();
    mTemplateContext = templateContext;
    addEventSubscriptions();
  }

  @Override
  public void invalidateHost() {
    mIsValid = false;
  }

  @Override
  public void onCarAppBound() {}

  @Override
  public void onNewIntentDispatched() {}

  @Override
  public void onBindToApp(Intent intent) {}

  @Override
  public void reportStatus(PrintWriter pw, Pii piiHandling) {}

  /** Called when the app is disconnected. */
  public void onDisconnectedEvent() {}

  /** Called when the app is unbound. */
  public void onUnboundEvent() {}

  /** Asserts that the service is valid. */
  protected void assertIsValid() {
    Preconditions.checkState(mIsValid, "Accessed a host service after it became invalidated");
  }

  /** Runs the {@code runnable} iff the host is valid. */
  protected void runIfValid(String methodName, Runnable runnable) {
    if (isValid()) {
      runnable.run();
    } else {
      L.w(mName, "Accessed %s after host became invalidated", methodName);
    }
  }

  /** Returns whether the host is valid. */
  protected boolean isValid() {
    return mIsValid;
  }

  private void addEventSubscriptions() {
    mTemplateContext
        .getEventManager()
        .subscribeEvent(this, APP_DISCONNECTED, this::onDisconnectedEvent);
    mTemplateContext.getEventManager().subscribeEvent(this, APP_UNBOUND, this::onUnboundEvent);
  }

  private void removeEventSubscriptions() {
    mTemplateContext.getEventManager().unsubscribeEvent(this, APP_DISCONNECTED);
    mTemplateContext.getEventManager().unsubscribeEvent(this, APP_UNBOUND);
  }
}
