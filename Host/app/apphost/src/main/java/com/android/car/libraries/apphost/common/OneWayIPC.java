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

import android.os.RemoteException;
import androidx.car.app.serialization.BundlerException;
import com.android.car.libraries.apphost.common.ANRHandler.ANRToken;

/**
 * A request to send over the wire to the app.
 *
 * <p>The method interface of the client should be marked {@code oneway}.
 *
 * <p>You should not call {@link #send} yourself, but rather use the {@link AppDispatcher} to send
 * this request. This allows for a single location to handle exceptions and performing IPC.
 */
public interface OneWayIPC {
  /** Sends an IPC to the app, using the given {@link ANRToken}. */
  void send(ANRToken anrToken) throws BundlerException, RemoteException;
}
