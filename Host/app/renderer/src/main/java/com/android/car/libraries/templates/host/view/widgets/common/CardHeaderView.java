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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.OnClickDelegate;
import androidx.car.app.model.OnContentRefreshDelegate;
import androidx.core.graphics.drawable.IconCompat;
import com.android.car.libraries.apphost.common.CommonUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.TemplateValidator;
import com.android.car.libraries.apphost.view.common.ImageUtils;
import com.android.car.libraries.apphost.view.common.ImageViewParams;
import com.android.car.libraries.templates.host.R;
import com.android.car.ui.widget.CarUiTextView;

/** A view that displays the header for the templates. */
public class CardHeaderView extends LinearLayout {
  private CarUiTextView mHeaderTitle;
  private ImageView mHeaderButtonIcon;
  private FrameLayout mHeaderButtonContainer;
  private FrameLayout mRefreshButtonContainer;
  private ImageView mRefreshButtonIcon;
  @ColorInt private final int mHeaderIconTint;

  public CardHeaderView(Context context) {
    this(context, null);
  }

  public CardHeaderView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CardHeaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  @SuppressWarnings("nullness:argument") // Fix UnderInitialization warnings
  public CardHeaderView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    LayoutInflater.from(context).inflate(R.layout.header_view, this);

    @StyleableRes final int[] themeAttrs = {R.attr.templateHeaderButtonIconTint};
    TypedArray themeAttrsArray = context.obtainStyledAttributes(themeAttrs);
    mHeaderIconTint = themeAttrsArray.getColor(0, 0);
    themeAttrsArray.recycle();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mHeaderTitle = findViewById(R.id.header_title);
    mHeaderButtonContainer = findViewById(R.id.header_button_container);
    mHeaderButtonIcon = findViewById(R.id.header_icon);
    mRefreshButtonContainer = findViewById(R.id.refresh_button_container);
    mRefreshButtonIcon = findViewById(R.id.refresh_icon);
    ViewUtils.setMinTapTarget(
        this,
        mHeaderButtonContainer,
        getResources().getDimensionPixelSize(R.dimen.template_min_tap_target_size));
  }

  /**
   * Update the {@link HeaderView} to show the given {@code title} and header {@code action}.
   *
   * <p>If the inputs are {@code null} then the view is hidden.
   */
  public void setContent(
      TemplateContext templateContext, @Nullable CarText title, @Nullable Action action) {
    setContent(templateContext, title, action, null);
  }

  /**
   * Update the {@link HeaderView} to show the given {@code title}, header {@code action}, and, if
   * {@code contentRefreshDelegate} is not {@code null}, a refresh button that allow users to
   * interact with to trigger refreshes.
   *
   * <p>If the inputs are {@code null} then the view is hidden.
   */
  public void setContent(
      TemplateContext templateContext,
      @Nullable CarText title,
      @Nullable Action action,
      @Nullable OnContentRefreshDelegate contentRefreshDelegate) {
    boolean isVisible = title != null;
    if (isVisible) {
      mHeaderTitle.setText(
          CarUiTextUtils.fromCarText(templateContext, title, mHeaderTitle.getMaxLines()));

      mHeaderTitle.setVisibility(VISIBLE);
    } else {
      mHeaderTitle.setVisibility(GONE);
    }

    isVisible |= updateHeaderButton(templateContext, action);
    isVisible |= updateRefreshButton(templateContext, contentRefreshDelegate);
    setVisibility(isVisible ? VISIBLE : GONE);
  }

  /**
   * Updates the optional button in the header.
   *
   * @return true if the button ended up visible, false otherwise.
   */
  private boolean updateHeaderButton(TemplateContext templateContext, @Nullable Action action) {
    if (action == null) {
      mHeaderButtonContainer.setVisibility(GONE);
      return false;
    }

    mHeaderButtonContainer.setVisibility(VISIBLE);

    ImageUtils.setImageSrc(
        templateContext,
        ImageUtils.getIconFromAction(action),
        mHeaderButtonIcon,
        ImageViewParams.builder().setDefaultTint(mHeaderIconTint).setForceTinting(true).build());

    if (action.getType() == Action.TYPE_APP_ICON) {
      // Special treatment for app icon as it is un-clickable and un-focusable.
      mHeaderButtonContainer.setFocusable(false);
      mHeaderButtonContainer.setClickable(false);
    } else if (action.getType() == Action.TYPE_BACK) {
      // Special treatment for back as it doesn't have a custom click listener
      mHeaderButtonContainer.setOnClickListener(
          view -> templateContext.getBackPressedHandler().onBackPressed());
      mHeaderButtonContainer.setFocusable(true);
      mHeaderButtonContainer.setClickable(true);
    } else {
      OnClickDelegate onClickDelegate = action.getOnClickDelegate();
      if (onClickDelegate != null) {
        mHeaderButtonContainer.setOnClickListener(
            view -> CommonUtils.dispatchClick(templateContext, onClickDelegate));
        mHeaderButtonContainer.setFocusable(true);
        mHeaderButtonContainer.setClickable(true);
      } else {
        mHeaderButtonContainer.setFocusable(false);
        mHeaderButtonContainer.setClickable(false);
      }
    }

    return true;
  }

  private boolean updateRefreshButton(
      TemplateContext templateContext, @Nullable OnContentRefreshDelegate contentRefreshDelegate) {
    if (!templateContext.getCarHostConfig().isPoiContentRefreshEnabled()
        || contentRefreshDelegate == null) {
      mRefreshButtonContainer.setVisibility(GONE);
      mRefreshButtonContainer.setFocusable(false);
      mRefreshButtonContainer.setClickable(false);
      return false;
    }

    CarIcon icon =
        new CarIcon.Builder(
                IconCompat.createWithResource(
                    getContext(), templateContext.getHostResourceIds().getRefreshIconDrawable()))
            .build();
    ImageUtils.setImageSrc(
        templateContext,
        icon,
        mRefreshButtonIcon,
        ImageViewParams.builder().setDefaultTint(mHeaderIconTint).setForceTinting(true).build());

    mRefreshButtonContainer.setVisibility(VISIBLE);
    mRefreshButtonContainer.setFocusable(true);
    mRefreshButtonContainer.setClickable(true);
    mRefreshButtonContainer.setOnClickListener(
        view -> {
          TemplateValidator templateValidator =
              templateContext.getAppHostService(TemplateValidator.class);
          if (templateValidator != null) {
            templateValidator.setIsNextTemplateContentRefreshIfSameType(true);
          }
          templateContext.getAppDispatcher().dispatchContentRefreshRequest(contentRefreshDelegate);
        });

    return true;
  }
}
