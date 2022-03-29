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
package com.android.car.libraries.templates.host.internal;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.NonNull;
import androidx.annotation.StyleableRes;
import androidx.car.app.constraints.ConstraintManager;
import com.android.car.libraries.apphost.common.EventManager;
import com.android.car.libraries.apphost.distraction.constraints.ConstraintsProvider;
import com.android.car.libraries.templates.host.di.UxreConfig;
import com.android.car.libraries.templates.host.R;

/** Provides different limit values for the car app. */
public final class ConstraintsProviderImpl implements ConstraintsProvider {
  private final Context mContext;
  private final EventManager mEventManager;
  private final UxreConfig mUxreConfig;
  private final int mListMaxLength;
  private final int mGridMaxLength;

  private final CarUxRestrictionsUtil.OnUxRestrictionsChangedListener mListener =
      new UxRestrictionChangedListener();

  private CarUxRestrictions mCurrentRestrictions;

  @SuppressWarnings({"ResourceType"})
  public ConstraintsProviderImpl(
      Context context, EventManager eventManager, UxreConfig uxreConfig) {
    mContext = context;
    mEventManager = eventManager;
    mUxreConfig = uxreConfig;

    CarUxRestrictionsUtil carUxRestrictionsUtil = CarUxRestrictionsUtil.getInstance(mContext);
    carUxRestrictionsUtil.register(mListener);
    mCurrentRestrictions = carUxRestrictionsUtil.getCurrentRestrictions();

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateListMaxLength, R.attr.templateGridMaxLength,
    };
    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    mListMaxLength = ta.getInt(0, 6);
    mGridMaxLength = ta.getInt(1, 6);
    ta.recycle();
  }

  @Override
  public int getContentLimit(int contentType) {

    switch (contentType) {
      case ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST:
      case ConstraintManager.CONTENT_LIMIT_TYPE_LIST:
        // TODO(b/186693941): For Android T and above, introduce an accurate UXRE API
        // representing single-page limit so that we don't have to rely on the
        // the getMaxCumulativeContentItems() API.
        return mUxreConfig.getListMaxLength(mListMaxLength);
      case ConstraintManager.CONTENT_LIMIT_TYPE_GRID:
        // TODO(b/186693941): For Android T and above, introduce an accurate UXRE API
        // representing single-page limit so that we don't have to rely on the
        // the getMaxCumulativeContentItems() API.
        return mUxreConfig.getGridMaxLength(mGridMaxLength);
      case ConstraintManager.CONTENT_LIMIT_TYPE_PANE:
        return mUxreConfig.getPaneMaxLength(
            mContext.getResources().getInteger(R.integer.pane_max_length));
      case ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST:
        return mUxreConfig.getRouteListMaxLength(
            mContext.getResources().getInteger(R.integer.route_list_max_length));
      default:
        throw new IllegalArgumentException("Unknown content type: " + contentType);
    }
  }

  @Override
  public int getTemplateStackMaxSize() {
    return mUxreConfig.getTemplateStackMaxSize(
        mContext.getResources().getInteger(R.integer.template_stack_max_size));
  }

  @Override
  public boolean isKeyboardRestricted() {
    return CarUxRestrictionsUtil.isRestricted(
        CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD, mCurrentRestrictions);
  }

  @Override
  public boolean isConfigRestricted() {
    return CarUxRestrictionsUtil.isRestricted(
        CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP, mCurrentRestrictions);
  }

  @Override
  public boolean isFilteringRestricted() {
    return CarUxRestrictionsUtil.isRestricted(
        CarUxRestrictions.UX_RESTRICTIONS_NO_FILTERING, mCurrentRestrictions);
  }

  @Override
  public int getStringCharacterLimit() {
    return mCurrentRestrictions.getMaxRestrictedStringLength();
  }

  void update(CarUxRestrictions restrictions) {
    mCurrentRestrictions = restrictions;
    mEventManager.dispatchEvent(EventManager.EventType.CONSTRAINTS);
  }

  private class UxRestrictionChangedListener
      implements CarUxRestrictionsUtil.OnUxRestrictionsChangedListener {

    @Override
    public void onRestrictionsChanged(@NonNull CarUxRestrictions carUxRestrictions) {
      update(carUxRestrictions);
    }
  }
}
