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

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.Place;
import androidx.car.app.model.Row;
import com.android.car.libraries.apphost.distraction.constraints.RowConstraints;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** A host side wrapper for {@link Row} which can include extra metadata such as a {@link Place}. */
public class RowWrapper {
  /** Represents flags that control some attributes of the row. */
  // TODO(b/174601019): clean this up along with ListFlags
  @IntDef(
      value = {ROW_FLAG_NONE, ROW_FLAG_SHOW_DIVIDERS, ROW_FLAG_SECTION_HEADER},
      flag = true)
  @Retention(RetentionPolicy.SOURCE)
  public @interface RowFlags {}

  /** No flags applied to the row. */
  public static final int ROW_FLAG_NONE = (1 << 0);

  /** Whether to show dividers around the row. */
  public static final int ROW_FLAG_SHOW_DIVIDERS = (1 << 1);

  /**
   * Whether the row is a section header.
   *
   * <p>Sections are used to group rows in the UI, for example, by showing them all within a block
   * of the same background color.
   *
   * <p>A section header is a string of text above the section with a title for it.
   */
  public static final int ROW_FLAG_SECTION_HEADER = (1 << 2);

  /** The default flags to use for uniform lists. */
  public static final int DEFAULT_UNIFORM_LIST_ROW_FLAGS = ROW_FLAG_SHOW_DIVIDERS;

  private final Object mRow;
  private final int mRowIndex;
  private final Metadata mMetadata;
  @Nullable private final CarText mSelectedText;
  @RowFlags private final int mRowFlags;
  @RowListWrapper.ListFlags private final int mListFlags;
  private final RowConstraints mRowConstraints;
  private boolean mIsHalfList;

  /**
   * The selection group this row belongs to, or {@code null} if the row does not belong to one.
   *
   * <p>Selection groups are used to establish mutually-exclusive scopes of row selection, for
   * example, to implement radio button groups.
   *
   * <p>The selection index in the group is mutable and allows for automatically updating it the UI
   * at the host without a round-trip to the client to change the selection there.
   */
  @Nullable private final SelectionGroup mSelectionGroup;

  /**
   * Whether the toggle is checked.
   *
   * <p>This field is mutable so that we can remember toggle changes on the host without having to
   * round-trip to the client when toggle states change.
   *
   * <p>It is initialized with the initial value from the model coming from the client, then can
   * mutate after.
   */
  private boolean mIsToggleChecked;

  /** Returns a {@link Builder} that wraps a row with the provided index. */
  public static Builder wrap(Object row, int rowIndex) {
    return new Builder(row, rowIndex);
  }

  @Override
  public String toString() {
    return "[" + mRow + ", group: " + mSelectionGroup + "]";
  }

  /** Returns the actual {@link Row} object that this instance is wrapping. */
  public Object getRow() {
    return mRow;
  }

  /** Returns the absolute index of the row in the flattened container list. */
  public int getRowIndex() {
    return mRowIndex;
  }

  /** Returns the {@link Metadata} that is associated with the row. */
  public Metadata getMetadata() {
    return mMetadata;
  }

  /** Returns the {@link CarText} that should be displayed in the row when it has focus. */
  @Nullable
  public CarText getSelectedText() {
    return mSelectedText;
  }

  /**
   * Returns the flags that control how to render this row.
   *
   * @see Builder#setRowFlags(int)
   */
  @RowFlags
  public int getRowFlags() {
    return mRowFlags;
  }

  /**
   * Returns the flags that control how to render the list this row belongs to.
   *
   * @see Builder#setListFlags
   */
  @RowListWrapper.ListFlags
  public int getListFlags() {
    return mListFlags;
  }

  /**
   * Returns whether the row belongs to a "half" list.
   *
   * @see Builder#setIsHalfList(boolean)
   */
  public boolean isHalfList() {
    return mIsHalfList;
  }

  /**
   * Returns the selection group this row belongs to.
   *
   * @see Builder#setSelectionGroup(SelectionGroup)
   */
  @Nullable
  public SelectionGroup getSelectionGroup() {
    return mSelectionGroup;
  }

