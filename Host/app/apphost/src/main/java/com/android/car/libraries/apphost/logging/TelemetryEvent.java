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
package com.android.car.libraries.apphost.logging;

import android.content.ComponentName;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;

/** Internal representation of a telemetry event. */
@AutoValue
public abstract class TelemetryEvent {

  /** Types of actions to be reported */
  // LINT.IfChange
  public enum UiAction {
    APP_START,
    APP_RUNTIME,

    CAR_APP_API_SUCCESS,
    CAR_APP_API_FAILURE,

    CAR_APPS_AVAILABLE,

    CLIENT_SDK_VERSION,
    HOST_SDK_VERSION,

    TEMPLATE_FLOW_LIMIT_EXCEEDED,
    TEMPLATE_FLOW_INVALID_BACK,

    NAVIGATION_STARTED,
    NAVIGATION_TRIP_UPDATED,
    NAVIGATION_ENDED,

    PAN,
    ROTARY_PAN,
    FLING,
    ZOOM,

    ROW_CLICKED,
    ACTION_STRIP_FAB_CLICKED,
    ACTION_BUTTON_CLICKED,

    LIST_SIZE,
    ACTION_STRIP_SIZE,
    GRID_ITEM_LIST_SIZE,

    ACTION_STRIP_SHOW,
    ACTION_STRIP_HIDE,

    CONTENT_LIMIT_QUERY,

    HOST_FAILURE_CLUSTER_ICON,

    MINIMIZED_STATE,

    SPEEDBUMPED,

    COLOR_CONTRAST_CHECK_PASSED,
    COLOR_CONTRAST_CHECK_FAILED,
  }

  /** Returns the {@link UiAction} that represents the type of action associated with this event. */
  public abstract UiAction getAction();

  /** Returns the version of the app SDK. */
  public abstract Optional<String> getCarAppSdkVersion();

  /** Returns the duration of the event, in milliseconds. */
  public abstract Optional<Long> getDurationMs();

  /** Returns the {@link CarAppApi} associated with the event. */
  public abstract Optional<CarAppApi> getCarAppApi();

  /** Returns the {@link ComponentName} for the app the event is coming from. */
  public abstract Optional<ComponentName> getComponentName();

  /** Returns the {@link CarAppApiErrorType} if the event is an error. */
  public abstract Optional<CarAppApiErrorType> getErrorType();

  /** Returns the position of the event */
  public abstract Optional<Integer> getPosition();

  /** Returns the count of the loaded item */
  public abstract Optional<Integer> getItemsLoadedCount();

  /** Returns a {@link ContentLimitQuery} that is used in the car app. */
  public abstract Optional<ContentLimitQuery> getCarAppContentLimitQuery();

  /** Returns the name of the template used for this event. */
  public abstract Optional<String> getTemplateClassName();

  /**
   * Returns a new builder of {@link TelemetryEvent} set up with the given {@link UiAction}, and the
   * provided {@link ComponentName} set.
   */
  public static Builder newBuilder(UiAction action, ComponentName appName) {
    return newBuilder(action).setComponentName(appName);
  }

  /** Returns a new builder of {@link TelemetryEvent} set up with the given {@link UiAction} */
  public static Builder newBuilder(UiAction action) {
    return new AutoValue_TelemetryEvent.Builder().setAction(action);
  }

  /** UiLogEvent builder. */
  @AutoValue.Builder
  public abstract static class Builder {
    /** Sets the {@link UiAction} that represents the type of action associated with this event. */
    public abstract Builder setAction(UiAction action);

    /** Sets the version of the app SDK. */
    public abstract Builder setCarAppSdkVersion(String carAppSdkVersion);

    /** Sets the duration of the event, in milliseconds. */
    public abstract Builder setDurationMs(long durationMillis);

    /** Sets the {@link CarAppApi} associated with the event. */
    public abstract Builder setCarAppApi(CarAppApi carAppApi);

    /** Sets the {@link ComponentName} for the app the event is coming from. */
    public abstract Builder setComponentName(ComponentName componentName);

    /** Sets the {@link CarAppApiErrorType} if the event is an error. */
    public abstract Builder setErrorType(CarAppApiErrorType errorType);

    /** Sets the position of the event */
    public abstract Builder setPosition(int position);

    /** Sets the count of the loaded item */
    public abstract Builder setItemsLoadedCount(int position);

    /** Sets the {@link ContentLimitQuery} that is used in the car app. */
    public abstract Builder setCarAppContentLimitQuery(ContentLimitQuery constraints);

    /** Sets the class name of the template */
    public abstract Builder setTemplateClassName(String className);

    /** Builds a {@link TelemetryEvent} from this builder. */
    public TelemetryEvent build() {
      return autoBuild();
    }

    /** Non-visible builder method for AutoValue to implement. */
    abstract TelemetryEvent autoBuild();
  }
  // LINT.ThenChange(//depot/google3/java/com/google/android/apps/auto/components/apphost/\
  //      internal/TelemetryHandlerImpl.java,
  //      //depot/google3/java/com/google/android/apps/automotive/templates/host/di/logging/\
  //      ClearcutTelemetryHandler.java,
  //      //depot/google3/logs/proto/wireless/android/automotive/templates/host/\
  //      android_automotive_templates_host_info.proto)
}
