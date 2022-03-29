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

import static android.graphics.Color.TRANSPARENT;

import static androidx.core.graphics.drawable.IconCompat.TYPE_RESOURCE;
import static androidx.core.graphics.drawable.IconCompat.TYPE_URI;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.apphost.common.HostResourceIds;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints;
import com.android.car.libraries.apphost.distraction.constraints.CarIconConstraints;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.common.ImageViewParams.ImageLoadCallback;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.util.function.Consumer;

/** Assorted image utilities. */
public final class ImageUtils {

    /** Represents different ways of scaling bitmaps. */
    @IntDef(
            value = {
                SCALE_FIT_CENTER,
                SCALE_CENTER_Y_INSIDE,
                SCALE_INSIDE,
                SCALE_CENTER_XY_INSIDE,
            })
    public @interface ScaleType {}

    /**
     * Scales an image so that it fits centered within a bounding box, while maintaining its aspect
     * ratio, and ensuring that at least one of the axis will match exactly the size of the bounding
     * box. This means images may be down-scaled or up-scaled. The smaller dimension of the image
     * will be centered within the bounding box.
     */
    @ScaleType public static final int SCALE_FIT_CENTER = 0;

    /**
     * This scale type is similar to {@link #SCALE_INSIDE} with the difference that the resulting
     * bitmap will always have a height equals to the bounding box's, and the image will be drawn
     * center-aligned vertically if smaller than the bounding box height, with the space at either
     * side padded with transparent pixels.
     */
    @ScaleType public static final int SCALE_CENTER_Y_INSIDE = 1;

    /**
     * Scales an image so that it fits within a bounding box, while maintaining its aspect ratio,
     * but images smaller than the bounding box do not get up-scaled.
     */
    @ScaleType public static final int SCALE_INSIDE = 2;

    /**
     * Similar to {@link #SCALE_FIT_CENTER} but the resulting bitmap never be up-scaled, only
     * down-scaled (if needed).
     */
    @ScaleType public static final int SCALE_CENTER_XY_INSIDE = 3;

    // Suppressing nullness check because AndroidX @Nullable can't be used to annotate generic types
    @SuppressWarnings("nullness:argument")
    private static class ImageTarget extends CustomTarget<Drawable> {
        private final Consumer<Drawable> mImageTarget;

        ImageTarget(int width, int height, Consumer<Drawable> imageTarget) {
            super(width, height);
            this.mImageTarget = imageTarget;
        }

        @Override
        public void onLoadFailed(@Nullable Drawable errorDrawable) {
            mImageTarget.accept(errorDrawable);
        }

        @Override
        public void onResourceReady(
                @NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
            mImageTarget.accept(resource);
        }

        @Override
        public void onLoadCleared(@Nullable Drawable placeholder) {
            mImageTarget.accept(placeholder);
        }
    }

    /** Sets the image source in an {@link ImageView} from a {@link CarIcon}. */
    public static boolean setImageSrc(
            TemplateContext templateContext,
            @Nullable CarIcon carIcon,
            ImageView imageView,
            ImageViewParams viewParams) {
        if (carIcon == null) {
            L.e(LogTags.TEMPLATE, "Failed to load image from a null icon");
            return false;
        }

        try {
            viewParams.getConstraints().validateOrThrow(carIcon);
        } catch (IllegalArgumentException | IllegalStateException e) {
            L.e(LogTags.TEMPLATE, e, "Failed to load image from an invalid icon: %s", carIcon);
            return false;
        }

        int type = carIcon.getType();

        // If the icon is custom, check that it is of a supported type.
        if (type == CarIcon.TYPE_CUSTOM) {
            IconCompat iconCompat = carIcon.getIcon();

            if (iconCompat == null) {
                L.e(LogTags.TEMPLATE, "Failed to get a valid backing icon for: %s", carIcon);
                return setImageDrawable(imageView, null);
            } else if (iconCompat.getType() == TYPE_URI) { // a custom icon of type URI.
                return setImageSrcFromUri(
                        templateContext,
                        iconCompat.getUri(),
                        imageView,
                        carIcon.getTint(),
                        viewParams);
            } else { // a custom icon not of type URI.
                return setImageDrawable(
                        imageView, getIconDrawable(templateContext, carIcon, viewParams));
            }
        }

        // a standard icon
        return setImageDrawable(imageView, getIconDrawable(templateContext, carIcon, viewParams));
    }

