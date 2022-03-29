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
package com.android.car.libraries.apphost.view.common;

import static androidx.car.app.model.CarIconSpan.ALIGN_BASELINE;
import static androidx.car.app.model.CarIconSpan.ALIGN_BOTTOM;
import static androidx.car.app.model.CarIconSpan.ALIGN_CENTER;
import static com.android.car.libraries.apphost.view.common.ImageUtils.SCALE_CENTER_Y_INSIDE;
import static com.android.car.libraries.apphost.view.common.ImageUtils.SCALE_INSIDE;
import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarIconSpan;
import androidx.car.app.model.CarSpan;
import androidx.car.app.model.CarText;
import androidx.car.app.model.ClickableSpan;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.model.ForegroundCarColorSpan;
import androidx.car.app.model.OnClickDelegate;
import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.apphost.common.CommonUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.common.ImageUtils.ScaleType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Utilities for handling {@link CarText} instances. */
public class CarTextUtils {
  /**
   * An internal flag that indicates that the main text should be converted instead of a variant.
   */
  private static final int USE_MAIN_TEXT = -1;

  /**
   * Returns {@code true} if there is enough color contrast between all {@link
   * ForegroundCarColorSpan}s in the given {@code carText} and the given {@code backgroundColor},
   * otherwise {@code false}.
   */
  public static boolean checkColorContrast(
      TemplateContext templateContext, CarText carText, @ColorInt int backgroundColor) {
    List<CharSequence> texts = new ArrayList<>();
    texts.add(carText.toCharSequence());
    texts.addAll(carText.getVariants());

    for (CharSequence text : texts) {
      if (text instanceof Spanned) {
        Spanned spanned = (Spanned) text;

        for (Object span : spanned.getSpans(0, spanned.length(), Object.class)) {
          if (span instanceof ForegroundCarColorSpan) {
            ForegroundCarColorSpan colorSpan = (ForegroundCarColorSpan) span;
            CarColor foregroundCarColor = colorSpan.getColor();
            if (!CarColorUtils.checkColorContrast(
                templateContext, foregroundCarColor, backgroundColor)) {
              return false;
            }
          }

          if (span instanceof CarIconSpan) {
            CarIconSpan carIconSpan = (CarIconSpan) span;
            CarIcon icon = carIconSpan.getIcon();
            if (icon != null) {
              CarColor tint = icon.getTint();
              if (tint != null
                  && !CarColorUtils.checkColorContrast(templateContext, tint, backgroundColor)) {
                return false;
              }
            }
          }
        }
      }
    }

    return true;
  }

  /**
   * Returns a {@link CharSequence} from a {@link CarText} instance, with default {@link
   * CarTextParams} that disallow images in text spans.
   *
   * @see #toCharSequenceOrEmpty(TemplateContext, CarText, CarTextParams)
   */
  public static CharSequence toCharSequenceOrEmpty(
      TemplateContext templateContext, @Nullable CarText carText) {
    return toCharSequenceOrEmpty(templateContext, carText, CarTextParams.DEFAULT);
  }

  /**
   * Returns a {@link CharSequence} from a {@link CarText} instance, or an empty string if the input
   * {@link CarText} is {@code null}.
   */
  public static CharSequence toCharSequenceOrEmpty(
      TemplateContext templateContext, @Nullable CarText carText, CarTextParams params) {
    return toCharSequenceOrEmpty(templateContext, carText, params, USE_MAIN_TEXT);
  }

  /**
   * Returns a {@link CharSequence} from a {@link CarText} instance's variant at the given index, or
   * an empty string if the input {@link CarText} is {@code null}.
   *
   * <p>if {@code variantIndex} is equal to {@link #USE_MAIN_TEXT}, the main text will be used.
   */
  public static CharSequence toCharSequenceOrEmpty(
      TemplateContext templateContext,
      @Nullable CarText carText,
      CarTextParams params,
      int variantIndex) {
    CharSequence s = toCharSequence(templateContext, carText, params, variantIndex);
    return s == null ? "" : s;
  }

