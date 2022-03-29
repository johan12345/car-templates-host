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
package com.android.car.libraries.templates.host.view.presenters.common;

import android.os.ParcelFileDescriptor;
import androidx.annotation.VisibleForTesting;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import java.io.IOException;
import java.io.InputStream;

/** A class that is used to write bytes to an {@link OutputStream} from an [@link AudioRecord} */
final class AudioRecordThread extends Thread {

  private static final int AUDIO_RECORD_BUFFER_SIZE_BYTES = 512;

  private final ParcelFileDescriptor.AutoCloseOutputStream mOutputStream;
  private final InputStream mInputStream;
  private boolean mIsRecording;
  private final MicrophoneClosedListener mMicrophoneClosedListener;

  AudioRecordThread(
      ParcelFileDescriptor inputDescriptor,
      ParcelFileDescriptor outputDescriptor,
      MicrophoneClosedListener microphoneClosedListener) {
    mOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(outputDescriptor);
    mInputStream = new ParcelFileDescriptor.AutoCloseInputStream(inputDescriptor);
    mMicrophoneClosedListener = microphoneClosedListener;
  }

  @Override
  public void run() {
    mIsRecording = true;
    L.i(LogTags.TEMPLATE, "Recording START");
    while (mIsRecording) {

      // TODO(b/159207187): Consider using read blocking
      byte[] bData = new byte[AUDIO_RECORD_BUFFER_SIZE_BYTES];
      try {
        mInputStream.read(bData);
      } catch (IOException e) {
        L.w(LogTags.TEMPLATE, e, "Recording STOPPED");
        break;
      }
      if (bData == null) {
        L.w(LogTags.TEMPLATE, "Recording STOPPED");
        break;
      }
      // The task may have been cancelled:
      if (isInterrupted()) {
        L.d(LogTags.TEMPLATE, "Recording CANCELLED");
        break;
      }

      if (bData != null) {
        try {
          mOutputStream.write(bData, 0, AUDIO_RECORD_BUFFER_SIZE_BYTES);
        } catch (IOException e) {
          // If we are unable to write bytes to the outputstream
          // we close the outputstream and finish recording
          L.i(LogTags.TEMPLATE, "Recording DONE");
          break;
        }
      }
    }

    L.d(LogTags.TEMPLATE, "Recording CLEANUP");

    // TODO(b/159208600): rewrite AudioRecordThread to use a monitor instead of errors to
    // communicate
    closeRecordingResourcesSafe();
  }

  /** Closes all resources associated with an ongoing recording. */
  public void closeRecordingResourcesSafe() {
    if (!mIsRecording) {
      return;
    }
    try {
      mOutputStream.close();
    } catch (IOException e) {
      L.e(LogTags.TEMPLATE, e, "IOException closing outputstream");
    } finally {
      mIsRecording = false;
      if (mMicrophoneClosedListener != null) {

        mMicrophoneClosedListener.onMicrophoneClosed();
      }
    }
  }

  @VisibleForTesting
  public MicrophoneClosedListener getMicrophoneClosedListener() {
    return mMicrophoneClosedListener;
  }

  @VisibleForTesting
  public void setRecording(boolean isRecording) {
    mIsRecording = isRecording;
  }

  public boolean isRecording() {
    return mIsRecording;
  }
}