    /** Sets the image source in an {@link Consumer<Drawable>} from a {@link CarIcon}. */
    // TODO(b/183990524): See if this method could be unified with setImageSrc()
    // Suppressing nullness check because AndroidX @Nullable can't be used to annotate generic types
    // (see imageTarget parameter)
    @SuppressWarnings("nullness:argument")
    public static boolean setImageTargetSrc(
            TemplateContext templateContext,
            @Nullable CarIcon carIcon,
            Consumer<Drawable> imageTarget,
            ImageViewParams viewParams,
            int width,
            int height) {
        if (carIcon == null) {
            L.e(LogTags.TEMPLATE, "Failed to load image from a null icon");
            return false;
        }

        try {
            viewParams.getConstraints().validateOrThrow(carIcon);
        } catch (IllegalArgumentException | IllegalStateException e) {
            L.e(LogTags.TEMPLATE, e, "Failed to load image from an invalid icon: %s", carIcon);
            return false;
        }

        int type = carIcon.getType();

        // If the icon is custom, check that it is of a supported type.
        if (type == CarIcon.TYPE_CUSTOM) {
            IconCompat iconCompat = carIcon.getIcon();

            if (iconCompat == null) {
                L.e(LogTags.TEMPLATE, "Failed to get a valid backing icon for: %s", carIcon);
                imageTarget.accept(null);
                return false;
            } else if (iconCompat.getType() == TYPE_URI) { // a custom icon of type URI.
                getRequestFromUri(
                                templateContext, iconCompat.getUri(), carIcon.getTint(), viewParams)
                        .into(new ImageTarget(width, height, imageTarget));
                return true;
            } else { // a custom icon not of type URI.
                imageTarget.accept(getIconDrawable(templateContext, carIcon, viewParams));
                return true;
            }
        }

        // a standard icon
        imageTarget.accept(getIconDrawable(templateContext, carIcon, viewParams));
        return true;
    }

    /**
     * Returns a bitmap containing the given {@link IconCompat}.
     *
     * <p>This method cannot be used for icons of type URI which require asynchronous loading.
     */
    @Nullable
    public static Bitmap getBitmapFromIcon(
            TemplateContext templateContext,
            CarIcon icon,
            int targetWidth,
            int targetHeight,
            ImageViewParams viewParams,
            @ScaleType int scaleType) {
        Drawable drawable = getIconDrawable(templateContext, icon, viewParams);
        return drawable == null
                ? null
                : getBitmapFromDrawable(
                        drawable,
                        targetWidth,
                        targetHeight,
                        templateContext.getResources().getDisplayMetrics().densityDpi,
                        scaleType);
    }

    /** Returns a bitmap containing the given label using the given paint. */
    public static Bitmap getBitmapFromString(String label, Paint textPaint) {
        Rect bounds = new Rect();
        textPaint.getTextBounds(label, 0, label.length(), bounds);

        // TODO(b/149182818): robolectric always returns empty bound. Bypass with a 1x1 bitmap.
        // See https://github.com/robolectric/robolectric/issues/4343 for public bug.
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            bounds.set(0, 0, 1, 1);
        }

