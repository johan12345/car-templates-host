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
package com.android.car.libraries.apphost.template.view.model;

import androidx.car.app.model.OnSelectedDelegate;

/**
 * Represents a set of rows inside of a list that describe a mutually-exclusive selection group.
 *
 * <p>This can be used to describe multiple radio sub-lists within the same list.
 */
public class SelectionGroup {
  private final int mStartIndex;
  private final int mEndIndex;
  private final OnSelectedDelegate mOnSelectedDelegate;

  /**
   * The currently selected index.
   *
   * <p>The selection index in the group is mutable and allows for automatically updating it the UI
   * at the host without a round-trip to the client to change the selection there.
   */
  private int mSelectedIndex;

  /**
   * Returns an instance of a {@link SelectionGroup}.
   *
   * @param startIndex the index where the selection group starts, inclusive
   * @param endIndex the index where the selection ends, inclusive
   * @param selectedIndex the index of the item in the selection group to select
   * @param onSelectedDelegate a delegate to invoke upon selection change events
   */
  public static SelectionGroup create(
      int startIndex, int endIndex, int selectedIndex, OnSelectedDelegate onSelectedDelegate) {
    return new SelectionGroup(startIndex, endIndex, selectedIndex, onSelectedDelegate);
  }

  /** Returns whether the item at the given index is selected. */
  public boolean isSelected(int index) {
    return index == mSelectedIndex;
  }

  /** Returns the index of the item that's currently selected in the group. */
  public int getSelectedIndex() {
    return mSelectedIndex;
  }

  /** Returns the index relative to the selection group. */
  public int getRelativeIndex(int index) {
    return index - mStartIndex;
  }

  /** Returns the delegate to invoke upon selection change events. */
  public OnSelectedDelegate getOnSelectedDelegate() {
    return mOnSelectedDelegate;
  }

  /** Sets the index of the item to select in the group. */
  public void setSelectedIndex(int selectedIndex) {
    checkSelectedIndexOutOfBounds(mStartIndex, mEndIndex, selectedIndex);
    mSelectedIndex = selectedIndex;
  }

  @Override
  public String toString() {
    return "[start: " + mStartIndex + ", end: " + mEndIndex + ", selected: " + mSelectedIndex + "]";
  }

  private SelectionGroup(
      int startIndex, int endIndex, int selectedIndex, OnSelectedDelegate onSelectedDelegate) {
    checkSelectedIndexOutOfBounds(startIndex, endIndex, selectedIndex);
    mStartIndex = startIndex;
    mEndIndex = endIndex;
    mSelectedIndex = selectedIndex;
    mOnSelectedDelegate = onSelectedDelegate;
  }

  private static void checkSelectedIndexOutOfBounds(
      int startIndex, int endIndex, int selectedIndex) {
    if (selectedIndex < startIndex || selectedIndex > endIndex) {
      throw new IndexOutOfBoundsException(
          "Selected index "
              + selectedIndex
              + " not within bounds of ["
              + startIndex
              + ", "
              + endIndex
              + "]");
    }
  }
}
