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

import androidx.car.app.model.Item;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.SectionedItemList;
import androidx.car.app.model.Toggle;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A {@link TemplateChecker} implementation for {@link ListTemplate} */
public class ListTemplateChecker implements TemplateChecker<ListTemplate> {
  /**
   * A new template is considered a refresh of the old if:
   *
   * <ul>
   *   <li>The previous template is in a loading state, or
   *   <li>The template title has not changed, and the {@link ItemList} structure between the
   *       templates have not changed. This means that if the previous template has multiple {@link
   *       ItemList} sections, the new template must have the same number of sections with the same
   *       headers. Further, the number of rows and the string contents (title, texts, not counting
   *       spans) of each row must not have changed.
   *   <li>For rows that contain a {@link Toggle}, updates to the title or texts are also allowed if
   *       the toggle state has changed between the previous and new templates.
   * </ul>
   */
  @Override
  public boolean isRefresh(ListTemplate newTemplate, ListTemplate oldTemplate) {
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
    } else {
      List<SectionedItemList> oldSectionedList = oldTemplate.getSectionedLists();
      List<SectionedItemList> newSectionedList = newTemplate.getSectionedLists();

      if (oldSectionedList.size() != newSectionedList.size()) {
        return false;
      }

      for (int i = 0; i < newSectionedList.size(); i++) {
        SectionedItemList newSection = newSectionedList.get(i);
        SectionedItemList oldSection = oldSectionedList.get(i);

        ItemList oldItemList = oldSection.getItemList();
        ItemList newItemList = newSection.getItemList();
        List<Item> oldSubList =
            oldItemList == null ? Collections.emptyList() : oldItemList.getItems();
        List<Item> newSubList =
            newItemList == null ? Collections.emptyList() : newItemList.getItems();
        if (!Objects.equals(newSection.getHeader(), oldSection.getHeader())
            || !CheckerUtils.itemsHaveSameContent(oldSubList, newSubList)) {
          return false;
        }
      }
    }

    return true;
  }
}
