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
package com.android.car.libraries.apphost.internal;

import android.os.RemoteException;
import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.serialization.BundlerException;
import com.android.car.libraries.apphost.common.ANRHandler;
import com.android.car.libraries.apphost.common.ANRHandler.ANRToken;
import com.android.car.libraries.apphost.common.OneWayIPC;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link OneWayIPC} that will block waiting for a response from the client before returning.
 *
 * <p>Once the client responds set the value received via calling {@link
 * BlockingResponse#setResponse} on the {@link BlockingResponse} that was supplied.
 *
 * <p>When {@link #send} is called, the thread will be blocked up to {@link
 * BlockingResponse#BLOCKING_MAX_MILLIS} milliseconds, or until the client responds, whichever comes
 * first.
 *
 * <p>If the client does not respond until the timeout, the {@link ANRHandler} will display an ANR
 * to the user.
 *
 * @param <T> the type of the response for the IPC
 */
public class BlockingOneWayIPC<T> implements OneWayIPC {
  /**
   * A class to block waiting on a response from the client.
   *
   * @param <T> the type of the response for the IPC
   */
  public static class BlockingResponse<T> {
    // Set to 4 seconds instead of 5 seconds so the system does not ANR.
    private static final long BLOCKING_MAX_MILLIS = 4000;
    private static long sBlockingMaxMillis = BLOCKING_MAX_MILLIS;

    @GuardedBy("this")
    private boolean mComplete;

    @GuardedBy("this")
    @Nullable
    private T mResponse;

    /** Sets the response from the app, releasing any blocking threads. */
    public void setResponse(@Nullable T response) {
      synchronized (this) {
        mResponse = response;
        mComplete = true;
        notifyAll();
      }
    }

    /** Sets the maximum time to block the IPC for before considering it an ANR, in milliseconds. */
    @VisibleForTesting
    public static void setBlockingMaxMillis(long valueForTesting) {
      sBlockingMaxMillis = valueForTesting;
    }

    /**
     * Returns the value provided by calling {@link #setResponse}.
     *
     * <p>This method will block waiting for the client to call back before returning.
     *
     * <p>The max time method will wait is {@link #BLOCKING_MAX_MILLIS}.
     */
    @Nullable
    private T getBlocking() throws TimeoutException, InterruptedException {
      synchronized (this) {
        long startedTimeMillis = System.currentTimeMillis();
        long waitMillis = sBlockingMaxMillis;

        while (!mComplete && waitMillis > 0) {
          wait(waitMillis);

          long elapsedMillis = System.currentTimeMillis() - startedTimeMillis;
          waitMillis = sBlockingMaxMillis - elapsedMillis;
        }
        if (!mComplete) {
          throw new TimeoutException("Response was not set while blocked");
        }

        return mResponse;
      }
    }
  }

  private final OneWayIPC mOneWayIPC;
  private final BlockingResponse<T> mBlockingResponse;
  @Nullable private T mResponse;

  /** Constructs an instance of a {@link BlockingOneWayIPC}. */
  public BlockingOneWayIPC(OneWayIPC oneWayIPC, BlockingResponse<T> blockingResponse) {
    mOneWayIPC = oneWayIPC;
    mBlockingResponse = blockingResponse;
  }

  @Override
  public void send(ANRToken anrToken) throws BundlerException, RemoteException {
    mOneWayIPC.send(anrToken);
    try {
      mResponse = mBlockingResponse.getBlocking();
      anrToken.dismiss();
    } catch (InterruptedException e) {
      anrToken.dismiss();
      throw new IllegalStateException("Exception while waiting for client response.", e);
    } catch (TimeoutException e) {
      L.w(LogTags.APP_HOST, e, "Timeout blocking for a client response");
      // Let the ANR handler handle the ANR by not dismissing the token.
    }
  }

  /**
   * Returns the {@code Response} returned from the {@link Future} provided, or {@code null} if the
   * app did not respond.
   *
   * <p>{@link #send} should be called before calling method, otherwise the result will be {@code
   * null}.
   */
  @Nullable
  public T getResponse() {
    return mResponse;
  }
}
