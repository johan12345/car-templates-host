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

import android.content.Context;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import java.util.Collection;

/**
 * Represents a type that can convert a {@link Template} instance into another type of template.
 *
 * <p>{@link TemplateConverter}s can be taken advantage to do N:1 conversions between template
 * types. This allows converting different templates that are similar but for which we want
 * different types in the client API to a common template that can be used internally be a single
 * presenter, thus avoiding duplicating the presenter code.
 */
public interface TemplateConverter {

  /** Changes the template instance in the template wrapper, if a mapping is necessary. */
  TemplateWrapper maybeConvertTemplate(Context context, TemplateWrapper templateWrapper);

  /** Returns the list of template types this converter supports. */
  Collection<Class<? extends Template>> getSupportedTemplates();
}
