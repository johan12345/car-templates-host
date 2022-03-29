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
package com.android.car.libraries.templates.host.internal;

import android.content.ComponentName;
import android.os.DeadObjectException;
import android.os.RemoteException;
import android.util.Log;
import androidx.car.app.activity.renderer.ICarAppActivity;
import com.android.car.libraries.apphost.logging.LogTags;

/**
 * A dispatcher that can be used to send messages to {@link ICarAppActivity}, handling any remote
 * errors.
 */
public class CarActivityDispatcher {
  private final ComponentName mAppName;
  private final ICarAppActivity mCarAppActivity;
  private final Callback mCallback;
  private boolean mIsConnected;

  public CarActivityDispatcher(
      ComponentName appName, ICarAppActivity carActivity, Callback callback) {
    mAppName = appName;
    mCarAppActivity = carActivity;
    mCallback = callback;
    mIsConnected = true;
  }

  /** {@link CarActivityDispatcher} callbacks */
  public interface Callback {
    /** Notifies that the client associated with this {@link ICarAppActivity} is disconnected */
    void onDisconnect(ComponentName appName);
  }

  /** An IPC call that can be dispatched by this dispatcher */
  public interface IPCCall {
    /** Remote invocation to execute */
    void call(ICarAppActivity carActivity) throws RemoteException;
  }

  /** Returns true if the application is still considered to be connected */
  public boolean isConnected() {
    return mIsConnected;
  }

  /**
   * Dispatches an IPC call to the {@link ICarAppActivity} associated with this dispatcher. If this
   * result in an error, the dispatcher will handle the error and returns false.
   *
   * @return true iif dispatch is successful.
   */
  public boolean dispatchNoFail(IPCCall call) {
    if (!mIsConnected) {
      // Ignoring request as we have already disconnected from the client app
      return false;
    }

    try {
      call.call(mCarAppActivity);
      return true;
    } catch (DeadObjectException e) {
      Log.w(LogTags.APP_HOST, "App " + mAppName + " is dead", e);
      return false;
    } catch (RemoteException e) {
      Log.w(LogTags.APP_HOST, "App " + mAppName + " has caused a remote exception", e);
      return false;
    } catch (Throwable e) {
      Log.w(LogTags.APP_HOST, "App " + mAppName + " caused an unknown error", e);
      return false;
    }
  }

  /**
   * Dispatches an IPC call to the {@link ICarAppActivity} associated with this dispatcher. If this
   * result in an error, the dispatcher will handle the error and then call {@link
   * Callback#onDisconnect(ComponentName)} to notify that this client is not longer valid.
   */
  public void dispatch(IPCCall call) {
    if (!mIsConnected) {
      // Ignoring request as we have already disconnected from the client app
      return;
    }

    try {
      call.call(mCarAppActivity);
    } catch (DeadObjectException e) {
      Log.e(LogTags.APP_HOST, "App " + mAppName + " is dead", e);
      mIsConnected = false;
      mCallback.onDisconnect(mAppName);
    } catch (RemoteException e) {
      Log.e(LogTags.APP_HOST, "App " + mAppName + " has caused a remote exception", e);
      disconnect();
    } catch (Throwable e) {
      Log.e(LogTags.APP_HOST, "App " + mAppName + " caused an unknown error", e);
      disconnect();
    }
  }

  /** Disconnects this dispatcher from its associated {@link ICarAppActivity} */
  public void disconnect() {
    mIsConnected = false;
    try {
      mCarAppActivity.finishCarApp();
    } catch (Throwable e) {
      // Ignoring error as we are already finishing anyways (avoid spamming the logs).
    }
    mCallback.onDisconnect(mAppName);
  }
}