  /**
   * Reconstitutes a {@link CharSequence} from a {@link CarText} instance.
   *
   * <p>The client converts {@link CharSequence}s containing our custom car spans into {@link
   * CarText}s that get marshaled to the host. These spans may contain standard images or icons in
   * them. This method does the inverse conversion to generate char sequences that resolve the
   * actual color resources to use when rendering the text.
   */
  @Nullable
  private static CharSequence toCharSequence(
      TemplateContext templateContext,
      @Nullable CarText carText,
      CarTextParams params,
      int variantIndex) {
    if (carText == null) {
      return null;
    }

    CharSequence charSequence;
    if (variantIndex == USE_MAIN_TEXT) {
      charSequence = carText.toCharSequence();
    } else {
      List<CharSequence> variants = carText.getVariants();
      if (variantIndex >= variants.size()) {
        return null;
      }
      charSequence = variants.get(variantIndex);
    }

    if (!(charSequence instanceof Spanned)) {
      // The API should always return a spanned, but in case it does not, we'll convert the
      // char
      // sequence to string and log a warning, to prevent an invalid cast exception that would
      // crash the host.
      L.w(LogTags.TEMPLATE, "Expecting spanned char sequence, will default to string");
      return charSequence.toString();
    }

    Spanned spanned = (Spanned) charSequence;

    // Separate style and replacement spans.
    List<SpanWrapper> styleSpans = new ArrayList<>();
    List<SpanWrapper> replacementSpans = new ArrayList<>();
    for (Object span : spanned.getSpans(0, spanned.length(), Object.class)) {
      if (span instanceof CarSpan) {
        CarSpan carSpan = (CarSpan) span;
        SpanWrapper wrapper =
            new SpanWrapper(
                carSpan,
                spanned.getSpanStart(span),
                spanned.getSpanEnd(span),
                spanned.getSpanFlags(span));
        if (carSpan instanceof DistanceSpan
            || carSpan instanceof DurationSpan
            || carSpan instanceof CarIconSpan) {
          replacementSpans.add(wrapper);
        } else if (carSpan instanceof ForegroundCarColorSpan || carSpan instanceof ClickableSpan) {
          styleSpans.add(wrapper);
        } else {
          L.e(LogTags.TEMPLATE, "Ignoring non unsupported span type: %s", span);
        }
      } else {
        L.e(LogTags.TEMPLATE, "Ignoring span not of CarSpan type: %s", span);
      }
    }

    // Apply style spans first, and then the replacement spans, in order to apply the correct
    // styling span range to the replacement texts.
    SpannableStringBuilder sb = new SpannableStringBuilder(charSequence.toString());
    setStyleSpans(templateContext, styleSpans, sb, params);
    setReplacementSpans(templateContext, replacementSpans, sb, params);

    return sb;
  }

  /**
   * Sets the spans that change the text style.
   *
   * <p>Supports {@link ForegroundCarColorSpan}. Unsupported spans are ignored.
   */
  private static void setStyleSpans(
      TemplateContext templateContext,
      List<SpanWrapper> styleSpans,
      SpannableStringBuilder sb,
      CarTextParams params) {
    final CarColorConstraints colorSpanConstraints = params.getColorSpanConstraints();
    final boolean allowClickableSpans = params.getAllowClickableSpans();
    for (SpanWrapper wrapper : styleSpans) {
      if (wrapper.mCarSpan instanceof ForegroundCarColorSpan) {
        if (colorSpanConstraints.equals(CarColorConstraints.NO_COLOR)) {
          L.w(LogTags.TEMPLATE, "Color spans not allowed, dropping color: %s", wrapper);
        } else {
          setColorSpan(
              templateContext,
              wrapper,
              sb,
              (ForegroundCarColorSpan) wrapper.mCarSpan,
              colorSpanConstraints,
              params.getBackgroundColor());
        }
      } else if (wrapper.mCarSpan instanceof ClickableSpan) {
        if (!allowClickableSpans) {
          L.w(LogTags.TEMPLATE, "Clickable spans not allowed, dropping click listener");
        } else {
          setClickableSpan(templateContext, wrapper, sb, (ClickableSpan) wrapper.mCarSpan);
        }
      } else {
        L.e(LogTags.TEMPLATE, "Ignoring unsupported span: %s", wrapper);
      }
    }
  }

