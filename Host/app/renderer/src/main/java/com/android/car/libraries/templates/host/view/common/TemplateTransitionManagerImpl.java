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
package com.android.car.libraries.templates.host.view.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import com.android.car.libraries.apphost.view.TemplatePresenter;
import com.android.car.libraries.apphost.view.TemplateTransitionManager;
import com.android.car.libraries.templates.host.R;

/** Controls transitions between different templates. */
public class TemplateTransitionManagerImpl implements TemplateTransitionManager {
  private static final float TRANSITION_ALPHA_GONE = 0f;
  private static final float TRANSITION_ALPHA_VISIBLE = 1f;

  @Override
  public void transition(
      ViewGroup root, View surface, TemplatePresenter to, @Nullable TemplatePresenter from) {
    boolean toFullScreen = to.isFullScreen();
    boolean fromFullScreen = from == null || from.isFullScreen();

    if (toFullScreen || fromFullScreen) {
      transitionDefault(root, surface, to, from);
    } else {
      transitionBetweenHalfScreenTemplates(root, to);
    }
  }

  private static void transitionBetweenHalfScreenTemplates(ViewGroup root, TemplatePresenter to) {
    Scene endingScene = new Scene(root, to.getView());
    Transition transition =
        TransitionInflater.from(root.getContext())
            .inflateTransition(R.transition.half_screen_to_half_screen_transition);

    TransitionManager.go(endingScene, transition);
  }

  @SuppressLint("Recycle")
  private static void transitionDefault(
      ViewGroup root, View surface, TemplatePresenter to, @Nullable TemplatePresenter from) {
    @StyleableRes final int[] themeAttrs = {R.attr.templateUpdateAnimationDurationMilliseconds};
    TypedArray ta = root.getContext().obtainStyledAttributes(themeAttrs);
    long animationDurationMillis = ta.getInteger(0, 0);
    ta.recycle();

    if (to.usesSurface()) {
      surface.setVisibility(View.VISIBLE);
    }

    View toView = to.getView();
    View fromView = from == null ? null : from.getView();

    toView.setAlpha(TRANSITION_ALPHA_GONE);
    root.addView(toView);

    toView.animate().alpha(TRANSITION_ALPHA_VISIBLE).setDuration(animationDurationMillis);

    if (fromView != null) {
      fromView
          .animate()
          .alpha(TRANSITION_ALPHA_GONE)
          .setDuration(animationDurationMillis)
          .setListener(
              new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                  if (!to.usesSurface()) {
                    surface.setVisibility(View.GONE);
                  }
                  root.removeView(fromView);
                }
              });
    }
  }
}
