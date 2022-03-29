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
package com.android.car.libraries.templates.host.view.widgets.common;

import static com.android.car.libraries.apphost.view.common.ImageUtils.SCALE_CENTER_XY_INSIDE;
import static com.android.car.libraries.apphost.view.common.ImageUtils.SCALE_FIT_CENTER;
import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.PlaceMarker;
import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.apphost.common.CommonUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.common.ImageUtils;
import com.android.car.libraries.apphost.view.common.ImageViewParams;
import com.android.car.libraries.templates.host.R;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** A factory of bitmaps to be used as map markers in different templates. */
public class MarkerFactory {
  // Cache the default anchor so that we don't have to draw if users did not customize.
  private final Bitmap mDefaultAnchorBitmap;

  // Cache the default marker so that we don't have to draw if users did not customize.
  private final Bitmap mDefaultMarkerBitmap;

  // Mask for clipping an image within bounds. A marker image's draw area is slightly bigger than
  // icons with rounded corners.
  @MonotonicNonNull private Bitmap mMarkerImageMask;
  private final Paint mMarkerImageMaskPaint;

  // Default path used for drawing the standard-size map marker.
  private final Path mDefaultMarkerPath;
  private final MarkerAppearance mAppearance;

  /** Create a MarkerFactory */
  public static MarkerFactory create(Context context, MarkerAppearance appearance) {
    return new MarkerFactory(context, appearance);
  }

  /** Returns a map marker bitmap with the given {@link PlaceMarker} configuration. */
  // TODO(b/144920236): cache and reuse bitmaps when applicable.
  public Bitmap createPoiMarkerBitmap(
      TemplateContext templateContext, @Nullable PlaceMarker marker) {
    if (marker == null) {
      return mDefaultMarkerBitmap;
    }

    // Use the dark variant for background color.
    @ColorInt int markerColor = resolveMarkerColor(templateContext, marker);
    boolean useDefaultMarker = markerColor == mAppearance.mMarkerDefaultBackgroundColor;

    CarText label = marker.getLabel();
    CarIcon icon = marker.getIcon();
    if (label == null && icon == null && useDefaultMarker) {
      return mDefaultMarkerBitmap;
    }

    boolean needWideMarker = false;
    Bitmap markerBitmap;
    String labelString = label == null ? null : label.toString();
    if (icon == null && labelString != null) {
      // If we need to draw a label, check if we need to draw a wider marker to fit the text.
      Rect bounds = new Rect();
      mAppearance.mDefaultTextPaint.getTextBounds(labelString, 0, labelString.length(), bounds);

      if (bounds.width() > mAppearance.mMarkerSize - mAppearance.mTextHorizontalPadding * 2) {
        needWideMarker = true;
        markerBitmap =
            Bitmap.createBitmap(
                bounds.width() + mAppearance.mTextHorizontalPadding * 2,
                mAppearance.mMarkerSize + mAppearance.mMarkerPointerHeight,
                Config.ARGB_8888);
        markerBitmap.setDensity(mAppearance.mDensityDpi);
      } else {
        markerBitmap = Bitmap.createBitmap(mDefaultMarkerBitmap);
      }
    } else {
      markerBitmap = Bitmap.createBitmap(mDefaultMarkerBitmap);
    }

    Canvas canvas = new Canvas(markerBitmap);
    if (needWideMarker || !useDefaultMarker) {
      drawMarker(
          canvas,
          mAppearance,
          markerColor,
          useDefaultMarker
              ? mAppearance.mMarkerDefaultBorderColor
              : mAppearance.mMarkerCustomBorderColor);
    }

    Bitmap contentBitmap = getContentForMapMarker(templateContext, marker, useDefaultMarker);
    if (contentBitmap != null) {
      canvas.drawBitmap(
          contentBitmap,
          // Width may have been adjusted so we use the bitmap's width as source of truth.
          (markerBitmap.getWidth() - contentBitmap.getWidth()) / 2f,
          (mAppearance.mMarkerSize - contentBitmap.getHeight()) / 2f,
          mAppearance.mDefaultTextPaint);
    }

    return markerBitmap;
  }