  /**
   * Sets the spans that replace the text.
   *
   * <p>Supported spans are:
   *
   * <ul>
   *   <li>{@link DistanceSpan}
   *   <li>{@link DurationSpan}
   *   <li>{@link CarIconSpan}
   * </ul>
   *
   * Unsupported spans are ignored.
   *
   * <p>Only spans that do not overlap with any other replacement spans will be applied.
   */
  private static void setReplacementSpans(
      TemplateContext templateContext,
      List<SpanWrapper> replacementSpans,
      SpannableStringBuilder sb,
      CarTextParams params) {
    // Only apply disjoint spans.
    List<SpanWrapper> spans = new ArrayList<>();
    for (SpanWrapper wrapper : replacementSpans) {
      if (isDisjoint(wrapper, replacementSpans)) {
        spans.add(wrapper);
      }
    }

    // Apply replacement spans from right to left.
    Collections.sort(spans, (s1, s2) -> s2.mStart - s1.mStart);
    final int maxImages = params.getMaxImages();
    int imageCount = 0;
    for (SpanWrapper wrapper : spans) {
      CarSpan span = wrapper.mCarSpan;
      if (span instanceof DistanceSpan) {
        Distance distance = ((DistanceSpan) span).getDistance();
        if (distance == null) {
          L.w(LogTags.TEMPLATE, "Distance span is missing its distance: %s", span);
        } else {
          String distanceText =
              DistanceUtils.convertDistanceToDisplayString(templateContext, distance);
          sb.replace(wrapper.mStart, wrapper.mEnd, distanceText);
        }
      } else if (span instanceof DurationSpan) {
        DurationSpan durationSpan = (DurationSpan) span;
        String durationText =
            DateTimeUtils.formatDurationString(
                templateContext, Duration.ofSeconds(durationSpan.getDurationSeconds()));
        sb.replace(wrapper.mStart, wrapper.mEnd, durationText);
      } else if (span instanceof CarIconSpan) {
        if (++imageCount > maxImages) {
          L.w(LogTags.TEMPLATE, "Span over max image count, dropping image: %s", span);
        } else {
          setImageSpan(templateContext, params, wrapper, sb, (CarIconSpan) span);
        }
      } else {
        L.e(
            LogTags.TEMPLATE,
            "Ignoring unsupported span found of type: %s",
            span.getClass().getCanonicalName());
      }
    }
  }

  private static boolean isDisjoint(SpanWrapper wrapper, List<SpanWrapper> spans) {
    for (SpanWrapper otherWrapper : spans) {
      if (wrapper.equals(otherWrapper)) {
        continue;
      }

      if (wrapper.mStart < otherWrapper.mEnd && wrapper.mEnd > otherWrapper.mStart) {
        // The wrapper overlaps with the other wrapper.
        return false;
      }
    }
    return true;
  }

