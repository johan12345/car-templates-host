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

import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import java.util.Objects;

/** A {@link TemplateChecker} implementation for {@link PaneTemplate} */
public class PaneTemplateChecker implements TemplateChecker<PaneTemplate> {
  /**
   * A new template is considered a refresh of the old if:
   *
   * <ul>
   *   <li>The previous template is in a loading state, or
   *   <li>The template title has not changed, and the number of rows and the string contents
   *       (title, texts, not counting spans) of each row between the previous and new {@link Pane}s
   *       have not changed.
   * </ul>
   */
  @Override
  public boolean isRefresh(PaneTemplate newTemplate, PaneTemplate oldTemplate) {
    Pane oldPane = oldTemplate.getPane();
    Pane newPane = newTemplate.getPane();
    if (oldPane.isLoading()) {
      return true;
    } else if (newPane.isLoading()) {
      return false;
    }

    if (!Objects.equals(oldTemplate.getTitle(), newTemplate.getTitle())) {
      return false;
    }

    return CheckerUtils.itemsHaveSameContent(oldPane.getRows(), newPane.getRows());
  }
}
