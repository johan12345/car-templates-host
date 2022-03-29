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

import androidx.annotation.Nullable;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.template.view.model.ActionStripWrapper;
import com.android.car.libraries.apphost.template.view.model.ActionWrapper;
import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Util class for {@link ActionStrip}. */
final class ActionStripUtils {
  /**
   * Validates the {@link ActionStrip} against the {@link ActionsConstraints} instance's required
   * types.
   *
   * @throws ValidationException if the action strip does not meet the required type constraints.
   */
  static void validateRequiredTypes(
      @Nullable ActionStripWrapper actionStrip, ActionsConstraints constraints)
      throws ValidationException {
    List<ActionWrapper> actions =
        actionStrip == null ? ImmutableList.of() : actionStrip.getActions();

    // Check for any missing required types.
    Set<Integer> requiredActionTypes = constraints.getRequiredActionTypes();
    if (!requiredActionTypes.isEmpty()) {
      Set<Integer> requiredTypes = new HashSet<>(requiredActionTypes);

      for (ActionWrapper action : actions) {
        requiredTypes.remove(action.get().getType());
      }

      if (!requiredTypes.isEmpty()) {
        StringBuilder missingTypeError = new StringBuilder();
        for (int type : requiredTypes) {
          missingTypeError.append(Action.typeToString(type)).append(";");
        }
        throw new ValidationException("Missing required action types: " + missingTypeError);
      }
    }
  }

  private ActionStripUtils() {}

  /** An exception thrown if the action strip validation fails. */
  static class ValidationException extends Exception {
    private ValidationException(String errorMessage) {
      super(errorMessage);
    }
  }
}