  /**
   * Returns the bitmap representing the content (icon of text) that should appear within a marker
   * in the map view, or {code null} if the marker's content is not specified.
   */
  @Nullable
  private Bitmap getContentForMapMarker(
      TemplateContext templateContext, PlaceMarker marker, boolean hasDefaultBackground) {
    CarText label = marker.getLabel();
    String labelString = label != null ? label.toString() : null;
    CarIcon icon = marker.getIcon();
    Bitmap contentBitmap = null;

    // The icon value takes precedence over the label if both are set.
    if (icon != null) {
      contentBitmap =
          getIconBitmap(templateContext, marker, icon, CommonUtils.isDarkMode(templateContext));
    } else if (labelString != null && !labelString.isEmpty()) {
      contentBitmap =
          ImageUtils.getBitmapFromString(
              labelString,
              hasDefaultBackground
                  ? mAppearance.mDefaultTextPaint
                  : mAppearance.mCustomBackgroundTextPaint);
    }

    if (contentBitmap != null) {
      contentBitmap.setDensity(mAppearance.mDensityDpi);
    }

    return contentBitmap;
  }

  /**
   * Returns the bitmap representing the content (icon of text) of the given marker, or {code null}
   * if the marker's content is not specified.
   */
  @Nullable
  public Bitmap getContentForListMarker(TemplateContext templateContext, PlaceMarker marker) {
    CarText label = marker.getLabel();
    String labelString = label != null ? label.toString() : null;
    CarIcon icon = marker.getIcon();
    Bitmap contentBitmap = null;

    // The icon value takes precedence over the label if both are set.
    if (icon != null) {
      // We always use the light-variant tint for list marker because the card background is
      // dark.
      contentBitmap = getIconBitmap(templateContext, marker, icon, /* isDark= */ false);
    } else if (labelString != null && !labelString.isEmpty()) {
      // We use the light-variant color for the text in the list.
      int resolvedColor =
          CarColorUtils.resolveColor(
              templateContext,
              marker.getColor(),

              // The background of the card is dark so use the light variant for the
              // text color.
              /* isDark= */ false,
              mAppearance.mDefaultTextPaint.getColor(),
              CarColorConstraints.UNCONSTRAINED);
      Paint paint;
      if (resolvedColor == mAppearance.mCustomBackgroundTextPaint.getColor()) {
        paint = mAppearance.mCustomBackgroundTextPaint;
      } else {
        paint = new Paint(mAppearance.mCustomBackgroundTextPaint);
        paint.setColor(resolvedColor);
      }

      Resources resources = templateContext.getResources();
      Drawable bitmap =
          new BitmapDrawable(resources, ImageUtils.getBitmapFromString(labelString, paint));
      contentBitmap =
          ImageUtils.getBitmapFromDrawable(
              bitmap,
              mAppearance.mListIconSize,
              mAppearance.mListIconSize,
              resources.getDisplayMetrics().densityDpi,
              SCALE_CENTER_XY_INSIDE);
    }

    if (contentBitmap != null) {
      contentBitmap.setDensity(mAppearance.mDensityDpi);
    }

    return contentBitmap;
  }

  @Nullable
  private Bitmap getIconBitmap(
      TemplateContext templateContext, PlaceMarker marker, CarIcon icon, boolean isDark) {
    Bitmap contentBitmap;
    boolean isImage = isMarkerImage(marker);
    int bitmapSize = isImage ? mAppearance.mImageSize : mAppearance.mIconSize;
    ImageViewParams imageParams =
        ImageViewParams.builder()
            .setDefaultTint(mAppearance.mDefaultIconTint)
            .setForceTinting(!isImage)
            .setIsDark(isDark)
            .build();
    contentBitmap =
        ImageUtils.getBitmapFromIcon(
            templateContext, icon, bitmapSize, bitmapSize, imageParams, SCALE_FIT_CENTER);

    if (contentBitmap == null) {
      L.e(LogTags.TEMPLATE, "Failed to get bitmap for marker: %s", marker);
    } else if (isImage) {
      // Apply masking to get the rounded corner effect.
      Bitmap maskedImage = Bitmap.createBitmap(bitmapSize, bitmapSize, Config.ARGB_8888);
      maskedImage.setDensity(mAppearance.mDensityDpi);

      Canvas maskedCanvas = new Canvas(maskedImage);
      maskedCanvas.drawBitmap(getOrCreateMarkerImageMask(mAppearance), 0, 0, null);
      maskedCanvas.drawBitmap(contentBitmap, 0, 0, mMarkerImageMaskPaint);
      contentBitmap = maskedImage;
    }
    return contentBitmap;
  }

  /** Returns an {@link Bitmap} which has been adjusted for a given background color. */
  public Bitmap getAnchorBitmap(TemplateContext templateContext, @Nullable CarColor background) {
    if (background == null) {
      return mDefaultAnchorBitmap;
    }

    @ColorInt
    int resolvedBackground =
        CarColorUtils.resolveColor(
            templateContext,
            background,
            // Use the dark-variant in day mode, and vice versa.
            /* isDark= */ !CommonUtils.isDarkMode(templateContext),
            mAppearance.mAnchorDefaultBackgroundColor,
            CarColorConstraints.UNCONSTRAINED);
    if (resolvedBackground == mAppearance.mAnchorDefaultBackgroundColor) {
      return mDefaultAnchorBitmap;
    }

    return createAnchorBitmap(templateContext, resolvedBackground, mAppearance);
  }

