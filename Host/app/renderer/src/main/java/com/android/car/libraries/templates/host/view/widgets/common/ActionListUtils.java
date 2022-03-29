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
package com.android.car.libraries.templates.host.view.widgets.common;

import androidx.car.app.model.Action;
import java.util.ArrayList;
import java.util.List;

/** Util class for {@link Action} lists. */
public final class ActionListUtils {
  /**
   * Returns whether the given object is a list of {@link Action}s or not.
   *
   * <p>An empty list is not considered an action list.
   */
  @SuppressWarnings("unchecked")
  public static boolean isActionList(Object obj) {
    if (!(obj instanceof List)) {
      return false;
    }

    List<Object> list = (List) obj;
    if (list.isEmpty()) {
      return false;
    }

    // Only check if the first element is an action. When we create a list of actions later, we
    // will
    // skip non-action elements.
    return list.get(0) instanceof Action;
  }

  /**
   * Returns a list of {@link Action}s if the given object is an action list, and an empty list if
   * it is not.
   */
  @SuppressWarnings("unchecked")
  public static List<Action> getActionList(Object obj) {
    List<Action> actionList = new ArrayList<>();
    if (obj instanceof List) {
      List<Object> list = (List) obj;
      for (Object element : list) {
        if (element instanceof Action) {
          actionList.add((Action) element);
        }
      }
    }

    return actionList;
  }

  private ActionListUtils() {}
}
