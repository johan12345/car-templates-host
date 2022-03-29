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

import androidx.car.app.model.signin.InputSignInMethod;
import androidx.car.app.model.signin.SignInTemplate;
import java.util.Objects;

/** A {@link TemplateChecker} implementation for {@link SignInTemplate} */
public class SignInTemplateChecker implements TemplateChecker<SignInTemplate> {
  /**
   * A new template is considered a refresh of a previous one if:
   *
   * <ul>
   *   <li>The previous template is in a loading state, or
   *   <li>The template title and instructions have not changed and the input method is the same
   *       type.
   * </ul>
   */
  @Override
  public boolean isRefresh(SignInTemplate newTemplate, SignInTemplate oldTemplate) {
    if (oldTemplate.isLoading()) {
      // Transition from a previous loading state is allowed.
      return true;
    } else if (newTemplate.isLoading()) {
      // Transition to a loading state is not considered a refresh.
      return false;
    }
    boolean equalSignInMethods =
        Objects.equals(
            oldTemplate.getSignInMethod().getClass(), newTemplate.getSignInMethod().getClass());

    if (equalSignInMethods && oldTemplate.getSignInMethod() instanceof InputSignInMethod) {
      InputSignInMethod oldMethod = (InputSignInMethod) oldTemplate.getSignInMethod();
      InputSignInMethod newMethod = (InputSignInMethod) newTemplate.getSignInMethod();

      equalSignInMethods =
          oldMethod.getKeyboardType() == newMethod.getKeyboardType()
              && Objects.equals(oldMethod.getHint(), newMethod.getHint())
              && oldMethod.getInputType() == newMethod.getInputType();
    }

    return Objects.equals(oldTemplate.getTitle(), newTemplate.getTitle())
        && Objects.equals(oldTemplate.getInstructions(), newTemplate.getInstructions())
        && Objects.equals(oldTemplate.getAdditionalText(), newTemplate.getAdditionalText())
        && equalSignInMethods;
  }
}
