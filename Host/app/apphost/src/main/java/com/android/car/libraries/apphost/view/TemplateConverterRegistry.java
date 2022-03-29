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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A registry of {@link TemplateConverter} instances.
 *
 * <p>It is implemented as a {@link TemplateConverter} that wraps N other {@link
 * TemplateConverter}s.
 */
public class TemplateConverterRegistry implements TemplateConverter {
  private static final TemplateConverterRegistry INSTANCE = new TemplateConverterRegistry();

  private final Map<Class<? extends Template>, TemplateConverter> mRegistry = new HashMap<>();
  private final Set<Class<? extends Template>> mSupportedTemplates = new HashSet<>();

  /** Returns a singleton instance of the {@link TemplateConverterRegistry}. */
  public static TemplateConverterRegistry get() {
    return INSTANCE;
  }

  @Override
  public TemplateWrapper maybeConvertTemplate(Context context, TemplateWrapper templateWrapper) {
    TemplateConverter converter = mRegistry.get(templateWrapper.getTemplate().getClass());
    if (converter != null) {
      return converter.maybeConvertTemplate(context, templateWrapper);
    }
    return templateWrapper;
  }

  @Override
  public Collection<Class<? extends Template>> getSupportedTemplates() {
    return Collections.unmodifiableCollection(mSupportedTemplates);
  }

  /** Registers the given {@link TemplateConverter}. */
  public void register(TemplateConverter converter) {
    for (Class<? extends Template> clazz : converter.getSupportedTemplates()) {
      mRegistry.put(clazz, converter);
      mSupportedTemplates.add(clazz);
    }
  }

  private TemplateConverterRegistry() {}
}
