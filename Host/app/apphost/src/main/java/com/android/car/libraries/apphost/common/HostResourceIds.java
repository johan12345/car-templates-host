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

package com.android.car.libraries.apphost.common;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/**
 * Host-dependent resource identifiers.
 *
 * <p>Given that each host will have its own set of resources, this interface abstracts out the
 * exact resource needed in each case.
 */
public interface HostResourceIds {

  /** Returns the resource ID of drawable for the alert icon. */
  @DrawableRes
  int getAlertIconDrawable();

  /** Returns the resource ID of the drawable for the error icon. */
  @DrawableRes
  int getErrorIconDrawable();

  /** Returns the resource ID of the drawable for the error icon. */
  @DrawableRes
  int getBackIconDrawable();

  /** Returns the resource ID of the drawable for the pan icon. */
  @DrawableRes
  int getPanIconDrawable();

  /** Returns the resource ID of drawable for the refresh icon. */
  @DrawableRes
  int getRefreshIconDrawable();

  /** Returns the resource ID of the standard red color. */
  @ColorRes
  int getRedColor();

  /** Returns the resource ID of the standard red color's dark variant. */
  @ColorRes
  int getRedDarkColor();

  /** Returns the resource ID of the standard green color. */
  @ColorRes
  int getGreenColor();

  /** Returns the resource ID of the standard red color's dark variant. */
  @ColorRes
  int getGreenDarkColor();

  /** Returns the resource ID of the standard blue color. */
  @ColorRes
  int getBlueColor();

  /** Returns the resource ID of the standard red color's dark variant. */
  @ColorRes
  int getBlueDarkColor();

  /** Returns the resource ID of the standard yellow color. */
  @ColorRes
  int getYellowColor();

  /** Returns the resource ID of the standard red color's dark variant. */
  @ColorRes
  int getYellowDarkColor();

  /**
   * Returns the resource ID of the default color to use for the standard primary color, unless
   * specified by the app.
   */
  @ColorRes
  int getDefaultPrimaryColor();

  /**
   * Returns the resource ID of the default color to use for the standard primary color, unless
   * specified by the app, in its dark variant.
   */
  @ColorRes
  int getDefaultPrimaryDarkColor();

  /**
   * Returns the resource ID of the default color to use for the standard secondary color, unless
   * specified by the app.
   */
  @ColorRes
  int getDefaultSecondaryColor();

  /**
   * Returns the resource ID of the default color to use for the standard secondary color, unless
   * specified by the app, in its dark variant.
   */
  @ColorRes
  int getDefaultSecondaryDarkColor();

  /** Returns the resource ID of the string used to format a distance in meters. */
  @StringRes
  int getDistanceInMetersStringFormat();

  /** Returns the resource ID of the string used to format a distance in kilometers. */
  @StringRes
  int getDistanceInKilometersStringFormat();

  /** Returns the resource ID of the string used to format a distance in feet. */
  @StringRes
  int getDistanceInFeetStringFormat();

  /** Returns the resource ID of the string used to format a distance in miles. */
  @StringRes
  int getDistanceInMilesStringFormat();

  /** Returns the resource ID of the string used to format a distance in yards. */
  @StringRes
  int getDistanceInYardsStringFormat();

  /** Returns the resource ID of the string used to format a time with a time zone string. */
  @StringRes
  int getTimeAtDestinationWithTimeZoneStringFormat();

  /** Returns the resource ID of the string used to format a duration in days. */
  @StringRes
  int getDurationInDaysStringFormat();

  /** Returns the resource ID of the string used to format a duration in days and hours. */
  @StringRes
  int getDurationInDaysAndHoursStringFormat();

  /** Returns the resource ID of the string used to format a duration in hours. */
  @StringRes
  int getDurationInHoursStringFormat();

  /** Returns the resource ID of the string used to format a duration in hours and minutes. */
  @StringRes
  int getDurationInHoursAndMinutesStringFormat();

  /** Returns the resource ID of the string used to format a duration in minutes. */
  @StringRes
  int getDurationInMinutesStringFormat();

  /** Returns the resource ID of the error message for client app exception */
  @StringRes
  int getAnrMessage();

  /** Returns the resource ID of the button text for waiting for ANR */
  @StringRes
  int getAnrWait();

  /** Returns the resource ID of the error message for waiting for application to respond */
  @StringRes
  int getAnrWaiting();

  /**
   * Returns the resource ID of the error message for client version check failure of the given
   * {@link ApiIncompatibilityType}
   */
  @StringRes
  int getAppApiIncompatibleText(@NonNull ApiIncompatibilityType apiIncompatibilityType);

  /** Returns the resource ID of the error message for client app exception */
  @StringRes
  int getClientErrorText();

  /**
   * Returns the resource ID of the error message for the application not having required permission
   */
  @StringRes
  int getMissingPermissionText();

  /** Returns the resource ID of the error message for client app exception */
  @StringRes
  int getExitText();

  /**
   * Returns the resource ID of the toast message for user selecting action that can only be
   * selected when parked
   */
  @StringRes
  int getParkedOnlyActionText();

  /** Returns the resource ID of the search hint */
  @StringRes
  int getSearchHintText();

  /** Returns the resource ID of the disabled search hint */
  @StringRes
  int getSearchHintDisabledText();

  /** Returns the resource ID of the message for driving state */
  @StringRes
  int getDrivingStateMessageText();

  /** Returns the resource ID of the message for no item for the current list */
  @StringRes
  int getTemplateListNoItemsText();

  /** Returns the resource ID of the message for disabled action in long message template */
  @StringRes
  int getLongMessageTemplateDisabledActionText();
}
