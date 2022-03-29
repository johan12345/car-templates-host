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

import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Toggle;
import java.util.Objects;

/** A {@link TemplateChecker} implementation for {@link GridTemplate} */
public class GridTemplateChecker implements TemplateChecker<GridTemplate> {
  /**
   * A new template is considered a refresh of the old if:
   *
   * <ul>
   *   <li>The previous template is in a loading state, or
   *   <li>The template title has not changed, and the number of grid items and the string contents
   *       (title, texts) of each grid item have not changed.
   *   <li>For grid items that contain a {@link Toggle}, updates to the title, text and image are
   *       also allowed if the toggle state has changed between the previous and new templates.
   * </ul>
   */
  @Override
  public boolean isRefresh(GridTemplate newTemplate, GridTemplate oldTemplate) {
    if (oldTemplate.isLoading()) {
      // Transition from a previous loading state is allowed.
      return true;
    } else if (newTemplate.isLoading()) {
      // Transition to a loading state is disallowed.
      return false;
    }
    if (!Objects.equals(oldTemplate.getTitle(), newTemplate.getTitle())) {
      return false;
    }

    ItemList oldList = oldTemplate.getSingleList();
    ItemList newList = newTemplate.getSingleList();
    if (oldList != null && newList != null) {
      return CheckerUtils.itemsHaveSameContent(oldList.getItems(), newList.getItems());
    }

    return true;
  }
}
