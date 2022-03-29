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
package com.android.car.libraries.apphost.distraction.checkers;

import android.content.Context;
import androidx.car.app.model.Template;

/**
 * Used for checking template of the specified type within the distraction framework to see if they
 * meet certain criteria (e.g. whether they are refreshes).
 *
 * @param <T> the type of template to check
 */
public interface TemplateChecker<T extends Template> {
  /** Returns whether the {@code newTemplate} is a refresh of the {@code oldTemplate}. */
  boolean isRefresh(T newTemplate, T oldTemplate);

  /**
   * Checks that the application has the required permissions for this template.
   *
   * @throws SecurityException if the application is missing any required permissions
   */
  default void checkPermissions(Context context, T newTemplate) {}
}
