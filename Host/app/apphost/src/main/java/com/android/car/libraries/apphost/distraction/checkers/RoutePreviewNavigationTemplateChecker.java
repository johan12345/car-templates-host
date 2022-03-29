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
import androidx.car.app.CarAppPermission;
import androidx.car.app.model.ItemList;
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate;
import java.util.Objects;

/** A {@link TemplateChecker} implementation for {@link RoutePreviewNavigationTemplate} */
public class RoutePreviewNavigationTemplateChecker
    implements TemplateChecker<RoutePreviewNavigationTemplate> {
  /**
   * A new template is considered a refresh of the old if:
   *
   * <ul>
   *   <li>The previous template is in a loading state, or
   *   <li>The template title has not changed, and the number of rows and the string contents
   *       (title, texts, not counting spans) of each row between the previous and new {@link
   *       ItemList}s have not changed.
   * </ul>
   */
  @Override
  public boolean isRefresh(
      RoutePreviewNavigationTemplate newTemplate, RoutePreviewNavigationTemplate oldTemplate) {
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

    ItemList oldList = oldTemplate.getItemList();
    ItemList newList = newTemplate.getItemList();
    if (oldList != null && newList != null) {
      return CheckerUtils.itemsHaveSameContent(oldList.getItems(), newList.getItems());
    }

    return true;
  }

  @Override
  public void checkPermissions(Context context, RoutePreviewNavigationTemplate newTemplate) {
    CarAppPermission.checkHasLibraryPermission(context, CarAppPermission.NAVIGATION_TEMPLATES);
  }
}
