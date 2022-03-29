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

import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import androidx.lifecycle.LifecycleOwner;
import com.android.car.libraries.apphost.common.TemplateContext;

/**
 * A presenter is responsible for connecting a {@link Template} model with an Android {@link View}.
 *
 * <p>In <a href="https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel">MVVM</a>
 * terms, the {@link Template} is both the view as well as the data model, the {@link View} owned by
 * is the view, and {@link TemplatePresenter} is the controller which connects them together.
 *
 * <p>A presenter has a lifecycle, and extends {@link LifecycleOwner} to allow registering observers
 * to it.
 *
 * <p>Note the presenter's started and stopped states are dependent upon the parent lifecycle
 * owner's (e.g. the activity or fragment the template view is attached to). This means for example
 * that if the owner is not started at the time it is created, the presenter won't be started
 * either. Conversely, if the parent is stopped and then started, the presenter will also change
 * states accordingly.
 */
public interface TemplatePresenter extends LifecycleOwner {

  /**
   * Sets the {@link Template} instance for this presenter.
   *
   * <p>If the new template is of the same type as the one currently set, implementations should try
   * to diff the data to apply a minimal view update when it would otherwise cause undesirable
   * performance or visible UI artifacts. For example, when updating a list view, the diffing logic
   * should detect which specific items changed and only update those rather than doing a full
   * update of all items, which is important if using a {@link
   * androidx.recyclerview.widget.RecyclerView} that may have special animations for the different
   * adapter update operations.
   */
  void setTemplate(TemplateWrapper templateWrapper);

  /** Returns the {@link Template} instance set in the template wrapper. */
  Template getTemplate();

  /** Returns the {@link TemplateWrapper} instance set in the presenter. */
  TemplateWrapper getTemplateWrapper();

  /** Returns the {@link TemplateContext} set in the presenter. */
  TemplateContext getTemplateContext();

  /**
   * Returns the {@link View} instance representing the UI to display for the currently set {@link
   * Template}.
   */
  View getView();

  /** Applies the given {@code windowInsets} to the appropriate views. */
  void applyWindowInsets(WindowInsets windowInsets, int minimumTopPadding);

  /** Sets the default focus of the presenter's UI in the rotary or touchpad mode. */
  boolean setDefaultFocus();

  /**
   * Called when a key event was received while the presenter is currently visible.
   *
   * @return {@code true} if the presenter handled the key event, otherwise {@code false}.
   */
  boolean onKeyUp(int keyCode, KeyEvent keyEvent);

  /**
   * Called when the view tree is about to be drawn. At this point, all views in the tree have been
   * measured and given a frame. Presenters can use this to adjust their scroll bounds or even to
   * request a new layout before drawing occurs.
   */
  boolean onPreDraw();

  /**
   * Notifies that the presenter instance has been created and its view is about to be added to the
   * template view's hierarchy as the currently visible one.
   *
   * <p>Presenters can implement any initialization logic in here.
   */
  void onCreate();

  /**
   * Notifies that the presenter instance has been destroyed, and removed from the template view's
   * hierarchy.
   *
   * <p>Presenters can implement any cleanup logic in here.
   */
  void onDestroy();

  /**
   * Notifies that the presenter is visible to the user.
   *
   * <p>Presenters can use method to implement any logic that was stopped during {@link #onStop}.
   */
  void onStart();

  /**
   * Notifies that the presenter is not visible to the user.
   *
   * <p>Presenters can use method to stop any logic that is not needed when the presenter is not
   * visible, e.g. to conserve resources.
   */
  void onStop();

  /** Notifies that the presenter is actively running. */
  void onResume();

  /** Notifies that the presenter is not actively running but still visible. */
  void onPause();

  /** Returns whether this presenter handles its own template change animations. */
  boolean handlesTemplateChangeAnimation();

  /**
   * Returns whether this presenter is considered a full screen template.
   *
   * <p>Map and navigation templates are not full screen as they leave the space for map to be
   * shown, and the UI elements only cover a smaller portion of the car screen.
   */
  boolean isFullScreen();

  /**
   * Returns whether this presenter uses the surface accessible via a {@link
   * androidx.car.app.SurfaceContainer}.
   */
  boolean usesSurface();
}
