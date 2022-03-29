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
package com.android.car.libraries.apphost.logging;

import com.google.auto.value.AutoValue;

/** Internal representation of the content limit queried by car app */
@AutoValue
public abstract class ContentLimitQuery {

  /** Returns the content limit type */
  public abstract int getContentLimitType();

  /** Returns the content limit value */
  public abstract int getContentLimitValue();

  /**
   * Returns a new builder of {@link ContentLimitQuery} set up with the information from this event.
   */
  public static ContentLimitQuery.Builder newBuilder() {
    return new AutoValue_ContentLimitQuery.Builder();
  }

  /** ContentLimit builder. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Sets the content limits {@code type}. */
    public abstract Builder setContentLimitType(int type);

    /** Sets the content limits {@code value}. */
    public abstract Builder setContentLimitValue(int value);

    /** Builds a {@link ContentLimitQuery} from this builder. */
    public ContentLimitQuery build() {
      return autoBuild();
    }

    abstract ContentLimitQuery autoBuild();
  }

  /** Returns a {@link ContentLimitQuery} with given {@code type} and {@code value}. */
  public static ContentLimitQuery getContentLimitQuery(int type, int value) {
    return ContentLimitQuery.newBuilder()
        .setContentLimitValue(value)
        .setContentLimitType(type)
        .build();
  }
}
