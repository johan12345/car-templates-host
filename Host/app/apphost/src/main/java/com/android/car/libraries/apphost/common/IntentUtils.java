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

import android.content.Intent;
import androidx.annotation.NonNull;
import java.util.List;

/** Holds static util methods for host Intent manipulations. */
public final class IntentUtils {
  private IntentUtils() {}

  private static final String EXTRA_ORIGINAL_INTENT_KEY =
      "com.android.car.libraries.apphost.common.ORIGINAL_INTENT";

  /** Embeds {@code originalIntent} inside {@code wrappingIntent} for later extraction. */
  public static void embedOriginalIntent(
      @NonNull Intent wrappingIntent, @NonNull Intent originalIntent) {
    wrappingIntent.putExtra(EXTRA_ORIGINAL_INTENT_KEY, originalIntent);
  }

  /**
   * Tries to extract the embedded "original" intent. Gearhead doesn't set this, so it won't always
   * be there.
   */
  @NonNull
  public static Intent extractOriginalIntent(@NonNull Intent binderIntent) {
    Intent originalIntent = binderIntent.getParcelableExtra(EXTRA_ORIGINAL_INTENT_KEY);
    return originalIntent != null ? originalIntent : binderIntent;
  }

  /**
   * Removes any extras that we pass around internally as metadata, preventing them from being
   * exposed to the client apps.
   */
  public static void removeInternalIntentExtras(Intent intent, List<String> extrasToRemove) {
    for (String extra : extrasToRemove) {
      intent.removeExtra(extra);
    }
  }
}
