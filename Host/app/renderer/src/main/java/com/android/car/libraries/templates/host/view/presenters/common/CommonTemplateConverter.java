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

import android.content.Context;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import com.android.car.libraries.apphost.template.view.model.RowListWrapperTemplate;
import com.android.car.libraries.apphost.view.TemplateConverter;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;

/** A {@link TemplateConverter} for common templates. */
public class CommonTemplateConverter implements TemplateConverter {
  private static final CommonTemplateConverter INSTANCE = new CommonTemplateConverter();
  private static final ImmutableSet<Class<? extends Template>> SUPPORTED_TEMPLATES =
      ImmutableSet.of(PaneTemplate.class, ListTemplate.class);

  /** Returns an instance of CommonTemplateConverter */
  public static CommonTemplateConverter get() {
    return INSTANCE;
  }

  @Override
  public TemplateWrapper maybeConvertTemplate(Context context, TemplateWrapper templateWrapper) {
    Template template = templateWrapper.getTemplate();
    if (template instanceof ListTemplate || template instanceof PaneTemplate) {
      Template newTemplate =
          RowListWrapperTemplate.wrap(context, template, templateWrapper.isRefresh());

      TemplateWrapper newWrapper = TemplateWrapper.wrap(newTemplate, templateWrapper.getId());
      newWrapper.setRefresh(templateWrapper.isRefresh());
      newWrapper.setCurrentTaskStep(templateWrapper.getCurrentTaskStep());
      return newWrapper;
    }
    return templateWrapper;
  }

  @Override
  public Collection<Class<? extends Template>> getSupportedTemplates() {
    return SUPPORTED_TEMPLATES;
  }

  private CommonTemplateConverter() {}
}
