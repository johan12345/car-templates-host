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
package com.android.car.libraries.apphost;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.model.CarLocation;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import java.util.List;

/**
 * A helper class to convert template navigation {@link Intent}s to/from legacy format.
 *
 * <p>Legacy apps are navigation apps that are non template (gmm, waze and kakao).
 *
 * <p>Legacy apps currently support a "https://maps.google.com/maps" uri, which we are not going to
 * force all nav apps to support.
 *
 * <p>There are other navigation uris that some legacy apps support, such as "google.navigation:" or
 * "google.maps:", but not all of them do.
 *
 * <p>The format for the uri for new navigation apps is described at {@link CarContext#startCarApp}.
 */
public final class NavigationIntentConverter {
  public static final String GEO_QUERY_PREFIX = "geo";

  private static final String LEGACY_NAVIGATION_INTENT_DATA_PREFIX =
      "https://maps.google.com/maps?nav=1&q=";

  private static final String NAV_PREFIX = "google.navigation";
  private static final String MAPS_PREFIX = "google.maps";

  private static final String HTTP_MAPS_URL_PREFIX = "http://maps.google.com";
  private static final String HTTPS_MAPS_URL_PREFIX = "https://maps.google.com";
  private static final String HTTPS_ASSISTANT_MAPS_URL_PREFIX = "https://assistant-maps.google.com";

  private static final String TEMPLATE_NAVIGATION_INTENT_DATA_LAT_LNG_PREFIX =
      GEO_QUERY_PREFIX + ":";
  private static final String TEMPLATE_NAVIGATION_INTENT_DATA_PREFIX =
      TEMPLATE_NAVIGATION_INTENT_DATA_LAT_LNG_PREFIX + "0,0?q=";

  private static final String SEARCH_QUERY_PARAMETER = "q";
  private static final String SEARCH_QUERY_PARAMETER_SPLITTER = SEARCH_QUERY_PARAMETER + "=";
  private static final String ADDRESS_QUERY_PARAMETER = "daddr";
  private static final String ADDRESS_QUERY_PARAMETER_SPLITTER = ADDRESS_QUERY_PARAMETER + "=";

  /**
   * Converts the given {@code navIntent} to one that is supported by legacy apps.
   *
   * <p>This method <strong>will update</strong> the {@link Intent} provided.
   *
   * @see CarContext#startCarApp for format documentation
   */
  public static void toLegacyNavIntent(Intent navIntent) {
    L.d(LogTags.APP_HOST, "Converting to legacy nav intent %s", navIntent);

    navIntent.setAction(Intent.ACTION_VIEW);

    Uri navUri = Preconditions.checkNotNull(navIntent.getData());

    // Cleanup by removing spaces.
    CarLocation location = getCarLocation(navUri);

    if (location != null) {
      navIntent.setData(
          Uri.parse(
              LEGACY_NAVIGATION_INTENT_DATA_PREFIX
                  + location.getLatitude()
                  + ","
                  + location.getLongitude()));
    } else {
      String query = getQueryString(navUri);
      if (query == null) {
        throw new IllegalArgumentException("Navigation intent is not properly formed");
      }
      navIntent.setData(
          Uri.parse(LEGACY_NAVIGATION_INTENT_DATA_PREFIX + query.replaceAll("\\s", "+")));
    }
    L.d(LogTags.APP_HOST, "Converted to legacy nav intent %s", navIntent);
  }

  /** Verifies if the given {@link Intent} is for navigation with a legacy navigation app. */
  public static boolean isLegacyNavIntent(Intent intent) {
    Uri uri = intent.getData();

    if (uri == null) {
      return false;
    }

    String scheme = uri.getScheme();
    String dataString = intent.getDataString();
    return GEO_QUERY_PREFIX.equals(scheme)
        || NAV_PREFIX.equals(scheme)
        || MAPS_PREFIX.equals(scheme)
        || Strings.nullToEmpty(dataString).startsWith(HTTP_MAPS_URL_PREFIX) // NOLINT
        || Strings.nullToEmpty(dataString).startsWith(HTTPS_MAPS_URL_PREFIX) // NOLINT
        || Strings.nullToEmpty(dataString).startsWith(HTTPS_ASSISTANT_MAPS_URL_PREFIX); // NOLINT
  }

