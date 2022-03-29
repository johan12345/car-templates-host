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

import androidx.car.app.model.MessageTemplate;
import java.util.Objects;

/** A {@link TemplateChecker} implementation for {@link MessageTemplate} */
public class MessageTemplateChecker implements TemplateChecker<MessageTemplate> {
  /**
   * A new template is considered a refresh of a previous one if:
   *
   * <ul>
   *   <li>The previous template is in a loading state, or
   *   <li>The template title and messages have not changed.
   * </ul>
   */
  @Override
  public boolean isRefresh(MessageTemplate newTemplate, MessageTemplate oldTemplate) {
    if (oldTemplate.isLoading()) {
      // Transition from a previous loading state is allowed.
      return true;
    } else if (newTemplate.isLoading()) {
      // Transition to a loading state is not considered a refresh.
      return false;
    }

    return Objects.equals(oldTemplate.getTitle(), newTemplate.getTitle())
        && Objects.equals(oldTemplate.getDebugMessage(), newTemplate.getDebugMessage())
        && Objects.equals(oldTemplate.getMessage(), newTemplate.getMessage());
  }
}
