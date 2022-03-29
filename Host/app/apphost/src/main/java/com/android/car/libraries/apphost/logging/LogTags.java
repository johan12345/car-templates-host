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

/**
 * Declares the log tags to use in the app host package.
 *
 * <p>These tags are defined at a higher logical and component level, rather than on a strict
 * per-class basis.
 *
 * <p><strong>IMPORTANT</strong>: do not use per-class tags, since those are often way too granular,
 * hard to manage, and an inferior choice in every way. If you need finer-granularity tags than
 * those here, consider adding a new one.
 */
public abstract class LogTags {
  /** General purpose tag used for most components. */
  public static final String APP_HOST = "CarApp.H";

  /** Tag for code related to constraint host. */
  public static final String CONSTRAINT = APP_HOST + ".Con";

  /** Tag for code related to driver distraction handling. */
  public static final String DISTRACTION = APP_HOST + ".Dis";

  /** Tag for code related to template handling. */
  public static final String TEMPLATE = APP_HOST + ".Tem";

  /** Tag for navigation specific host code. */
  public static final String NAVIGATION = APP_HOST + ".Nav";

  /** Tag for cluster specific host code. */
  public static final String CLUSTER = APP_HOST + ".Clu";

  /** Tag for renderer service (automotive) specific host code. */
  public static final String SERVICE = APP_HOST + ".Ser";

  private LogTags() {}
}