  private static void setImageSpan(
      TemplateContext templateContext,
      CarTextParams params,
      SpanWrapper wrapper,
      SpannableStringBuilder sb,
      CarIconSpan carIconSpan) {
    L.d(LogTags.TEMPLATE, "Converting car image: %s", wrapper);

    Rect boundingBox = requireNonNull(params.getImageBoundingBox());

    // Get the desired alignment for span coming from the app.
    int alignment = carIconSpan.getAlignment();
    if (alignment != ALIGN_BASELINE && alignment != ALIGN_BOTTOM && alignment != ALIGN_CENTER) {
      L.e(LogTags.TEMPLATE, "Invalid alignment value, will default to baseline");
      alignment = ALIGN_BASELINE;
    }

    // Determine how to scale the span image.
    @ScaleType int scaleType;
    int spanAlignment;
    switch (alignment) {
      case ALIGN_BOTTOM:
        spanAlignment = ImageSpan.ALIGN_BOTTOM;
        scaleType = SCALE_INSIDE;
        break;
      case ALIGN_CENTER:
        // API 29 introduces a native ALIGN_BOTTOM ImageSpan option, but in order to supoprt
        // APIs down to our minimum, we implement center alignment by using a
        // center_y_inside
        // scale type. This makes the icon be center aligned with the bounding box on the Y
        // axis. Since our bounding boxes are configured to match the height of a line of
        // text,
        // makes the icon display as center aligned.
        spanAlignment = ImageSpan.ALIGN_BOTTOM;
        scaleType = SCALE_CENTER_Y_INSIDE;
        break;
      case ALIGN_BASELINE: // fall-through
      default:
        spanAlignment = ImageSpan.ALIGN_BASELINE;
        scaleType = SCALE_INSIDE;
        break;
    }

    CarIcon icon = carIconSpan.getIcon();
    if (icon == null) {
      L.e(LogTags.TEMPLATE, "Icon span doesn't contain an icon");
      return;
    }

    ImageViewParams imageParams =
        ImageViewParams.builder()
            .setDefaultTint(params.getDefaultIconTint())
            .setBackgroundColor(params.getBackgroundColor())
            .setIgnoreAppTint(params.ignoreAppIconTint())
            .build();
    Bitmap bitmap =
        ImageUtils.getBitmapFromIcon(
            templateContext,
            icon,
            boundingBox.width(),
            boundingBox.height(),
            imageParams,
            scaleType);
    if (bitmap == null) {
      L.e(LogTags.TEMPLATE, "Failed to get bitmap for icon span");
    } else {
      sb.setSpan(
          new ImageSpan(templateContext, bitmap, spanAlignment),
          wrapper.mStart,
          wrapper.mEnd,
          wrapper.mFlags);
    }
  }

  private static void setColorSpan(
      TemplateContext templateContext,
      SpanWrapper wrapper,
      SpannableStringBuilder sb,
      ForegroundCarColorSpan carColorSpan,
      CarColorConstraints colorSpanConstraints,
      @ColorInt int backgroundColor) {
    L.d(LogTags.TEMPLATE, "Converting foreground color span: %s", wrapper);

    @ColorInt
    int color =
        CarColorUtils.resolveColor(
            templateContext,
            carColorSpan.getColor(),
            /* isDark= */ false,
            /* defaultColor= */ Color.WHITE,
            colorSpanConstraints,
            backgroundColor);
    if (color == Color.WHITE) {
      // If the ForegroundCarColoSpan is of the default color, we do not need to create a span
      // as the view will just use its default color to render.
      return;
    }

    try {
      sb.setSpan(new ForegroundColorSpan(color), wrapper.mStart, wrapper.mEnd, wrapper.mFlags);
    } catch (RuntimeException e) {
      L.e(LogTags.TEMPLATE, e, "Failed to create foreground color span: %s", wrapper);
    }
  }

  private static void setClickableSpan(
      TemplateContext templateContext,
      SpanWrapper wrapper,
      SpannableStringBuilder sb,
      ClickableSpan clickableSpan) {
    L.d(LogTags.TEMPLATE, "Converting clickable span: %s", wrapper);

    OnClickDelegate onClickDelegate = clickableSpan.getOnClickDelegate();
    android.text.style.ClickableSpan span =
        new android.text.style.ClickableSpan() {
          @Override
          public void onClick(@NonNull View widget) {
            CommonUtils.dispatchClick(templateContext, onClickDelegate);
          }
        };

    try {
      sb.setSpan(span, wrapper.mStart, wrapper.mEnd, wrapper.mFlags);
    } catch (RuntimeException e) {
      L.e(LogTags.TEMPLATE, e, "Failed to create clickable span: %s", wrapper);
    }
  }

  /** A simple convenient structure to contain a span with its associated metadata. */
  private static class SpanWrapper {
    CarSpan mCarSpan;
    int mStart;
    int mEnd;
    int mFlags;

    SpanWrapper(CarSpan carSpan, int start, int end, int flags) {
      mCarSpan = carSpan;
      mStart = start;
      mEnd = end;
      mFlags = flags;
    }

    @NonNull
    @Override
    public String toString() {
      return "[" + mCarSpan + ": " + mStart + ", " + mEnd + ", flags: " + mFlags + "]";
    }
  }

  private CarTextUtils() {}
}
