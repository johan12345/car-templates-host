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

import com.android.car.libraries.apphost.internal.CarAppBinding;

/** A factory of {@link Host} instances. */
public interface HostFactory {
  /**
   * Creates a {@link Host} instance.
   *
   * @param appBinding the binding to use to dispatch calls to the client. This is upper bounded to
   *     {@link Object} and down-casted later to avoid making {@link CarAppBinding} public, while
   *     allowing round-tripping it outside of the package
   */
  Host createHost(Object appBinding);
}
