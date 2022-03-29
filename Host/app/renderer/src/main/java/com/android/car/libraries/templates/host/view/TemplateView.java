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
package com.android.car.libraries.templates.host.view;

import static java.lang.Math.max;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Insets;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import com.android.car.libraries.apphost.common.DebugOverlayHandler.Observer;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.view.AbstractTemplateView;
import com.android.car.libraries.apphost.view.SurfaceProvider;
import com.android.car.libraries.apphost.view.SurfaceViewContainer;
import com.android.car.libraries.apphost.view.TemplateTransitionManager;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.common.TemplateTransitionManagerImpl;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A view that displays {@link Template}s.
 *
 * <p>The current template can be set with {@link #setTemplate} method.
 */
public class TemplateView extends AbstractTemplateView implements Observer {
  /**
   * The {@link SurfaceViewContainer} which holds the surface that 3p apps can use to render custom
   * content.
   */
  private SurfaceViewContainer mSurfaceViewContainer;

  /** The {@link FrameLayout} container which holds the currently set template. */
  private FrameLayout mTemplateContainer;

  /** The {@link TextView} container which holds debug overlay info. */
  private TextView mDebugOverlayText;

  /** See {@link AbstractTemplateView#getMinimumTopPadding()} */
  private final int mMinimumTopPadding;

  /** {@link TemplateTransitionManager} used by this {@link AbstractTemplateView} implementation */
  private final TemplateTransitionManager mTransitionManager = new TemplateTransitionManagerImpl();

  /** Creates a new instance of {@link TemplateView}. */
  @SuppressLint("InflateParams")
  public static TemplateView create(TemplateContext context) {
    TemplateView templateView =
        (TemplateView) LayoutInflater.from(context).inflate(R.layout.template_view, null);
    context.getDebugOverlayHandler().setObserver(templateView);
    return templateView;
  }

  public TemplateView(Context context) {
    this(context, null);
  }

  public TemplateView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TemplateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    final int[] themeAttrs = {R.attr.templateStatusBarMinimumTopPadding};
    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    mMinimumTopPadding = ta.getDimensionPixelSize(0, 0);
    ta.recycle();
  }

  /**
   * Returns a {@link SurfaceProvider} which can be used to retrieve the {@link
   * android.view.Surface} that 3p apps can use to draw custom content.
   */
  @Override
  public SurfaceProvider getSurfaceProvider() {
    return mSurfaceViewContainer;
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mSurfaceViewContainer = findViewById(R.id.surface_container);
    mTemplateContainer = findViewById(R.id.template_container);
    mDebugOverlayText = findViewById(R.id.debug_overlay);
  }

  @Override
  protected SurfaceViewContainer getSurfaceViewContainer() {
    return mSurfaceViewContainer;
  }

  @Override
  protected ViewGroup getTemplateContainer() {
    return mTemplateContainer;
  }

  @Override
  protected int getMinimumTopPadding() {
    return mMinimumTopPadding;
  }

  @Override
  protected TemplateTransitionManager getTransitionManager() {
    return mTransitionManager;
  }

  @Override
  public void setWindowInsets(WindowInsets windowInsets) {
    super.setWindowInsets(windowInsets);

    if (mDebugOverlayText == null) {
      return;
    }

    int leftInset;
    int topInset;
    int rightInset;
    int bottomInset;

    if (VERSION.SDK_INT >= VERSION_CODES.R) {
      Insets insets =
          windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
      leftInset = insets.left;
      topInset = insets.top;
      rightInset = insets.right;
      bottomInset = insets.bottom;

    } else {
      leftInset = windowInsets.getSystemWindowInsetLeft();
      topInset = windowInsets.getSystemWindowInsetTop();
      rightInset = windowInsets.getSystemWindowInsetRight();
      bottomInset = windowInsets.getSystemWindowInsetBottom();
    }

    FrameLayout.LayoutParams lp = (LayoutParams) mDebugOverlayText.getLayoutParams();
    lp.setMargins(leftInset, max(topInset, getMinimumTopPadding()), rightInset, bottomInset);
  }

  @Override
  public void setTemplate(TemplateWrapper templateWrapper) {
    super.setTemplate(templateWrapper);

    TemplateContext templateContext = getTemplateContext();
    if (templateContext != null) {
      templateContext.getDebugOverlayHandler().resetTemplateDebugOverlay(templateWrapper);
    }
  }

  @Override
  public void entriesUpdated() {
    TemplateContext templateContext = getTemplateContext();
    if (templateContext != null) {
      setDebugOverlayText(templateContext.getDebugOverlayHandler().getDebugOverlayText());
      setDebugOverlayVisibility(templateContext.getDebugOverlayHandler().isActive());
    }
  }

  private void setDebugOverlayText(CharSequence text) {
    if (mDebugOverlayText == null) {
      return;
    }

    mDebugOverlayText.setText(text);
  }

  private void setDebugOverlayVisibility(boolean isVisible) {
    if (mDebugOverlayText == null) {
      return;
    }

    mDebugOverlayText.setVisibility(isVisible ? View.VISIBLE : View.GONE);
  }
}
