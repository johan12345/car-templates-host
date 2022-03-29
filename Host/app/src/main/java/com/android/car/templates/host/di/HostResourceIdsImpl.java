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
package com.android.car.templates.host.di;

import androidx.annotation.NonNull;
import com.android.car.libraries.apphost.common.ApiIncompatibilityType;
import com.android.car.libraries.apphost.common.HostResourceIds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ServiceComponent;
import com.android.car.templates.host.R;

/** The service level module to provide resources to AOSP Templates Host */
@Module
@InstallIn(ServiceComponent.class)
public class HostResourceIdsImpl implements HostResourceIds {

  @Provides
  static HostResourceIds provideHostResourceIds() {
    return new HostResourceIdsImpl();
  }

  private HostResourceIdsImpl() {}

  @Override
  public int getAlertIconDrawable() {
    return com.android.car.templates.host.R.drawable.default_alert_icon;
  }

  @Override
  public int getErrorIconDrawable() {
    return com.android.car.templates.host.R.drawable.default_error_icon;
  }

  @Override
  public int getBackIconDrawable() {
    return com.android.car.templates.host.R.drawable.default_back_icon;
  }

  @Override
  public int getPanIconDrawable() {
    return com.android.car.templates.host.R.drawable.default_ic_pan_button;
  }

  @Override
  public int getRefreshIconDrawable() {
    return com.android.car.templates.host.R.drawable.default_ic_refresh_button;
  }

  @Override
  public int getRedColor() {
    return R.color.default_standard_red;
  }

  @Override
  public int getRedDarkColor() {
    return R.color.default_standard_red_dark;
  }

  @Override
  public int getGreenColor() {
    return R.color.default_standard_green;
  }

  @Override
  public int getGreenDarkColor() {
    return R.color.default_standard_green_dark;
  }

  @Override
  public int getBlueColor() {
    return R.color.default_standard_blue;
  }

  @Override
  public int getBlueDarkColor() {
    return R.color.default_standard_blue_dark;
  }

  @Override
  public int getYellowColor() {
    return R.color.default_standard_yellow;
  }

  @Override
  public int getYellowDarkColor() {
    return R.color.default_standard_yellow_dark;
  }

  @Override
  public int getDefaultPrimaryColor() {
    return R.color.default_primary_color;
  }

  @Override
  public int getDefaultPrimaryDarkColor() {
    return R.color.default_primary_dark_color;
  }

  @Override
  public int getDefaultSecondaryColor() {
    return R.color.default_secondary_color;
  }

  @Override
  public int getDefaultSecondaryDarkColor() {
    return R.color.default_secondary_dark_color;
  }

  @Override
  public int getDistanceInMetersStringFormat() {
    return R.string.meter_text;
  }

  @Override
  public int getDistanceInKilometersStringFormat() {
    return R.string.kilometer_text;
  }

  @Override
  public int getDistanceInFeetStringFormat() {
    return R.string.feet_text;
  }

  @Override
  public int getDistanceInMilesStringFormat() {
    return R.string.mile_text;
  }

  @Override
  public int getDistanceInYardsStringFormat() {
    return R.string.yard_text;
  }

  @Override
  public int getTimeAtDestinationWithTimeZoneStringFormat() {
    return R.string.time_at_destination_with_time_zone;
  }

  @Override
  public int getDurationInDaysStringFormat() {
    return R.string.duration_in_days;
  }

  @Override
  public int getDurationInDaysAndHoursStringFormat() {
    return R.string.duration_in_days_hours;
  }

  @Override
  public int getDurationInHoursStringFormat() {
    return R.string.duration_in_hours;
  }

  @Override
  public int getDurationInHoursAndMinutesStringFormat() {
    return R.string.duration_in_hours_minutes;
  }

  @Override
  public int getDurationInMinutesStringFormat() {
    return R.string.duration_in_minutes;
  }

  @Override
  public int getAnrMessage() {
    return R.string.anr_message;
  }

  @Override
  public int getAnrWait() {
    return R.string.anr_wait;
  }

  @Override
  public int getAnrWaiting() {
    return R.string.anr_waiting;
  }

  @Override
  public int getAppApiIncompatibleText(@NonNull ApiIncompatibilityType apiIncompatibilityType) {
    return apiIncompatibilityType == ApiIncompatibilityType.APP_TOO_OLD
        ? R.string.app_api_too_old
        : R.string.host_api_too_old;
  }

  @Override
  public int getClientErrorText() {
    return R.string.client_error_text;
  }

  @Override
  public int getMissingPermissionText() {
    return R.string.missing_permission_text;
  }

  @Override
  public int getExitText() {
    return R.string.exit_text;
  }

  @Override
  public int getParkedOnlyActionText() {
    return R.string.parked_only_action;
  }

  @Override
  public int getSearchHintText() {
    return R.string.search_hint;
  }

  @Override
  public int getSearchHintDisabledText() {
    return R.string.search_hint_disabled;
  }

  @Override
  public int getDrivingStateMessageText() {
    return R.string.driving_state_message;
  }

  @Override
  public int getTemplateListNoItemsText() {
    return R.string.template_list_no_items;
  }

  @Override
  public int getLongMessageTemplateDisabledActionText() {
    return R.string.long_message_disabled_action_text;
  }
}