  /** Draw a rounded-corner marker with a pointer at the bottom center. */
  private void drawMarker(
      @UnknownInitialization MarkerFactory this,
      Canvas canvas,
      MarkerAppearance appearance,
      @ColorInt int backgroundColor,
      @ColorInt int borderColor) {
    int defaultMarkerWidth = appearance.mMarkerSize;
    int defaultMarkerHeight = appearance.mMarkerSize + appearance.mMarkerPointerHeight;

    Path markerPath =
        canvas.getWidth() == defaultMarkerWidth
            ? mDefaultMarkerPath
            : createMarkerPath(canvas.getWidth(), defaultMarkerHeight, appearance);
    if (markerPath == null) {
      return;
    }

    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(backgroundColor);
    canvas.drawPath(markerPath, paint);

    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(borderColor);
    paint.setStrokeWidth(appearance.mMarkerStroke);
    paint.setStrokeCap(Cap.ROUND);
    canvas.drawPath(markerPath, paint);
  }

  private Bitmap createDefaultMarkerBitmap(
      @UnderInitialization MarkerFactory this, MarkerAppearance appearance) {
    Bitmap markerBitmap =
        Bitmap.createBitmap(
            appearance.mMarkerSize,
            appearance.mMarkerSize + appearance.mMarkerPointerHeight,
            Config.ARGB_8888);
    markerBitmap.setDensity(appearance.mDensityDpi);

    Canvas canvas = new Canvas(markerBitmap);
    drawMarker(
        canvas,
        appearance,
        appearance.mMarkerDefaultBackgroundColor,
        appearance.mMarkerDefaultBorderColor);

    return markerBitmap;
  }

  private Bitmap createAnchorBitmap(
      @UnknownInitialization MarkerFactory this,
      Context context,
      @ColorInt int backgroundColor,
      MarkerAppearance appearance) {
    Resources resources = context.getResources();

    Drawable markerBackground = resources.getDrawable(R.drawable.anchor_marker);
    markerBackground.setBounds(
        0, 0, markerBackground.getIntrinsicWidth(), markerBackground.getIntrinsicHeight());
    markerBackground.setColorFilter(backgroundColor, PorterDuff.Mode.SRC_IN);

    Drawable markerBorder = resources.getDrawable(R.drawable.anchor_marker_border);
    markerBorder.setBounds(
        0, 0, markerBorder.getIntrinsicWidth(), markerBorder.getIntrinsicHeight());
    markerBorder.setColorFilter(appearance.mAnchorBorderColor, PorterDuff.Mode.SRC_IN);

    Drawable markerDot = resources.getDrawable(R.drawable.anchor_marker_circle);
    markerDot.setBounds(0, 0, markerDot.getIntrinsicWidth(), markerDot.getIntrinsicHeight());
    markerDot.setColorFilter(appearance.mAnchorDotColor, PorterDuff.Mode.SRC_IN);

    Bitmap bitmap =
        Bitmap.createBitmap(
            markerBackground.getIntrinsicWidth(),
            markerBackground.getIntrinsicHeight(),
            Config.ARGB_8888);
    bitmap.setDensity(appearance.mDensityDpi);

    Canvas canvas = new Canvas(bitmap);
    markerBackground.draw(canvas);
    markerBorder.draw(canvas);
    markerDot.draw(canvas);

    return bitmap;
  }

  private Bitmap getOrCreateMarkerImageMask(MarkerAppearance appearance) {
    if (mMarkerImageMask != null) {
      return mMarkerImageMask;
    }

    mMarkerImageMask =
        Bitmap.createBitmap(appearance.mImageSize, appearance.mImageSize, Config.ALPHA_8);
    mMarkerImageMask.setDensity(appearance.mDensityDpi);

    Canvas canvas = new Canvas(mMarkerImageMask);
    canvas.drawRoundRect(
        0,
        0,
        appearance.mImageSize,
        appearance.mImageSize,
        appearance.mImageCornerRadius,
        appearance.mImageCornerRadius,
        new Paint());

    return mMarkerImageMask;
  }