  /**
   * Returns whether the toggle in the row, if there is one, is checked.
   *
   * @see Builder#setIsToggleChecked(boolean)
   */
  public boolean isToggleChecked() {
    return mIsToggleChecked;
  }

  /** Checks the toggle in the row if unchecked, and vice-versa. */
  public void switchToggleState() {
    mIsToggleChecked = !mIsToggleChecked;
  }

  /**
   * Returns the {@link RowConstraints} that define the restrictions to apply to the row.
   *
   * @see Builder#setRowConstraints(RowConstraints)
   */
  public RowConstraints getRowConstraints() {
    return mRowConstraints;
  }

  private RowWrapper(Builder builder) {
    mRow = builder.mRow;
    mRowIndex = builder.mRowIndex;
    mMetadata = builder.mEmptyMetadata;
    mSelectedText = builder.mSelectedText;
    mRowFlags = builder.mRowFlags;
    mListFlags = builder.mListFlags;
    mSelectionGroup = builder.mSelectionGroup;
    mIsToggleChecked = builder.mIsToggleChecked;
    mRowConstraints = builder.mRowConstraints;
    mIsHalfList = builder.mIsHalfList;
  }

  /** The builder class for {@link RowWrapper}. */
  public static class Builder {
    private final Object mRow;
    private final int mRowIndex;
    private Metadata mEmptyMetadata = Metadata.EMPTY_METADATA;
    @RowListWrapper.ListFlags private int mListFlags;
    @RowFlags private int mRowFlags = ROW_FLAG_NONE;
    @Nullable private SelectionGroup mSelectionGroup;
    private boolean mIsToggleChecked;
    @Nullable private CarText mSelectedText;
    private RowConstraints mRowConstraints = RowConstraints.ROW_CONSTRAINTS_CONSERVATIVE;
    private boolean mIsHalfList;

    /** Sets the {@link Metadata} associated with this row. */
    public Builder setMetadata(Metadata metadata) {
      mEmptyMetadata = metadata;
      return this;
    }

    /** Sets the text to display in the row when it is selected. */
    public Builder setSelectedText(@Nullable CarText selectedText) {
      mSelectedText = selectedText;
      return this;
    }

    /** Sets the flags that control how to render this row. */
    public Builder setRowFlags(@RowFlags int rowFlags) {
      mRowFlags = rowFlags;
      return this;
    }

    /** Sets the flags that control how to render the list this row belongs to. */
    public Builder setListFlags(@RowListWrapper.ListFlags int listFlags) {
      mListFlags = listFlags;
      return this;
    }

    /**
     * Set whether the list this row belongs to is a "half" list.
     *
     * <p>"Half list " is the term we use for lists that don't span the entire width of the screen
     * (e.g. inside of a card in a map template). Note these don't necessarily take exactly half the
     * width (depending on the screen width and how the card may adapt to it).
     */
    public Builder setIsHalfList(boolean isHalfList) {
      mIsHalfList = isHalfList;
      return this;
    }

    /** Sets the selection group this row belongs to. */
    public Builder setSelectionGroup(@Nullable SelectionGroup selectionGroup) {
      mSelectionGroup = selectionGroup;
      return this;
    }

    /** Sets whether the toggle in the row, if there is one, should be displayed checked. */
    public Builder setIsToggleChecked(boolean isToggleChecked) {
      mIsToggleChecked = isToggleChecked;
      return this;
    }

    /** Sets the {@link RowConstraints} that define the restrictions to apply to the row. */
    public Builder setRowConstraints(RowConstraints rowConstraints) {
      mRowConstraints = rowConstraints;
      return this;
    }

    /** Constructs a {@link RowWrapper} instance from this builder. */
    public RowWrapper build() {
      return new RowWrapper(this);
    }

    private Builder(Object row, int rowIndex) {
      mRow = row;
      mRowIndex = rowIndex;
    }
  }
}
