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

import android.content.ComponentName;
import androidx.car.app.model.TemplateWrapper;
import com.android.car.libraries.apphost.view.SurfaceProvider;

/**
 * Implements the UI operations that a client app may trigger in the UI.
 *
 * <p>This is normally implemented by a backing context such as an activity or fragment that a given
 * template host is connected to.
 *
 * <p>The methods in this interface are tagged with an {@code appName} parameter that indicates
 * which application the operation is intended to. The controller must drop the call if it is not
 * currently connected to that app.
 *
 * <p>These methods can also drop the calls, or return {@code null} if the backing context is not
 * available, e.g. because it's been collected by the GC or explicitly cleared.
 */
public interface UIController {
  /** Sets the {@link TemplateWrapper} to display in the given app's view. */
  void setTemplate(ComponentName appName, TemplateWrapper template);

  /** Returns the {@link SurfaceProvider} for the given app. */
  SurfaceProvider getSurfaceProvider(ComponentName appName);
}
