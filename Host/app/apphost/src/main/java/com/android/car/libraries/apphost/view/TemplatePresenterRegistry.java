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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A registry of {@link TemplatePresenterFactory} instances.
 *
 * <p>It is implemented as a {@link TemplatePresenterFactory} that wraps N other factories.
 */
public class TemplatePresenterRegistry implements TemplatePresenterFactory {
  private static final TemplatePresenterRegistry INSTANCE = new TemplatePresenterRegistry();

  private final Map<Class<? extends Template>, TemplatePresenterFactory> mRegistry =
      new HashMap<>();
  private final Set<Class<? extends Template>> mSupportedTemplates = new HashSet<>();

  /** Returns a singleton instance of the {@link TemplatePresenterRegistry}. */
  public static TemplatePresenterRegistry get() {
    return INSTANCE;
  }

  @Override
  @Nullable
  public TemplatePresenter createPresenter(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {

    TemplatePresenterFactory factory = mRegistry.get(templateWrapper.getTemplate().getClass());
    return factory == null ? null : factory.createPresenter(templateContext, templateWrapper);
  }

  @Override
  public Collection<Class<? extends Template>> getSupportedTemplates() {
    return Collections.unmodifiableCollection(mSupportedTemplates);
  }

  /** Registers the given {@link TemplatePresenterFactory}. */
  public void register(TemplatePresenterFactory factory) {
    for (Class<? extends Template> clazz : factory.getSupportedTemplates()) {
      mRegistry.put(clazz, factory);
      mSupportedTemplates.add(clazz);
    }
  }

  /** Clears the registry of any registered factories. */
  public void clear() {
    mRegistry.clear();
    mSupportedTemplates.clear();
  }

  private TemplatePresenterRegistry() {}
}