  /**
   * Returns the marker color that should be used based on the {@link PlaceMarker}'s configuration.
   *
   * <p>If the marker is of type {@link PlaceMarker#TYPE_IMAGE}, then the default color will be
   * used. Otherwise, we resolve the color provided via the marker to what is defined in our theme.
   */
  @ColorInt
  private int resolveMarkerColor(TemplateContext templateContext, PlaceMarker marker) {
    // We do not support rendering a background color for images.
    if (marker.getIconType() == PlaceMarker.TYPE_IMAGE) {
      return mAppearance.mMarkerDefaultBackgroundColor;
    }

    @ColorInt
    int resolvedColor =
        CarColorUtils.resolveColor(
            templateContext,
            marker.getColor(),
            // Use the dark-variant in day mode for better contrast with the
            // light-colored map, and
            // vice versa.
            /* isDark= */ !CommonUtils.isDarkMode(templateContext),
            mAppearance.mMarkerDefaultBackgroundColor,
            CarColorConstraints.UNCONSTRAINED);
    return resolvedColor;
  }

  private MarkerFactory(Context context, MarkerAppearance appearance) {
    mAppearance = appearance;

    mDefaultMarkerPath =
        createMarkerPath(
            appearance.mMarkerSize,
            appearance.mMarkerSize + appearance.mMarkerPointerHeight,
            appearance);
    mDefaultMarkerBitmap = createDefaultMarkerBitmap(appearance);
    mDefaultAnchorBitmap =
        createAnchorBitmap(context, appearance.mAnchorDefaultBackgroundColor, appearance);
    mDefaultAnchorBitmap.setDensity(appearance.mDensityDpi);

    mMarkerImageMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mMarkerImageMaskPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
  }

  /**
   * Create a {@link Path} representing a map marker that fits within the input width and height.
   */
  private static Path createMarkerPath(int width, int height, MarkerAppearance appearance) {
    // Actual draw region needs to account for the stroke size.
    // At the end, we offset the drawing by (markerStroke / 2) in both x and y to center it.
    int drawWidth = width - appearance.mMarkerStroke;
    int drawHeight = height - appearance.mMarkerStroke;
    int cornerRadius = appearance.mMarkerCornerRadius;
    int cornerDiameter = cornerRadius * 2;
    float halfStroke = appearance.mMarkerStroke / 2f;

    // Start from the pointer tip and draw clockwise.
    float startX = drawWidth / 2f;
    float startY = drawHeight;

    // Bottom of the rectangular region of the marker.
    float rectBottom = startY - appearance.mMarkerPointerHeight;
    float pointerHalfWidth = appearance.mMarkerPointerWidth / 2f;

    Path path = new Path();
    RectF cornerRect = new RectF();

    path.moveTo(startX, startY);
    path.lineTo(startX - pointerHalfWidth, rectBottom);
    path.lineTo(appearance.mMarkerCornerRadius, rectBottom);
    cornerRect.set(0, rectBottom - cornerDiameter, cornerDiameter, rectBottom);
    path.arcTo(cornerRect, 90, 90, false);

    path.lineTo(0, cornerRadius);
    cornerRect = new RectF(0, 0, cornerDiameter, cornerDiameter);
    path.arcTo(cornerRect, 180, 90, false);

    path.lineTo(drawWidth - cornerRadius, 0);
    cornerRect.set(drawWidth - cornerDiameter, 0, drawWidth, cornerDiameter);
    path.arcTo(cornerRect, 270, 90, false);

    path.lineTo(drawWidth, rectBottom - cornerRadius);
    cornerRect.set(drawWidth - cornerDiameter, rectBottom - cornerDiameter, drawWidth, rectBottom);
    path.arcTo(cornerRect, 0, 90, false);

    path.lineTo(startX + pointerHalfWidth, rectBottom);
    path.close();

    // Offset the path to accommodate the stroke so the drawing is centered within the region.
    path.offset(halfStroke, halfStroke);

    return path;
  }

  private static boolean isMarkerImage(PlaceMarker marker) {
    return marker.getIconType() == PlaceMarker.TYPE_IMAGE;
  }

  /** Contains the attributes that define the marker's appearance. */
  public static class MarkerAppearance {
    @ColorInt private final int mMarkerDefaultBackgroundColor;
    @ColorInt private final int mMarkerDefaultBorderColor;
    @ColorInt private final int mMarkerCustomBorderColor;
    private final int mMarkerSize;
    private final int mMarkerPointerWidth;
    private final int mMarkerPointerHeight;
    private final int mMarkerStroke;
    private final int mMarkerCornerRadius;
    @ColorInt private final int mAnchorDefaultBackgroundColor;
    @ColorInt private final int mAnchorBorderColor;
    @ColorInt private final int mAnchorDotColor;

