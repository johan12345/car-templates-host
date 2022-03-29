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
package com.android.car.libraries.templates.host.view.presenters.maps;

import android.content.Context;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import androidx.lifecycle.Lifecycle.State;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.TemplatePresenter;
import com.android.car.libraries.apphost.view.TemplatePresenterFactory;
import com.android.car.libraries.apphost.view.widget.map.AbstractMapViewContainer;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.di.MapViewContainerFactory;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.android.scopes.ServiceScoped;
import java.util.Collection;
import javax.inject.Inject;

/**
 * Implementation of a {@link TemplatePresenterFactory} for the production host, responsible for
 * providing {@link TemplatePresenter} instances for the set of templates the host supports.
 */
@ServiceScoped
public class MapsTemplatePresenterFactory implements TemplatePresenterFactory {

  private static final ImmutableSet<Class<? extends Template>> SUPPORTED_TEMPLATES =
      ImmutableSet.of(PlaceListMapTemplate.class);

  /** Boolean to trigger an one-time preload of MapView rendering code. */
  private static boolean sMapViewPreloaded;

  private final MapViewContainerFactory mMapViewContainerFactory;

  @Inject
  MapsTemplatePresenterFactory(MapViewContainerFactory mapViewContainerFactory) {
    mMapViewContainerFactory = mapViewContainerFactory;
  }

  @VisibleForTesting
  public static boolean isMapViewPreloaded() {
    return sMapViewPreloaded;
  }

  /**
   * Performance optimization: preload some of the MapView rendering code that requires one-time
   * static initialization.
   *
   * <p>This helps to speed up MapView being loaded when the {@link PlaceListMapTemplate} is
   * actually used.
   */
  @MainThread
  public void preloadMapView(Context context) {
    if (sMapViewPreloaded) {
      L.d(LogTags.TEMPLATE, "MapView previously preloaded. Skipping.");
      return;
    }

    AbstractMapViewContainer mapViewContainer =
        mMapViewContainerFactory.create(context, R.style.Theme_Template);
    // onCreate triggers the mapView.getMapAsync call to initialize the mapView.
    mapViewContainer.getLifecycleRegistry().setCurrentState(State.CREATED);
    // make sure the mapView is properly cleaned up to avoid any possible leaks.
    mapViewContainer.getLifecycleRegistry().setCurrentState(State.DESTROYED);
    sMapViewPreloaded = true;
  }

  @Override
  @Nullable
  public TemplatePresenter createPresenter(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    Class<? extends Template> clazz = templateWrapper.getTemplate().getClass();

    if (PlaceListMapTemplate.class == clazz) {
      return PlaceListMapTemplatePresenter.create(
          templateContext, templateWrapper, mMapViewContainerFactory);
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
}
