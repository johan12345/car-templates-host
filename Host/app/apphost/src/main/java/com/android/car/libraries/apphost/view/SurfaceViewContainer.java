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
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.annotation.VisibleForTesting;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Container of the {@link SurfaceView} which 3p apps can use to render custom content. For example,
 * navigation apps can use it to draw a map.
 */
public class SurfaceViewContainer extends SurfaceView implements SurfaceProvider {
  /** A listener for changes to {@link SurfaceView}. */
  @Nullable private SurfaceProviderListener mListener;

  /** Indicates whether the surface is ready for use. */
  private boolean mIsSurfaceReady;

  private final SurfaceHolder.Callback mSurfaceHolderCallback =
      new SurfaceHolder.Callback() {
        @SuppressWarnings("nullness") // suppress under initialization warning for this
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
          mIsSurfaceReady = true;
          notifySurfaceCreated();
        }

        @SuppressWarnings("nullness") // suppress under initialization warning for this
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
          notifySurfaceChanged();
        }

        @SuppressWarnings("nullness") // suppress under initialization warning for this
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
          mIsSurfaceReady = false;
          notifySurfaceDestroyed();
        }
      };

  /** Returns an instance of {@link SurfaceViewContainer}. */
  public SurfaceViewContainer(Context context) {
    this(context, null);
  }

  /**
   * Returns an instance of {@link SurfaceViewContainer} with the given attribute set.
   *
   * @see android.view.View#View(Context, AttributeSet)
   */
  public SurfaceViewContainer(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  /**
   * Returns an instance of {@link SurfaceViewContainer} with the given attribute set and default
   * style attribute.
   *
   * @see android.view.View#View(Context, AttributeSet, int)
   */
  public SurfaceViewContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  @Nullable
  public Surface getSurface() {
    if (!mIsSurfaceReady) {
      L.v(LogTags.TEMPLATE, "Surface is not ready for use");
      return null;
    }

    return getHolder().getSurface();
  }

  @Override
  public int getDpi() {
    if (!mIsSurfaceReady) {
      return 0;
    }

    return getResources().getDisplayMetrics().densityDpi;
  }

  @Override
  public void setListener(@Nullable SurfaceProviderListener listener) {
    mListener = listener;
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    getHolder().addCallback(mSurfaceHolderCallback);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    getHolder().removeCallback(mSurfaceHolderCallback);
  }

  /** Returns whether the surface is ready to be used. */
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public boolean isSurfaceReady() {
    return mIsSurfaceReady;
  }

  private void notifySurfaceCreated() {
    if (mListener != null) {
      mListener.onSurfaceCreated();
    }
  }

  private void notifySurfaceChanged() {
    if (mListener != null) {
      mListener.onSurfaceChanged();
    }
  }

  private void notifySurfaceDestroyed() {
    if (mListener != null) {
      mListener.onSurfaceDestroyed();
    }
  }
}