    @ColorInt private final int mDefaultIconTint;

    private final int mIconSize;
    private final int mTextHorizontalPadding;
    private final int mImageSize;
    private final int mImageCornerRadius;
    private final Paint mDefaultTextPaint;
    private final Paint mCustomBackgroundTextPaint;

    private final int mDensityDpi;
    private final int mListIconSize;

    /**
     * Creates an instance of a {@link MarkerAppearance} by reading it from the styled attributes in
     * the given context's theme.
     */
    public MarkerAppearance(
        Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
      // Get the marker appearance style resource id from the view's attributes.
      TypedArray viewStyledAttributes =
          context.obtainStyledAttributes(attrs, R.styleable.PlaceMarker, defStyleAttr, defStyleRes);
      int resId = viewStyledAttributes.getResourceId(R.styleable.PlaceMarker_markerAppearance, -1);
      viewStyledAttributes.recycle();

      // No need to pass default values here, the style should contain all these values, which
      // can be ensured by using a default style resource by the caller.
      TypedArray ta = context.obtainStyledAttributes(resId, R.styleable.MarkerAppearance);

      @ColorInt
      int defaultContentColor =
          ta.getColor(R.styleable.MarkerAppearance_markerDefaultContentColor, -1);

      // Set up the paint for the text of the marker's label.
      mDefaultTextPaint =
          new Paint(Paint.LINEAR_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
      mDefaultTextPaint.setTextAlign(Align.CENTER);
      mDefaultTextPaint.setTypeface(
          Typeface.create(
              requireNonNull(ta.getString(R.styleable.MarkerAppearance_android_fontFamily)),
              ta.getInt(R.styleable.MarkerAppearance_android_textStyle, -1)));
      mDefaultTextPaint.setColor(defaultContentColor);
      mDefaultTextPaint.setTextSize(
          ta.getDimensionPixelSize(R.styleable.MarkerAppearance_android_textSize, -1));

      mCustomBackgroundTextPaint = new Paint(mDefaultTextPaint);
      mCustomBackgroundTextPaint.setColor(
          ta.getColor(R.styleable.MarkerAppearance_markerCustomBackgroundContentColor, -1));

      // All other marker/anchor related dimensions and colors
      mMarkerDefaultBackgroundColor =
          ta.getInt(R.styleable.MarkerAppearance_markerDefaultBackgroundColor, -1);
      mMarkerDefaultBorderColor =
          ta.getInt(R.styleable.MarkerAppearance_markerDefaultBorderColor, -1);
      mMarkerCustomBorderColor =
          ta.getInt(R.styleable.MarkerAppearance_markerCustomBorderColor, -1);
      mMarkerPointerWidth =
          ta.getDimensionPixelSize(R.styleable.MarkerAppearance_markerPointerWidth, -1);
      mMarkerPointerHeight =
          ta.getDimensionPixelSize(R.styleable.MarkerAppearance_markerPointerHeight, -1);
      mMarkerStroke = ta.getDimensionPixelSize(R.styleable.MarkerAppearance_markerStroke, -1);
      mMarkerCornerRadius =
          ta.getDimensionPixelSize(R.styleable.MarkerAppearance_markerCornerRadius, -1);
      mAnchorDefaultBackgroundColor =
          ta.getInt(R.styleable.MarkerAppearance_anchorDefaultBackgroundColor, -1);
      mAnchorBorderColor = ta.getInt(R.styleable.MarkerAppearance_anchorBorderColor, -1);
      mAnchorDotColor = ta.getInt(R.styleable.MarkerAppearance_anchorDotColor, -1);
      mTextHorizontalPadding =
          ta.getDimensionPixelSize(R.styleable.MarkerAppearance_markerTextHorizontalPadding, -1);
      mIconSize = ta.getDimensionPixelSize(R.styleable.MarkerAppearance_markerIconSize, -1);
      mImageSize = ta.getDimensionPixelSize(R.styleable.MarkerAppearance_markerImageSize, -1);
      mImageCornerRadius =
          ta.getDimensionPixelSize(R.styleable.MarkerAppearance_markerImageCornerRadius, -1);

      mDefaultIconTint = defaultContentColor;

      int markerPadding = ta.getDimensionPixelSize(R.styleable.MarkerAppearance_markerPadding, -1);
      mMarkerSize = max(mIconSize, mImageSize) + mMarkerStroke + markerPadding;

      mListIconSize = ta.getDimensionPixelSize(R.styleable.MarkerAppearance_markerListIconSize, -1);

      ta.recycle();

      mDensityDpi = context.getResources().getDisplayMetrics().densityDpi;
    }
  }
}
