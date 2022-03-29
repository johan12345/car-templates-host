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

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.Action;
import java.util.HashSet;
import java.util.Set;

/** Encapsulates the constraints to apply when rendering a list of {@link Action}s on a template. */
public class ActionsConstraints {
  /** Conservative constraints for most template types. */
  private static final ActionsConstraints ACTIONS_CONSTRAINTS_CONSERVATIVE =
      ActionsConstraints.builder().setMaxActions(2).build();

  /**
   * Constraints for template headers, where only the special-purpose back and app-icon standard
   * actions are allowed.
   */
  public static final ActionsConstraints ACTIONS_CONSTRAINTS_HEADER =
      ActionsConstraints.builder().setMaxActions(1).addDisallowedAction(Action.TYPE_CUSTOM).build();

  /** Default constraints that should be applied to most templates (2 actions, 1 can have title). */
  public static final ActionsConstraints ACTIONS_CONSTRAINTS_SIMPLE =
      ACTIONS_CONSTRAINTS_CONSERVATIVE.newBuilder().setMaxCustomTitles(1).build();

  /** Constraints for navigation templates. */
  public static final ActionsConstraints ACTIONS_CONSTRAINTS_NAVIGATION =
      ACTIONS_CONSTRAINTS_CONSERVATIVE
          .newBuilder()
          .setMaxActions(4)
          .setMaxCustomTitles(1)
          .addRequiredAction(Action.TYPE_CUSTOM)
          .build();

  /** Constraints for navigation templates. */
  public static final ActionsConstraints ACTIONS_CONSTRAINTS_NAVIGATION_MAP =
      ACTIONS_CONSTRAINTS_CONSERVATIVE.newBuilder().setMaxActions(4).build();

  private final int mMaxActions;
  private final int mMaxCustomTitles;
  private final Set<Integer> mRequiredActionTypes;
  private final Set<Integer> mDisallowedActionTypes;

  /** Returns a builder of {@link ActionsConstraints}. */
  @VisibleForTesting
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns a new builder that contains the same data as this {@link ActionsConstraints} instance,
   */
  @VisibleForTesting
  public Builder newBuilder() {
    return new Builder(this);
  }

  /** Returns the max number of actions allowed. */
  public int getMaxActions() {
    return mMaxActions;
  }

  /** Returns the max number of actions with custom titles allowed. */
  public int getMaxCustomTitles() {
    return mMaxCustomTitles;
  }

  /** Adds the set of required action types. */
  @NonNull
  public Set<Integer> getRequiredActionTypes() {
    return mRequiredActionTypes;
  }

  /** Adds the set of disallowed action types. */
  @NonNull
  public Set<Integer> getDisallowedActionTypes() {
    return mDisallowedActionTypes;
  }

  /** A builder of {@link ActionsConstraints}. */
  @VisibleForTesting
  public static class Builder {
    private int mMaxActions = Integer.MAX_VALUE;
    private int mMaxCustomTitles;
    private final Set<Integer> mRequiredActionTypes = new HashSet<>();
    private final Set<Integer> mDisallowedActionTypes = new HashSet<>();

    /** Sets the maximum number of actions allowed. */
    public Builder setMaxActions(int maxActions) {
      mMaxActions = maxActions;
      return this;
    }

    /** Sets the maximum number of actions with custom titles allowed. */
    public Builder setMaxCustomTitles(int maxCustomTitles) {
      mMaxCustomTitles = maxCustomTitles;
      return this;
    }

    /** Adds an action type to the set of required types. */
    public Builder addRequiredAction(int actionType) {
      mRequiredActionTypes.add(actionType);
      return this;
    }

    /** Adds an action type to the set of disallowed types. */
    public Builder addDisallowedAction(int actionType) {
      mDisallowedActionTypes.add(actionType);
      return this;
    }

    /** TODO(b/174880910): Adding javadoc for AOSP */
    public ActionsConstraints build() {
      return new ActionsConstraints(this);
    }

    private Builder() {}

    private Builder(ActionsConstraints constraints) {
      mMaxActions = constraints.mMaxActions;
      mMaxCustomTitles = constraints.mMaxCustomTitles;
      mRequiredActionTypes.addAll(constraints.mRequiredActionTypes);
      mDisallowedActionTypes.addAll(constraints.mDisallowedActionTypes);
    }
  }

  private ActionsConstraints(Builder builder) {
    mMaxActions = builder.mMaxActions;
    mMaxCustomTitles = builder.mMaxCustomTitles;
    mRequiredActionTypes = new HashSet<>(builder.mRequiredActionTypes);

    if (!builder.mDisallowedActionTypes.isEmpty()) {
      Set<Integer> disallowedActionTypes = new HashSet<>(builder.mDisallowedActionTypes);
      disallowedActionTypes.retainAll(mRequiredActionTypes);
      if (!disallowedActionTypes.isEmpty()) {
        throw new IllegalArgumentException(
            "Disallowed action types cannot also be in the required set.");
      }
    }
    mDisallowedActionTypes = new HashSet<>(builder.mDisallowedActionTypes);

    if (mRequiredActionTypes.size() > mMaxActions) {
      throw new IllegalArgumentException("Required action types exceeded max allowed actions.");
    }
  }
}
