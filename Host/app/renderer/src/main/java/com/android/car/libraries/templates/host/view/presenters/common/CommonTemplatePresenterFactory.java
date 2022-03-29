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
package com.android.car.libraries.templates.host.view.presenters.common;

import androidx.annotation.Nullable;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.LongMessageTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import androidx.car.app.model.signin.SignInTemplate;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.template.view.model.RowListWrapperTemplate;
import com.android.car.libraries.apphost.view.TemplatePresenter;
import com.android.car.libraries.apphost.view.TemplatePresenterFactory;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;

/**
 * Implementation of a {@link TemplatePresenterFactory} for the production host, responsible for
 * providing {@link TemplatePresenter} instances for the set of templates the host supports.
 */
public class CommonTemplatePresenterFactory implements TemplatePresenterFactory {
  private static final CommonTemplatePresenterFactory INSTANCE =
      new CommonTemplatePresenterFactory();
  private static final ImmutableSet<Class<? extends Template>> SUPPORTED_TEMPLATES =
      ImmutableSet.of(
          GridTemplate.class,
          LongMessageTemplate.class,
          MessageTemplate.class,
          RowListWrapperTemplate.class,
          SearchTemplate.class,
          SignInTemplate.class);

  /** Returns an instance of CommonTemplatePresenterFactory */
  public static CommonTemplatePresenterFactory get() {
    return INSTANCE;
  }

  @Override
  @Nullable
  public TemplatePresenter createPresenter(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    Class<? extends Template> clazz = templateWrapper.getTemplate().getClass();
    if (GridTemplate.class == clazz) {
      return GridTemplatePresenter.create(templateContext, templateWrapper);
    } else if (MessageTemplate.class == clazz) {
      return MessageTemplatePresenter.create(templateContext, templateWrapper);
    } else if (LongMessageTemplate.class == clazz) {
      return LongMessageTemplatePresenter.create(templateContext, templateWrapper);
    } else if (RowListWrapperTemplate.class == clazz) {
      return RowListWrapperTemplatePresenter.create(templateContext, templateWrapper);
    } else if (SearchTemplate.class == clazz) {
      return SearchTemplatePresenter.create(templateContext, templateWrapper);
    } else if (SignInTemplate.class == clazz) {
      return SignInTemplatePresenter.create(templateContext, templateWrapper);
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

  private CommonTemplatePresenterFactory() {}
}
