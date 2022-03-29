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
package com.android.car.libraries.apphost.distraction.constraints;

/**
 * Encapsulates the constraints to apply when rendering a {@link androidx.car.app.model.Row} in
 * different contexts.
 */
public class RowConstraints {
  public static final RowConstraints UNCONSTRAINED = RowConstraints.builder().build();

  /** Conservative constraints for a row. */
  public static final RowConstraints ROW_CONSTRAINTS_CONSERVATIVE =
      RowConstraints.builder()
          .setMaxActionsExclusive(0)
          .setImageAllowed(false)
          .setMaxTextLinesPerRow(1)
          .setOnClickListenerAllowed(true)
          .setToggleAllowed(false)
          .build();

  /** The constraints for a full-width row in a pane. */
  public static final RowConstraints ROW_CONSTRAINTS_PANE =
      RowConstraints.builder()
          .setMaxActionsExclusive(2)
          .setImageAllowed(true)
          .setMaxTextLinesPerRow(2)
          .setToggleAllowed(false)
          .setOnClickListenerAllowed(false)
          .build();

  /** The constraints for a simple row (2 rows of text and 1 image */
  public static final RowConstraints ROW_CONSTRAINTS_SIMPLE =
      RowConstraints.builder()
          .setMaxActionsExclusive(0)
          .setImageAllowed(true)
          .setMaxTextLinesPerRow(2)
          .setToggleAllowed(false)
          .setOnClickListenerAllowed(true)
          .build();

  /** The constraints for a full-width row in a list (simple + toggle support). */
  public static final RowConstraints ROW_CONSTRAINTS_FULL_LIST =
      ROW_CONSTRAINTS_SIMPLE.newBuilder().setToggleAllowed(true).build();

  private final int mMaxTextLinesPerRow;
  private final int mMaxActionsExclusive;
  private final boolean mIsImageAllowed;
  private final boolean mIsToggleAllowed;
  private final boolean mIsOnClickListenerAllowed;
  private final CarIconConstraints mCarIconConstraints;

  /** Returns a builder of {@link RowConstraints}. */
  public static Builder builder() {
    return new Builder();
  }

  /** Returns a builder of {@link RowConstraints} set up with the information from this instance. */
  public Builder newBuilder() {
    return new Builder(this);
  }

  /** Returns whether the row can have a click listener associated with it. */
  public boolean isOnClickListenerAllowed() {
    return mIsOnClickListenerAllowed;
  }

  /** Returns the maximum number lines of text, excluding the title, to render in the row. */
  public int getMaxTextLinesPerRow() {
    return mMaxTextLinesPerRow;
  }

  /** Returns the maximum number actions to allowed in a row that consists only of actions. */
  public int getMaxActionsExclusive() {
    return mMaxActionsExclusive;
  }

  /** Returns whether a toggle can be added to the row. */
  public boolean isToggleAllowed() {
    return mIsToggleAllowed;
  }

  /** Returns whether an image can be added to the row. */
  public boolean isImageAllowed() {
    return mIsImageAllowed;
  }

  /** Returns the {@link CarIconConstraints} enforced for the row images. */
  public CarIconConstraints getCarIconConstraints() {
    return mCarIconConstraints;
  }

  private RowConstraints(Builder builder) {
    mIsOnClickListenerAllowed = builder.mIsOnClickListenerAllowed;
    mMaxTextLinesPerRow = builder.mMaxTextLines;
    mMaxActionsExclusive = builder.mMaxActionsExclusive;
    mIsToggleAllowed = builder.mIsToggleAllowed;
    mIsImageAllowed = builder.mIsImageAllowed;
    mCarIconConstraints = builder.mCarIconConstraints;
  }

  /** A builder of {@link RowConstraints}. */
  public static class Builder {
    private boolean mIsOnClickListenerAllowed = true;
    private boolean mIsToggleAllowed = true;
    private int mMaxTextLines = Integer.MAX_VALUE;
    private int mMaxActionsExclusive = Integer.MAX_VALUE;
    private boolean mIsImageAllowed = true;
    private CarIconConstraints mCarIconConstraints = CarIconConstraints.UNCONSTRAINED;

    /** Sets whether a click listener is allowed on the row. */
    public Builder setOnClickListenerAllowed(boolean isOnClickListenerAllowed) {
      mIsOnClickListenerAllowed = isOnClickListenerAllowed;
      return this;
    }

    /** Sets the maximum number of text lines in a row. */
    public Builder setMaxTextLinesPerRow(int maxTextLinesPerRow) {
      mMaxTextLines = maxTextLinesPerRow;
      return this;
    }

    /** Sets the maximum number actions to allowed in a row that consists only of actions. */
    public Builder setMaxActionsExclusive(int maxActionsExclusive) {
      mMaxActionsExclusive = maxActionsExclusive;
      return this;
    }

    /** Sets whether an image can be added to the row. */
    public Builder setImageAllowed(boolean imageAllowed) {
      mIsImageAllowed = imageAllowed;
      return this;
    }

    /** Sets whether a toggle can be added to the row. */
    public Builder setToggleAllowed(boolean toggleAllowed) {
      mIsToggleAllowed = toggleAllowed;
      return this;
    }

    /** Sets the {@link CarIconConstraints} enforced for the row images. */
    public Builder setCarIconConstraints(CarIconConstraints carIconConstraints) {
      mCarIconConstraints = carIconConstraints;
      return this;
    }

    /** Constructs a {@link RowConstraints} object from this builder. */
    public RowConstraints build() {
      return new RowConstraints(this);
    }

    private Builder() {}

    private Builder(RowConstraints constraints) {
      mIsOnClickListenerAllowed = constraints.mIsOnClickListenerAllowed;
      mMaxTextLines = constraints.mMaxTextLinesPerRow;
      mMaxActionsExclusive = constraints.mMaxActionsExclusive;
      mIsToggleAllowed = constraints.mIsToggleAllowed;
      mIsImageAllowed = constraints.mIsImageAllowed;
      mCarIconConstraints = constraints.mCarIconConstraints;
    }
  }
}
