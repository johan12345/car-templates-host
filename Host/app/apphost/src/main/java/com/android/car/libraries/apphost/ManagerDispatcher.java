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

import android.content.ComponentName;
import android.os.IInterface;
import androidx.annotation.AnyThread;
import com.android.car.libraries.apphost.common.NamedAppServiceCall;
import com.android.car.libraries.apphost.internal.CarAppBinding;

/**
 * A one-way dispatcher of calls to the client app.
 *
 * @param <ServiceT> The type of service to dispatch calls for.
 */
public abstract class ManagerDispatcher<ServiceT extends IInterface> {
  private final String mManagerType;
  private final CarAppBinding mAppBinding;

  public ComponentName getAppName() {
    return mAppBinding.getAppName();
  }

  protected ManagerDispatcher(String managerType, Object appBinding) {
    mManagerType = managerType;
    mAppBinding = (CarAppBinding) appBinding;
  }

  /** Dispatches the {@code call} to the appropriate app service. */
  @AnyThread
  protected void dispatch(NamedAppServiceCall<ServiceT> call) {
    mAppBinding.dispatch(mManagerType, call);
  }
}