        Bitmap bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(
                label,
                bounds.width() / 2.f,
                bounds.height() / 2.f - (textPaint.descent() + textPaint.ascent()) / 2.f,
                textPaint);
        return bitmap;
    }

    /**
     * Converts the {@code drawable} to a {@link Bitmap}.
     *
     * <p>The output {@link Bitmap} will be scaled to the input {@code targetWidth} and {@code
     * targetHeight} if the drawable's size does not match up.
     */
    public static Bitmap getBitmapFromDrawable(
            Drawable drawable, int maxWidth, int maxHeight, int density, @ScaleType int scaleType) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

        float widthScale = ((float) maxWidth) / width;
        float heightScale = ((float) maxHeight) / height;

        float scale = Math.min(widthScale, heightScale);

        if (scaleType == SCALE_INSIDE
                || scaleType == SCALE_CENTER_Y_INSIDE
                || scaleType == SCALE_CENTER_XY_INSIDE) {
            // Scale down if necessary. Do not scale up.
            scale = Math.min(1.f, scale);
        }

        int scaledWidth = (int) (width * scale);
        int scaledHeight = (int) (height * scale);

        int bitmapWidth = scaledWidth;
        int bitmapHeight = scaledHeight;
        switch (scaleType) {
            case SCALE_FIT_CENTER:
            case SCALE_CENTER_XY_INSIDE:
                bitmapWidth = maxWidth;
                bitmapHeight = maxHeight;
                break;
            case SCALE_CENTER_Y_INSIDE:
                bitmapHeight = maxHeight;
                break;
            case SCALE_INSIDE:
            default:
                break;
        }

        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Config.ARGB_8888);
        bitmap.setDensity(density);
        Canvas canvas = new Canvas(bitmap);

        float dx = 0;
        float dy = 0;
        // Center-align the image horizontally/vertically if we have to.
        switch (scaleType) {
            case SCALE_FIT_CENTER:
            case SCALE_CENTER_XY_INSIDE:
                dx = Math.max(0.f, (maxWidth - scaledWidth) / 2.f);
                dy = Math.max(0.f, (maxHeight - scaledHeight) / 2.f);
                break;
            case SCALE_CENTER_Y_INSIDE:
                dy = Math.max(0.f, (maxHeight - scaledHeight) / 2.f);
                break;
            case SCALE_INSIDE:
            default:
                break;
        }
        canvas.translate(dx, dy);
        canvas.scale(scale, scale);
        drawable.setFilterBitmap(true);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    @DrawableRes
    @VisibleForTesting
    static int drawableIdFromCarIconType(int type, HostResourceIds hostResourceIds) {
        switch (type) {
            case CarIcon.TYPE_ALERT:
                return hostResourceIds.getAlertIconDrawable();
            case CarIcon.TYPE_ERROR:
                return hostResourceIds.getErrorIconDrawable();
            case CarIcon.TYPE_BACK:
                return hostResourceIds.getBackIconDrawable();
            case CarIcon.TYPE_PAN:
                return hostResourceIds.getPanIconDrawable();
            case CarIcon.TYPE_APP_ICON:
            case CarIcon.TYPE_CUSTOM:
            default:
                L.w(LogTags.TEMPLATE, "Can't find drawable for icon type: %d", type);
                return 0;
        }
    }

    /** Returns the {@link CarIcon} that should be used for an {@link Action}. */
    @Nullable
    public static CarIcon getIconFromAction(Action action) {
        CarIcon icon = action.getIcon();
        if (icon == null && action.isStandard()) {
            int type = action.getType();
            icon = ImageUtils.getIconForStandardAction(type);
            if (icon == null) {
                L.e(LogTags.TEMPLATE, "Failed to get icon for standard action: %s", action);
            }
        }

        return icon;
    }

    /** Returns the {@link CarIcon} corresponding to an action type. */
    @Nullable
    private static CarIcon getIconForStandardAction(int type) {
        switch (type) {
            case Action.TYPE_APP_ICON:
                return CarIcon.APP_ICON;
            case Action.TYPE_BACK:
                return CarIcon.BACK;
            case Action.TYPE_PAN:
                return CarIcon.PAN;
            case Action.TYPE_CUSTOM:
            default:
                L.e(LogTags.TEMPLATE, "Not a standard action: %s", type);
                return null;
        }
    }

    /**
     * Sets the drawable to the image view.
     *
     * <p>Returns {@code true} if the view sets an image, and {@code false} if it clears the image
     * (by setting a {@code null} drawable).
     */
    private static boolean setImageDrawable(ImageView imageView, @Nullable Drawable drawable) {
        imageView.setImageDrawable(drawable);
        return drawable != null;
    }

    private static boolean setImageSrcFromUri(
            TemplateContext templateContext,
            Uri uri,
            ImageView imageView,
            @Nullable CarColor tint,
            ImageViewParams viewParams) {
        getRequestFromUri(templateContext, uri, tint, viewParams).into(imageView);
        return true;
    }

    private static RequestBuilder<Drawable> getRequestFromUri(
            TemplateContext templateContext,
            Uri uri,
            @Nullable CarColor tint,
            ImageViewParams viewParams) {
        return Glide.with(templateContext)
                .load(uri)
                .placeholder(viewParams.getPlaceholderDrawable())
                .listener(
                        new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(
                                    @Nullable GlideException e,
                                    Object model,
                                    Target<Drawable> target,
                                    boolean isFirstResource) {
                                ImageLoadCallback callback = viewParams.getImageLoadCallback();
                                if (callback != null) {
                                    callback.onLoadFailed(e);
                                } else {
                                    L.e(
                                            LogTags.TEMPLATE,
                                            e,
                                            "Failed to load the image for URI: %s",
                                            uri);
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(
                                    Drawable resource,
                                    Object model,
                                    Target<Drawable> target,
                                    DataSource dataSource,
                                    boolean isFirstResource) {
                                // If tint is specified in the icon, overwrite the backing icon's
                                // tint.
                                @ColorInt
                                int tintInt = getTintForIcon(templateContext, tint, viewParams);
                                if (tintInt != TRANSPARENT) {
                                    resource.mutate();
                                    resource.setTint(tintInt);
                                    resource.setTintMode(Mode.SRC_IN);
                                }

                                ImageLoadCallback callback = viewParams.getImageLoadCallback();
                                if (callback != null) {
                                    // TODO(b/156279162): Consider transition from placeholder image
                                    target.onResourceReady(resource, /* transition= */ null);
                                    callback.onImageReady();
                                    return true;
                                }
                                return false;
                            }
                        });
    }

    /**
     * Returns the tint to use for a given {@link CarColor} tint, or {@link Color#TRANSPARENT} if
     * not tint should be applied.
     */
    @ColorInt
    private static int getTintForIcon(
            TemplateContext templateContext, @Nullable CarColor tint, ImageViewParams params) {
        @ColorInt int defaultTint = params.getDefaultTint();
        boolean forceTinting = params.getForceTinting();
        boolean isDark = params.getIsDark();

        if (tint != null && params.ignoreAppTint()) {
            tint = CarColor.DEFAULT;
        }

        if (tint != null || forceTinting) {
            return CarColorUtils.resolveColor(
                    templateContext,
                    tint,
                    isDark,
                    defaultTint,
                    CarColorConstraints.UNCONSTRAINED,
                    params.getBackgroundColor());
        }
        return TRANSPARENT;
    }

    /**
     * Returns a drawable for a {@link CarIcon}.
     *
     * <p>This method should not be used for icons of type URI.
     *
     * @return {@code null} if it failed to get the icon, or if the icon type is a URI.
     */
    @Nullable
    public static Drawable getIconDrawable(
            TemplateContext templateContext, CarIcon carIcon, ImageViewParams viewParams) {
        int type = carIcon.getType();
        if (type == CarIcon.TYPE_APP_ICON) {
            return templateContext.getCarAppPackageInfo().getRoundAppIcon();
        }

        CarIconConstraints constraints = viewParams.getConstraints();
        try {
            constraints.validateOrThrow(carIcon);
        } catch (IllegalArgumentException | IllegalStateException e) {
            L.e(LogTags.TEMPLATE, e, "Failed to load drawable from an invalid icon: %s", carIcon);
            return null;
        }

        // Either a custom icon, or a standard icon other than the app icon: get its backing icon.
        IconCompat iconCompat = getBackingIconCompat(templateContext, carIcon);
        if (iconCompat == null) {
            return null;
        }

        // If tint is specified in the icon, overwrite the backing icon's tint.
        @ColorInt int tintInt = getTintForIcon(templateContext, carIcon.getTint(), viewParams);

        // Load the resource drawables from the app using the configuration context so that we get
        // them
        // with the right target DPI and theme attributes are resolved correctly.
        if (iconCompat.getType() == TYPE_RESOURCE) {
            String iconPackageName = iconCompat.getResPackage();
            if (iconPackageName == null) {
                // If an app sends an IconCompat created with an androidx.core version before 1.4,
                // the
                // package name will be null.
                L.w(
                        LogTags.TEMPLATE,
                        "Failed to load drawable from an icon with an unknown package name: %s",
                        carIcon);
                return null;
            }

            String packageName =
                    templateContext.getCarAppPackageInfo().getComponentName().getPackageName();

            // Remote resource from the app?
            if (iconPackageName.equals(packageName)) {
                return loadAppResourceDrawable(templateContext, iconCompat, tintInt);
            }
        }

        if (tintInt != TRANSPARENT) {
            iconCompat.setTint(tintInt);
            iconCompat.setTintMode(Mode.SRC_IN);
        }

        return iconCompat.loadDrawable(templateContext);
    }

    @Nullable
    private static Drawable loadAppResourceDrawable(
            TemplateContext templateContext, IconCompat iconCompat, @ColorInt int tintInt) {
        String packageName =
                templateContext.getCarAppPackageInfo().getComponentName().getPackageName();

        int density = templateContext.getResources().getDisplayMetrics().densityDpi;
        @SuppressLint("ResourceType")
        @DrawableRes
        int resId = iconCompat.getResId();

        Context configurationContext = templateContext.getAppConfigurationContext();
        if (configurationContext == null) {
            L.e(
                    LogTags.TEMPLATE,
                    "Failed to load drawable for %d, configuration unavailable",
                    resId);
            return null;
        }

        L.d(
                LogTags.TEMPLATE,
                "Loading resource drawable with id %d for density %d from package %s",
                resId,
                density,
                packageName);

        // Load the drawable passing the density explicitly.
        // The IconCompat#loadDrawable path /should/ be able to do this, but it does not.
        // See b/159103561 for details. A side effect of us branching off this code path is that
        // the tint set in the IconCompat instance is not honored.
        Drawable drawable =
                configurationContext
                        .getResources()
                        .getDrawableForDensity(resId, density, configurationContext.getTheme());
        if (drawable == null) {
            L.e(LogTags.TEMPLATE, "Failed to load drawable for %d", resId);
            return null;
        }

        if (tintInt != TRANSPARENT) {
            drawable.mutate();
            DrawableCompat.setTintList(drawable, ColorStateList.valueOf(tintInt));
            DrawableCompat.setTintMode(drawable, Mode.SRC_IN);
        }

        return drawable;
    }

    @Nullable
    private static IconCompat getBackingIconCompat(
            TemplateContext templateContext, CarIcon carIcon) {
        IconCompat iconCompat;
        int type = carIcon.getType();
        if (type == CarIcon.TYPE_CUSTOM) {
            iconCompat = carIcon.getIcon();
            if (iconCompat == null) {
                L.e(LogTags.TEMPLATE, "Custom icon without backing icon: %s", carIcon);
                return null;
            }
        } else { // a standard icon
            @DrawableRes
            int resId = drawableIdFromCarIconType(type, templateContext.getHostResourceIds());
            if (resId == 0) {
                L.e(LogTags.TEMPLATE, "Failed to find resource id for standard icon: %s", carIcon);
                return null;
            }

            iconCompat = IconCompat.createWithResource(templateContext, resId);
            if (iconCompat == null) {
                L.e(LogTags.TEMPLATE, "Failed to load standard icon: %s", carIcon);
                return null;
            }
        }

        return iconCompat;
    }

    private ImageUtils() {}
}
