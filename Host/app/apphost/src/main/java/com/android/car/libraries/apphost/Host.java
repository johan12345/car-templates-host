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

import android.content.Intent;
import android.os.IBinder;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.StatusReporter;

/**
 * A host that manages app specific behaviors such as managing template APIs, navigation APIs, etc.
 *
 * <p>This should be registered with {@link CarHost}.
 */
public interface Host extends StatusReporter {
  /** Invalidates the {@link Host} so that any subsequent call on any of the APIs will fail. */
  void invalidateHost();

  /** Informs the {@link Host} that an {@link Intent} has been received to bind to the app. */
  void onBindToApp(Intent intent);

  /** Indicates that the {@link CarHost} is now bound to the app. */
  void onCarAppBound();

  /** Indicates that a {@code onNewIntent} call has been dispatched to the app. */
  void onNewIntentDispatched();

  /** Returns the binder interface that the app can use to talk to this host. */
  IBinder getBinder();

  /** Sets the updated {@link TemplateContext} in this host instance. */
  void setTemplateContext(TemplateContext templateContext);
}
