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
package com.android.car.libraries.templates.host.view.presenters.navigation;

import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.PlaceListNavigationTemplate;
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.TemplatePresenter;
import com.android.car.libraries.apphost.view.TemplatePresenterFactory;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of a {@link TemplatePresenterFactory} for the production host, responsible for
 * providing {@link TemplatePresenter} instances for the set of templates the host supports.
 */
public class NavigationTemplatePresenterFactory implements TemplatePresenterFactory {
  private static final NavigationTemplatePresenterFactory sInstance =
      new NavigationTemplatePresenterFactory();
  private static final ImmutableSet<Class<? extends Template>> SUPPORTED_TEMPLATES =
      ImmutableSet.of(
          NavigationTemplate.class,
          PlaceListNavigationTemplate.class,
          RoutePreviewNavigationTemplate.class);

  /** Gets the singleton instance of{@link NavigationTemplatePresenterFactory}. */
  public static NavigationTemplatePresenterFactory get() {
    return sInstance;
  }

  @Override
  @Nullable
  public TemplatePresenter createPresenter(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    Template template = templateWrapper.getTemplate();

    Class<? extends Template> clazz = template.getClass();
    if (NavigationTemplate.class == clazz) {
      return NavigationTemplatePresenter.create(templateContext, templateWrapper);
    } else if (PlaceListNavigationTemplate.class == clazz) {
      return PlaceListNavigationTemplatePresenter.create(templateContext, templateWrapper);
    } else if (RoutePreviewNavigationTemplate.class == clazz) {
      return RoutePreviewNavigationTemplatePresenter.create(templateContext, templateWrapper);
    } else {
      L.w(
          LogTags.TEMPLATE,
          "Don't know how to create a presenter for template: %s",
          clazz.getSimpleName());
    }
    return null;
  }

  @Override
  public Collection<Class<? extends Template>> getSupportedTemplates() {
    return SUPPORTED_TEMPLATES;
  }

  private NavigationTemplatePresenterFactory() {}
}
