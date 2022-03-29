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

import static androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_LIST;
import static androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_PANE;
import static androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST;
import static com.android.car.libraries.apphost.distraction.constraints.RowConstraints.ROW_CONSTRAINTS_CONSERVATIVE;
import static com.android.car.libraries.apphost.distraction.constraints.RowConstraints.ROW_CONSTRAINTS_FULL_LIST;
import static com.android.car.libraries.apphost.distraction.constraints.RowConstraints.ROW_CONSTRAINTS_PANE;
import static com.android.car.libraries.apphost.distraction.constraints.RowConstraints.ROW_CONSTRAINTS_SIMPLE;

/** Encapsulates the constraints to apply when rendering a row list under different contexts. */
public class RowListConstraints {
  /** Conservative constraints for all types lists. */
  public static final RowListConstraints ROW_LIST_CONSTRAINTS_CONSERVATIVE =
      RowListConstraints.builder()
          .setListContentType(CONTENT_LIMIT_TYPE_LIST)
          .setMaxActions(0)
          .setRowConstraints(ROW_CONSTRAINTS_CONSERVATIVE)
          .setAllowSelectableLists(false)
          .build();

  /** Default constraints for heterogeneous pane of items, full width. */
  public static final RowListConstraints ROW_LIST_CONSTRAINTS_PANE =
      ROW_LIST_CONSTRAINTS_CONSERVATIVE
          .newBuilder()
          .setMaxActions(2)
          .setListContentType(CONTENT_LIMIT_TYPE_PANE)
          .setRowConstraints(ROW_CONSTRAINTS_PANE)
          .setAllowSelectableLists(false)
          .build();

  /** Default constraints for uniform lists of items, no toggles. */
  public static final RowListConstraints ROW_LIST_CONSTRAINTS_SIMPLE =
      ROW_LIST_CONSTRAINTS_CONSERVATIVE
          .newBuilder()
          .setRowConstraints(ROW_CONSTRAINTS_SIMPLE)
          .build();

  /** Default constraints for the route preview card. */
  public static final RowListConstraints ROW_LIST_CONSTRAINTS_ROUTE_PREVIEW =
      ROW_LIST_CONSTRAINTS_CONSERVATIVE
          .newBuilder()
          .setListContentType(CONTENT_LIMIT_TYPE_ROUTE_LIST)
          .setRowConstraints(ROW_CONSTRAINTS_SIMPLE)
          .setAllowSelectableLists(true)
          .build();

  /** Default constraints for uniform lists of items, full width (simple + toggle support). */
  public static final RowListConstraints ROW_LIST_CONSTRAINTS_FULL_LIST =
      ROW_LIST_CONSTRAINTS_CONSERVATIVE
          .newBuilder()
          .setRowConstraints(ROW_CONSTRAINTS_FULL_LIST)
          .setAllowSelectableLists(true)
          .build();

  private final int mListContentType;
  private final int mMaxActions;
  private final RowConstraints mRowConstraints;
  private final boolean mAllowSelectableLists;

  /** A builder of {@link RowListConstraints}. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns a builder of {@link RowListConstraints} set up with the information from this instance.
   */
  public Builder newBuilder() {
    return new Builder(this);
  }

  /**
   * Returns the list content type for this constraint.
   *
   * <p>This should be one of the content types as defined in {@link
   * androidx.car.app.constraints.ConstraintManager}.
   */
  public int getListContentType() {
    return mListContentType;
  }

  /** Returns the maximum number of actions allowed to be added alongside the list. */
  public int getMaxActions() {
    return mMaxActions;
  }

  /** Returns the constraints to apply on individual rows. */
  public RowConstraints getRowConstraints() {
    return mRowConstraints;
  }

  /** Returns whether radio lists are allowed. */
  public boolean getAllowSelectableLists() {
    return mAllowSelectableLists;
  }

  private RowListConstraints(Builder builder) {
    mMaxActions = builder.mMaxActions;
    mRowConstraints = builder.mRowConstraints;
    mAllowSelectableLists = builder.mAllowSelectableLists;
    mListContentType = builder.mListContentType;
  }

  /** A builder of {@link RowListConstraints}. */
  public static class Builder {
    private int mListContentType;
    private int mMaxActions;
    private RowConstraints mRowConstraints = RowConstraints.UNCONSTRAINED;
    private boolean mAllowSelectableLists;

    /**
     * Sets the content type for this constraint.
     *
     * <p>This should be one of the content types as defined in {@link
     * androidx.car.app.constraints.ConstraintManager}.
     */
    public Builder setListContentType(int contentType) {
      mListContentType = contentType;
      return this;
    }

    /** Sets the maximum number of actions allowed to be added alongside the list. */
    public Builder setMaxActions(int maxActions) {
      mMaxActions = maxActions;
      return this;
    }

    /** Sets the constraints to apply on individual rows. */
    public Builder setRowConstraints(RowConstraints rowConstraints) {
      mRowConstraints = rowConstraints;
      return this;
    }

    /** Sets whether radio lists are allowed. */
    public Builder setAllowSelectableLists(boolean allowSelectableLists) {
      mAllowSelectableLists = allowSelectableLists;
      return this;
    }

    /** Constructs a {@link RowListConstraints} from this builder. */
    public RowListConstraints build() {
      return new RowListConstraints(this);
    }

    private Builder() {}

    private Builder(RowListConstraints constraints) {
      mMaxActions = constraints.mMaxActions;
      mRowConstraints = constraints.mRowConstraints;
      mAllowSelectableLists = constraints.mAllowSelectableLists;
      mListContentType = constraints.mListContentType;
    }
  }
}
