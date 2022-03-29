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

import static androidx.annotation.VisibleForTesting.PROTECTED;
import static java.lang.Math.min;

import android.content.ComponentName;
import android.content.Intent;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.AppInfo;
import androidx.car.app.versioning.CarAppApiLevels;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.StatusReporter;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/** Configuration options from the car host. */
public abstract class CarHostConfig implements StatusReporter {

  /** Represent the OEMs' preference on ordering the primary action */
  @IntDef(
      value = {
        PRIMARY_ACTION_HORIZONTAL_ORDER_NOT_SET,
        PRIMARY_ACTION_HORIZONTAL_ORDER_LEFT,
        PRIMARY_ACTION_HORIZONTAL_ORDER_RIGHT,
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface PrimaryActionOrdering {}

  /** Indicates that OEMs choose to not re-ordering the actions */
  public static final int PRIMARY_ACTION_HORIZONTAL_ORDER_NOT_SET = 0;

  /** Indicates that OEMs choose to put the primary action on the left */
  public static final int PRIMARY_ACTION_HORIZONTAL_ORDER_LEFT = 1;

  /** Indicates that OEMs choose to put the primary action on the right */
  public static final int PRIMARY_ACTION_HORIZONTAL_ORDER_RIGHT = 2;

  private final ComponentName mAppName;
  // Default to oldest as the min communication until updated via a call to negotiateApi.
  // The oldest is the default lowest common denominator for communication.
  private int mNegotiatedApi = CarAppApiLevels.getOldest();
  // Last received app info, used for debugging purposes. This is the information the above
  // negotiated API level is based on.
  @Nullable private AppInfo mAppInfo = null;

  public CarHostConfig(ComponentName appName) {
    mAppName = appName;
  }

  /**
   * Returns how many seconds after the user leaves an app, should the system wait before unbinding
   * from it.
   */
  public abstract int getAppUnbindSeconds();

  /** Returns a list of intent extras to be stripped before binding to the client app. */
  public abstract List<String> getHostIntentExtrasToRemove();

  /** Returns whether the provided intent should be treated as a new task flow. */
  public abstract boolean isNewTaskFlowIntent(Intent intent);

  /**
   * Updates the API level for communication between the host and the connecting app.
   *
   * @return the negotiated api
   * @throws IncompatibleApiException if the app's supported API range does not work with the host's
   *     API range
   */
  public int updateNegotiatedApi(AppInfo appInfo) throws IncompatibleApiException {
    mAppInfo = appInfo;
    int appMinApi = mAppInfo.getMinCarAppApiLevel();
    int appMaxApi = mAppInfo.getLatestCarAppApiLevel();
    int hostMinApi = getHostMinApi();
    int hostMaxApi = getHostMaxApi();

    L.i(
        LogTags.APP_HOST,
        "App: [%s] app info: [%s] Host min api: [%d]  Host max api: [%d]",
        mAppName.flattenToShortString(),
        mAppInfo,
        hostMinApi,
        hostMaxApi);

    if (appMinApi > hostMaxApi) {
      throw new IncompatibleApiException(
          ApiIncompatibilityType.HOST_TOO_OLD,
          "App required min API level ["
              + appMinApi
              + "] is higher than the host's max API level ["
              + hostMaxApi
              + "]");
    } else if (hostMinApi > appMaxApi) {
      throw new IncompatibleApiException(
          ApiIncompatibilityType.APP_TOO_OLD,
          "Host required min API level ["
              + hostMinApi
              + "] is higher than the app's max API level ["
              + appMaxApi
              + "]");
    }

    mNegotiatedApi = min(appMaxApi, hostMaxApi);
    L.d(
        LogTags.APP_HOST,
        "App: [%s], Host negotiated api: [%d]",
        mAppName.flattenToShortString(),
        mNegotiatedApi);

    return mNegotiatedApi;
  }

  /** Returns the {@link AppInfo} that was last set, or {@code null} otherwise. */
  @Nullable
  public AppInfo getAppInfo() {
    return mAppInfo;
  }

  /**
   * Returns the API that was negotiated between the host and the connecting app. The host should
   * use this value to determine if a feature for a particular API is supported for the app.
   */
  public int getNegotiatedApi() {
    return mNegotiatedApi;
  }

  @Override
  public void reportStatus(PrintWriter pw, Pii piiHandling) {
    pw.printf(
        "- host min api: %d, host max api: %d, negotiated api: %s\n",
        getHostMinApi(), getHostMaxApi(), mNegotiatedApi);
    pw.printf(
        "- app min api: %s, app target api: %s\n",
        mAppInfo != null ? mAppInfo.getMinCarAppApiLevel() : "-",
        mAppInfo != null ? mAppInfo.getLatestCarAppApiLevel() : "-");
    pw.printf(
        "- sdk version: %s\n", mAppInfo != null ? mAppInfo.getLibraryDisplayVersion() : "n/a");
  }

  /**
   * Returns the host minimum API supported for the app.
   *
   * <p>Depending on the connecting app, the host may be configured to use a higher API level than
   * the lowest level that the host is capable of supporting.
   */
  @VisibleForTesting(otherwise = PROTECTED)
  public abstract int getHostMinApi();

  /**
   * Returns the host maximum API supported for the app.
   *
   * <p>Depending on the connecting app, the host may be configured to use a lower API level than
   * the highest level that the host is capable of supporting.
   */
  @VisibleForTesting(otherwise = PROTECTED)
  public abstract int getHostMaxApi();

  /** Returns whether oem choose to ignore app provided colors on buttons on select templates. */
  public abstract boolean isButtonColorOverriddenByOEM();

  /**
   * Returns the primary action order
   *
   * <p>Depending on the OEMs config, the primary action can be placed on the right or left,
   * regardless of the config from connection app.
   *
   * @see PrimaryActionOrdering
   */
  @PrimaryActionOrdering
  public abstract int getPrimaryActionOrder();

  /** Returns true if the host supports cluster activity */
  public abstract boolean isClusterEnabled();

  /** Returns whether the host supports pan and zoom features in the navigation template */
  public abstract boolean isNavPanZoomEnabled();

  /** Returns whether the host supports pan and zoom features in POI and route preview templates */
  public abstract boolean isPoiRoutePreviewPanZoomEnabled();

  /** Returns whether the host supports pan and zoom features in POI and route preview templates */
  public abstract boolean isPoiContentRefreshEnabled();
}
