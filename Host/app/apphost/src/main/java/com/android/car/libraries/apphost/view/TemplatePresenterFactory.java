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
package com.android.car.libraries.apphost.view;

import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import com.android.car.libraries.apphost.common.TemplateContext;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A provider of {@link TemplatePresenter} instances. */
public interface TemplatePresenterFactory {

  /**
   * Returns a new instance of a {@link TemplatePresenter} for the given template or {@code null} if
   * a presenter for the template type could not be found.
   */
  @Nullable TemplatePresenter createPresenter(
      TemplateContext templateContext, TemplateWrapper template);

  /** Returns the collection of templates this factory supports. */
  Collection<Class<? extends Template>> getSupportedTemplates();
}