  /**
   * Converts the given {@code legacyIntent} to one that is supported by template navigation apps.
   *
   * <p>This method <strong>will update</strong> the {@link Intent} provided.
   *
   * @see CarContext#startCarApp for the template navigation {@link Intent} format
   */
  public static void fromLegacyNavIntent(Intent legacyIntent) {
    L.d(LogTags.APP_HOST, "Converting from legacy nav intent %s", legacyIntent);
    Preconditions.checkArgument(isLegacyNavIntent(legacyIntent));

    legacyIntent.setAction(CarContext.ACTION_NAVIGATE);

    Uri uri = Preconditions.checkNotNull(legacyIntent.getData());

    CarLocation location = getCarLocation(uri);

    if (location != null) {
      legacyIntent.setData(
          Uri.parse(
              TEMPLATE_NAVIGATION_INTENT_DATA_LAT_LNG_PREFIX
                  + location.getLatitude()
                  + ","
                  + location.getLongitude()));
    } else {
      String query = getQueryString(uri);
      if (query == null) {
        throw new IllegalArgumentException("Navigation intent is not properly formed");
      }
      legacyIntent.setData(
          Uri.parse(TEMPLATE_NAVIGATION_INTENT_DATA_PREFIX + query.replaceAll("\\s", "+")));
    }
    L.d(LogTags.APP_HOST, "Converted from legacy nav intent %s", legacyIntent);
  }

  /**
   * Returns the latitude, longitude from the {@link Uri}, or {@code null} if none exists.
   *
   * <p>e.g. if Uri string is "geo:123.45,98.09", return value will be a {@link CarLocation} with
   * 123.45 latitude and 98.09 longitude.
   *
   * <p>e.g. if Uri string is "https://maps.google.com/maps?q=123.45,98.09&nav=1", return value will
   * be a {@link CarLocation} with 123.45 latitude and 98.09 longitude.
   */
  @Nullable
  public static CarLocation getCarLocation(Uri uri) {
    String possibleLatLng = getQueryString(uri);
    if (possibleLatLng == null) {
      // If not after a q=, uri is valid as geo:12.34,34.56
      possibleLatLng = uri.getEncodedSchemeSpecificPart();
    }

    List<String> latLngParts = Splitter.on(',').splitToList(possibleLatLng);
    if (latLngParts.size() == 2) {
      try {
        // Ensure both parts are doubles.
        return CarLocation.create(
            Double.parseDouble(latLngParts.get(0)), Double.parseDouble(latLngParts.get(1)));
      } catch (NumberFormatException e) {
        // Values are not Doubles.
      }
    }
    return null;
  }

  /**
   * Returns the actual query from the {@link Uri}, or {@code null} if none exists.
   *
   * <p>The query will be after "q=" or "daddr=".
   *
   * <p>e.g. if Uri string is "geo:0,0?q=124+Foo+St", return value will be "124+Foo+St".
   *
   * <p>e.g. if Uri string is "https://maps.google.com/maps?daddr=123+main+st&nav=1", return value
   * will be "123+main+st".
   */
  @Nullable
  public static String getQueryString(Uri uri) {
    if (uri.isHierarchical()) {
      List<String> query = uri.getQueryParameters(SEARCH_QUERY_PARAMETER);

      if (query.isEmpty()) {
        // No q= parameter, check if there is a daddr= parameter.
        query = uri.getQueryParameters(ADDRESS_QUERY_PARAMETER);
      }
      return Iterables.getFirst(query, null);
    }

    String schemeSpecificPart = uri.getEncodedSchemeSpecificPart();
    List<String> parts =
        Splitter.on(SEARCH_QUERY_PARAMETER_SPLITTER).splitToList(schemeSpecificPart);

    if (parts.size() < 2) {
      // Did not find "q=".
      parts = Splitter.on(ADDRESS_QUERY_PARAMETER_SPLITTER).splitToList(schemeSpecificPart);
    }

    // If we have a valid split on "q=" or "daddr=", split on "&" to only get the one parameter.
    return parts.size() < 2 ? null : Splitter.on("&").splitToList(parts.get(1)).get(0);
  }

  private NavigationIntentConverter() {}
}
