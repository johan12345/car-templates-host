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
package com.android.car.libraries.templates.host.internal

import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.car.app.CarContext
import androidx.car.app.activity.CarAppActivity
import com.android.car.libraries.apphost.NavigationIntentConverter
import com.google.common.base.Objects.equal
import java.lang.UnsupportedOperationException
import java.security.InvalidParameterException

/** An utility class to validate calls to start a car app, and to perform them. */
object StartCarAppUtil {
  private const val PHONE_URI_PREFIX = "tel:"

  /**
   * Metadata tag that points to the component name of the car app service that is linked to the car
   * app service.
   */
  @VisibleForTesting const val ACTIVITY_METADATA_KEY = "androidx.car.app.CAR_APP_ACTIVITY"

  /**
   * Asserts that the `intent` follows the guidelines set in [CarContext.startCarApp] and starts the
   * app.
   *
   * @param packageName the package name of the app that sent the intent
   * @param intent the intent for starting the car app.
   * @param allowedToStartSelf whether the calling app is allowed to start itself. Only nav apps
   * ```
   *                           can call via [CarContext.startCarApp], and all apps can via a
   *                           notification action.
   * @throws SecurityException
   * ```
   * if the app attempts to start a different app explicitly or
   * ```
   *                                   does not have permissions for the requested action.
   * @throws InvalidParameterException
   * ```
   * if the [Intent] does not meet the criteria listed at
   * ```
   *                                   [CarContext.startCarApp].
   * ```
   */
  fun validateStartCarAppIntent(
    context: Context,
    packageName: String,
    intent: Intent,
    allowedToStartSelf: Boolean
  ): Intent {
    val intentComponent = intent.component
    val action = intent.action

    if (intentComponent != null && equal(intentComponent.packageName, packageName)) {
      if (!allowedToStartSelf) {
        throw SecurityException(
          "The app is not a turn by turn navigation app, therefore it cannot start " +
            "itself in the car"
        )
      }
      intent.setClassName(packageName, CarAppActivity::class.qualifiedName!!)
    } else if (equal(action, CarContext.ACTION_NAVIGATE)) {
      assertNavigationIntentIsValid(intent)

      // TODO(b/171308515): Add telemetry support.
    } else if (equal(action, Intent.ACTION_DIAL) || equal(action, Intent.ACTION_CALL)) {
      assertPhoneIntentIsValid(intent)

      // TODO(b/171308515): Add telemetry support.
    } else if (intentComponent == null) {
      throw InvalidParameterException("The intent is not for a supported action")
    } else {
      throw SecurityException("Explicitly starting a separate app is not supported")
    }

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    intent.resolveActivity(context.packageManager)
      ?: throw UnsupportedOperationException(
        "No component found to handle the startCarApp intent: $intent"
      )

    return intent
  }

  /**
   * Checks that the [Intent] is for a phone call by validating it meets the following:
   *
   * * The data is correctly formatted starting with "tel:""
   * * Has no component name set
   */
  private fun assertPhoneIntentIsValid(intent: Intent) {
    if (!intent.dataString.orEmpty().startsWith(PHONE_URI_PREFIX)) {
      throw InvalidParameterException("Phone intent data is not properly formatted")
    }
    if (intent.component != null) {
      throw SecurityException("Phone intent cannot have a component")
    }
  }

  /**
   * Checks that the [Intent] is for navigation by validating it meets the following:
   *
   * * The data is formatted as described in [CarContext.startCarApp]
   * * Has no component name set
   */
  private fun assertNavigationIntentIsValid(intent: Intent) {
    val uri = intent.data
    if (uri == null || !equal(NavigationIntentConverter.GEO_QUERY_PREFIX, uri.scheme)) {
      throw InvalidParameterException("Navigation intent has a malformed uri")
    }

    val queryString = NavigationIntentConverter.getQueryString(uri)
    if (queryString == null) {
      if (NavigationIntentConverter.getCarLocation(uri) == null) {
        throw InvalidParameterException(
          "Navigation intent has neither a location nor a query string"
        )
      }
    } else {
      if (uri.encodedSchemeSpecificPart.contains("daddr=")) {
        // Other intent URIs support daddr, we do not as of right now.
        throw InvalidParameterException(
          "Navigation intent has neither latitude,longitude nor a query string"
        )
      }
    }
    if (intent.component != null) {
      throw SecurityException("Navigation intent cannot have a component")
    }
  }
}
