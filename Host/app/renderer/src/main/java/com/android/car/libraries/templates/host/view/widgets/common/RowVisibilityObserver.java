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
package com.android.car.libraries.templates.host.view.widgets.common;

import static java.util.Objects.requireNonNull;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.recyclerview.CarUiRecyclerView.OnScrollListener;

/**
 * Observes the visibility change of the given {@link RecyclerView}.
 *
 * <p>Since we do not own the layout manager of {@link RecyclerView}, this class can be used to
 * listen for the visibility changes of the recycler view due to scrolling.
 */
public class RowVisibilityObserver {

  /** Listener for the item visibility changes. */
  interface OnItemVisibilityChangedListener {

    /** Callback when item visibility changes. */
    void sendItemVisibilityChanged(int startIndexInclusive, int endIndexExclusive);
  }

  private static final int MSG_HANDLE_VISIBLE_ROWS_CHANGE = 1;
  private static final int HANDLE_ROW_CHANGE_DELAY_MILLIS = 150;
  private static final int INVALID_ROW_INDEX = Integer.MIN_VALUE;

  @NonNull private final CarUiRecyclerView mRecyclerView;
  private final Handler mHandler = new Handler(Looper.getMainLooper(), new HandlerCallback());
  private final OnScrollListener mOnScrollListener =
      new CarUiRecyclerView.OnScrollListener() {
        // Suppressing error for referencing handleVisibleRowsChange() before
        // initialization of RowVisibilityObserver.
        @SuppressWarnings("nullness:method.invocation")
        @Override
        public void onScrollStateChanged(CarUiRecyclerView recyclerView, int newState) {
          if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            handleVisibleRowsChange();
          }
        }

        @Override
        public void onScrolled(CarUiRecyclerView recyclerView, int dx, int dy) {}
      };

  private final OnLayoutChangeListener mOnLayoutChangeListener =
      new OnLayoutChangeListener() {
        // Suppressing error for referencing handleVisibleRowsChange() before
        // initialization of RowVisibilityObserver and mReceyclerView being null.
        @SuppressWarnings("nullness")
        @Override
        public void onLayoutChange(
            View v,
            int left,
            int top,
            int right,
            int bottom,
            int oldLeft,
            int oldTop,
            int oldRight,
            int oldBottom) {
          if (mRecyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
            handleVisibleRowsChange();
          }
        }
      };

  @Nullable private OnItemVisibilityChangedListener mListener;
  private int mFirstVisibleRowIndex;
  private int mLastVisibleRowIndexExclusive;

  /** Returns an instance of {@link RowVisibilityObserver}. */
  public static RowVisibilityObserver create(@NonNull CarUiRecyclerView recyclerView) {
    return new RowVisibilityObserver(requireNonNull(recyclerView));
  }

  /** Sets an {@link OnItemVisibilityChangedListener}. */
  public void setOnItemVisibilityChangedListener(
      @NonNull OnItemVisibilityChangedListener listener) {
    requireNonNull(listener);

    // Remove any existing listener.
    removeOnItemVisibilityChangedListener();

    mListener = listener;

    // Reset the cached start/end indices, so that the newly-set listener will be invoked even
    // if the visible rows might not have changed.
    mFirstVisibleRowIndex = INVALID_ROW_INDEX;
    mLastVisibleRowIndexExclusive = INVALID_ROW_INDEX;

    mRecyclerView.addOnScrollListener(mOnScrollListener);
    mRecyclerView.addOnLayoutChangeListener(mOnLayoutChangeListener);
  }

  /** Removes any existing {@link OnItemVisibilityChangedListener}. */
  public void removeOnItemVisibilityChangedListener() {
    if (mListener == null) {
      return;
    }

    mRecyclerView.removeOnScrollListener(mOnScrollListener);
    mRecyclerView.removeOnLayoutChangeListener(mOnLayoutChangeListener);
    mListener = null;
  }

  /** Creates a {@link RowVisibilityObserver} for given {@link RecyclerView}. */
  private RowVisibilityObserver(@NonNull CarUiRecyclerView recyclerView) {
    mRecyclerView = requireNonNull(recyclerView);

    // Start with an invalid index, so that the newly-set listener will be invoked.
    mFirstVisibleRowIndex = INVALID_ROW_INDEX;
    mLastVisibleRowIndexExclusive = INVALID_ROW_INDEX;
  }


  /** Sends a message to the handler to publish item visibility change event. */
  private void handleVisibleRowsChange() {
    mHandler.removeMessages(MSG_HANDLE_VISIBLE_ROWS_CHANGE);
    Message message = mHandler.obtainMessage(MSG_HANDLE_VISIBLE_ROWS_CHANGE);
    if (mRecyclerView.getRecyclerViewChildCount() == 0) {
      // When a full data refresh happens in the adapter that backs the recycler view, the
      // view reports no visible items first for a few milliseconds, and then reports the new
      // updated items.
      // This ephemeral state of emptiness can cause flickering for the views that listen to
      // the published events (e.g. the map view which clears and renders pins in the map).
      // This is a work around by adding a short delay before sending the item visibility
      // change event.
      // TODO(b/183989613): Possibly remove once list diffing is implemented.
      mHandler.sendMessageDelayed(message, HANDLE_ROW_CHANGE_DELAY_MILLIS);
    } else {
      mHandler.sendMessage(message);
    }
  }

  /** A {@link Handler.Callback} used to process the message queue for the visibility events. */
  private class HandlerCallback implements Handler.Callback {

    /** Publishes the item visibility changed event to the listener. */
    @Override
    public boolean handleMessage(Message msg) {
      if (msg.what == MSG_HANDLE_VISIBLE_ROWS_CHANGE) {
        int firstVisibleRowIndex = mRecyclerView.findFirstCompletelyVisibleItemPosition();
        int lastVisibleRowIndex = mRecyclerView.findLastCompletelyVisibleItemPosition();
        int lastVisibleRowIndexExclusive = lastVisibleRowIndex + 1;

        L.d(
            LogTags.TEMPLATE,
            "Handling visible rows in range (%d, %d)",
            firstVisibleRowIndex,
            lastVisibleRowIndexExclusive);

        if (firstVisibleRowIndex == mFirstVisibleRowIndex
            && lastVisibleRowIndexExclusive == mLastVisibleRowIndexExclusive) {
          return true;
        }

        if (mListener != null) {
          mListener.sendItemVisibilityChanged(firstVisibleRowIndex, lastVisibleRowIndexExclusive);
        }

        mFirstVisibleRowIndex = firstVisibleRowIndex;
        mLastVisibleRowIndexExclusive = lastVisibleRowIndexExclusive;

        return true;
      } else {
        L.w(LogTags.TEMPLATE, "Unknown message: %s", msg);
      }
      return false;
    }
  }
}
