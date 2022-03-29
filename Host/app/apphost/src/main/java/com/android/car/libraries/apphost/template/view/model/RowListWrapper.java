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

import static com.android.car.libraries.apphost.template.view.model.RowWrapper.ROW_FLAG_NONE;

import android.content.Context;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.CarText;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.OnItemVisibilityChangedDelegate;
import androidx.car.app.model.OnSelectedDelegate;
import androidx.car.app.model.Pane;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import androidx.car.app.model.SectionedItemList;
import androidx.car.app.model.Toggle;
import com.android.car.libraries.apphost.distraction.constraints.RowConstraints;
import com.android.car.libraries.apphost.distraction.constraints.RowListConstraints;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.template.view.model.RowWrapper.RowFlags;
import com.google.common.collect.ImmutableList;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * A host side wrapper for both {@link ItemList} and {@link Pane} to allow additional metadata such
 * as a {@link androidx.car.app.model.Place} for each individual row and/or {@link
 * RowListConstraints}.
 */
public class RowListWrapper {
  /** Represents different flags to determine how to render the list. */
  // TODO(b/174601019): clean this up along with RowFlags
  @IntDef(
      flag = true,
      value = {
        LIST_FLAGS_NONE,
        LIST_FLAGS_SELECTABLE_USE_RADIO_BUTTONS,
        LIST_FLAGS_SELECTABLE_HIGHLIGHT_ROW,
        LIST_FLAGS_SELECTABLE_FOCUS_SELECT_ROW,
        LIST_FLAGS_SELECTABLE_SCROLL_TO_ROW,
        LIST_FLAGS_RENDER_TITLE_AS_SECONDARY,
        LIST_FLAGS_TEMPLATE_HAS_LARGE_IMAGE,
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface ListFlags {}

  public static final int LIST_FLAGS_NONE = (1 << 0);

  /** The list is selectable, and selection should be rendered with radio buttons. */
  public static final int LIST_FLAGS_SELECTABLE_USE_RADIO_BUTTONS = (1 << 1);

  /** The list is selectable, and selection should be rendered by highlighting the row. */
  public static final int LIST_FLAGS_SELECTABLE_HIGHLIGHT_ROW = (1 << 2);

  /** The list is selectable, and focus on a row would select it. */
  public static final int LIST_FLAGS_SELECTABLE_FOCUS_SELECT_ROW = (1 << 3);

  /** The list is selectable, and selection will scroll the list to the selected row. */
  public static final int LIST_FLAGS_SELECTABLE_SCROLL_TO_ROW = (1 << 4);

  /** Renders the title of the rows as secondary text. */
  public static final int LIST_FLAGS_RENDER_TITLE_AS_SECONDARY = (1 << 5);

  /** Whether the list is placed alongside an image that needs to scroll with the list. */
  public static final int LIST_FLAGS_TEMPLATE_HAS_LARGE_IMAGE = (1 << 6);

  /** Whether the list should hide the dividers between the rows. */
  public static final int LIST_FLAGS_HIDE_ROW_DIVIDERS = (1 << 7);

  /** The default flags to use for selectable lists. */
  private static final int DEFAULT_SELECTABLE_LIST_FLAGS = LIST_FLAGS_SELECTABLE_USE_RADIO_BUTTONS;

  private final boolean mIsLoading;
  private final boolean mIsRefresh;
  @Nullable private final List<Object> mRowList;
  @Nullable private final CarText mEmptyListText;
  @Nullable private final CarIcon mImage;
  @Nullable private final OnItemVisibilityChangedDelegate mOnItemVisibilityChangedDelegate;
  @Nullable private final Runnable mOnRepeatedSelectionCallback;
  private final List<RowWrapper> mRowWrappers;
  private final RowListConstraints mRowListConstraints;
  @ListFlags private final int mListFlags;
  private final boolean mIsHalfList;

  /** Returns a builder of {@link RowListWrapper} that wraps the given {@link ItemList}. */
  public static Builder wrap(Context context, @Nullable ItemList itemList) {
    if (itemList == null) {
      return new Builder(context);
    }

    @SuppressWarnings("unchecked")
    List<Object> rows = (List) itemList.getItems();
    Builder builder =
        new Builder(context)
            .setRows(rows)

            // Set the default flags for the list, which can be overridden by the caller
            // to the builder.
            .setListFlags(getDefaultListFlags(itemList))
            .setEmptyListText(itemList.getNoItemsMessage())
            .setOnItemVisibilityChangedDelegate(itemList.getOnItemVisibilityChangedDelegate());

    OnSelectedDelegate onSelectedDelegate = itemList.getOnSelectedDelegate();
    if (onSelectedDelegate != null) {
      // Create a selection group for the rows that encompasses the entire list.
      // The selection groups keep a mutable selection index, and allow for having multiple
      // selection groups (e.g. different sections of radio buttons) within the same list.
      builder.setSelectionGroup(
          SelectionGroup.create(
              0, rows.size() - 1, itemList.getSelectedIndex(), onSelectedDelegate));
    }

    return builder;
  }

  /** Returns a builder of {@link RowListWrapper} that wraps the given {@link SectionedItemList}. */
  public static Builder wrap(Context context, List<SectionedItemList> sectionLists) {
    if (sectionLists.isEmpty()) {
      return new Builder(context);
    }

    @SuppressWarnings("unchecked")
    List<Object> rows = (List) sectionLists;
    return new Builder(context).setRows(rows);
  }

  /** Returns a builder of {@link RowListWrapper} that wraps the given {@link Pane}. */
  public static Builder wrap(Context context, @Nullable Pane pane) {
    if (pane == null) {
      L.w(LogTags.TEMPLATE, "Pane is expected on the template but not set");
      return new Builder(context);
    }

    // TODO(b/205522074): large image and dividers are specific to pane and the UI hierarchy between
    // list and pane is diverging more and more. Investigate whether we can decouple the two.
    int flags = LIST_FLAGS_HIDE_ROW_DIVIDERS;
    if (pane.getImage() != null) {
      flags |= LIST_FLAGS_TEMPLATE_HAS_LARGE_IMAGE;
    }

    return new Builder(context)
        .setRows(new ArrayList<>(pane.getRows()))
        .setListFlags(flags)
        .setImage(pane.getImage())
        .setIsLoading(pane.isLoading());
  }

  /** Returns the list flags to use by default for the given {@link ItemList}. */
  @ListFlags
  public static int getDefaultListFlags(@Nullable ItemList itemList) {
    return itemList != null && itemList.getOnSelectedDelegate() != null
        ? DEFAULT_SELECTABLE_LIST_FLAGS
        : LIST_FLAGS_NONE;
  }

  /** Returns a builder configured with the values from this {@link RowListWrapper} instance. */
  public Builder newBuilder(Context context) {
    return new Builder(context, this);
  }

  /**
   * Returns the list of rows that make up this list.
   *
   * @see Builder#setRows(List)
   */
  @Nullable
  public List<Object> getRows() {
    return mRowList == null ? null : ImmutableList.copyOf(mRowList);
  }

  /** Returns the image that should be shown alongside the row list. */
  @Nullable
  public CarIcon getImage() {
    return mImage;
  }

  /**
   * Returns the flags that control how to render the list.
   *
   * @see Builder#setListFlags(int)
   */
  @ListFlags
  public int getListFlags() {
    return mListFlags;
  }

  /**
   * Returns the delegate to use to notify when when the visibility of items in the list change, for
   * example during scroll.
   *
   * @see Builder#setOnItemVisibilityChangedDelegate(OnItemVisibilityChangedDelegate)
   */
  @Nullable
  public OnItemVisibilityChangedDelegate getOnItemVisibilityChangedDelegate() {
    return mOnItemVisibilityChangedDelegate;
  }

  /**
   * Returns the callback for when a row is repeatedly selected.
   *
   * @see Builder#setOnRepeatedSelectionCallback
   */
  @Nullable
  public Runnable getRepeatedSelectionCallback() {
    return mOnRepeatedSelectionCallback;
  }

  /** Returns whether the list has no rows. */
  public boolean isEmpty() {
    return mRowWrappers.isEmpty();
  }

  /**
   * Returns the {@link RowListConstraints} that define the restrictions to apply to the list.
   *
   * @see Builder#setRowListConstraints(RowListConstraints)
   */
  public RowListConstraints getRowListConstraints() {
    return mRowListConstraints;
  }

  /**
   * Returns the text to display when the list is empty or {@code null} to not display any text.
   *
   * @see Builder#setEmptyListText(CarText)
   */
  @Nullable
  public CarText getEmptyListText() {
    return mEmptyListText;
  }

  /** Returns the list of {@link RowWrapper} instances that wrap the rows in the list. */
  public List<RowWrapper> getRowWrappers() {
    return mRowWrappers;
  }

  /**
   * Returns whether the list is in loading state.
   *
   * @see Builder#setIsLoading(boolean)
   */
  public boolean isLoading() {
    return mIsLoading;
  }

  /**
   * Returns whether the list is in loading state.
   *
   * @see Builder#setIsRefresh(boolean)
   */
  public boolean isRefresh() {
    return mIsRefresh;
  }

  /**
   * Returns whether this is a half list, as opposed to a full width list.
   *
   * @see Builder#setIsHalfList(boolean)
   */
  public boolean isHalfList() {
    return mIsHalfList;
  }

  /**
   * Builds the {@link RowWrapper}s for a given list, expanding any sub-lists embedded within it.
   */
  @SuppressWarnings("RestrictTo")
  private static ImmutableList<RowWrapper> buildRowWrappers(
      Context context,
      @Nullable List<Object> rowList,
      @Nullable SelectionGroup selectionGroup,
      RowListConstraints rowListConstraints,
      @Nullable CarText selectedText,
      @RowFlags int rowFlags,
      @ListFlags int listFlags,
      int startIndex,
      boolean isHalfList) {
    if (rowList == null || rowList.isEmpty()) {
      return ImmutableList.of();
    }

    // If selectable lists are disallowed, set the selection group to null, which effectively
    // disables selection.
    if (!rowListConstraints.getAllowSelectableLists()) {
      L.w(LogTags.TEMPLATE, "Selectable lists disallowed for template this list");
      selectionGroup = null;
    }

    int labelIndex = 1;
    ImmutableList.Builder<RowWrapper> wrapperListBuilder = new ImmutableList.Builder<>();

    // Sub-lists are expanded inline in this list and become part of it. This size is the
    // number of rows accounting for any such sub-list expansions.
    int expandedSize = 0;

    for (Object rowObj : rowList) {
      // The row is a sub-list: we will expand it and add its rows to the parent list.
      if (rowObj instanceof SectionedItemList) {
        SectionedItemList section = (SectionedItemList) rowObj;
        ItemList subList = section.getItemList();

        if (subList == null || subList.getItems().isEmpty()) {
          // This should never happen as the client side should prevent empty sub-lists.
          L.e(LogTags.TEMPLATE, "Found empty sub-list, skipping...");
          continue;
        }

        CarText header = section.getHeader();
        if (header == null) {
          // This should never happen as the client side should prevent null headers.
          L.e(LogTags.TEMPLATE, "Header is expected on the section but not set, skipping...");
          continue;
        }

        // Create a row representing the header.
        Row headerRow = new Row.Builder().setTitle(header.toCharSequence()).build();
        wrapperListBuilder.add(
            RowWrapper.wrap(headerRow, startIndex + expandedSize)
                .setListFlags(listFlags)
                .setRowFlags(rowFlags | RowWrapper.ROW_FLAG_SECTION_HEADER)
                .setIsHalfList(isHalfList)
                .setRowConstraints(rowListConstraints.getRowConstraints())
                .build());
        expandedSize++;

        // Create wrappers for each row in the sublist.
        int subListSize = subList.getItems().size();
        List<RowWrapper> subWrappers =
            createRowWrappersForSublist(
                context,
                subList,
                expandedSize,
                rowListConstraints,
                selectedText,
                rowFlags,
                listFlags,
                isHalfList);
        wrapperListBuilder.addAll(subWrappers);
        expandedSize += subListSize;
      } else {
        RowWrapper.Builder wrapperBuilder = RowWrapper.wrap(rowObj, startIndex + expandedSize);
        RowConstraints rowConstraints = rowListConstraints.getRowConstraints();
        if (rowObj instanceof Row) {
          Row row = (Row) rowObj;
          labelIndex = addMetadataToRowWrapper(row, wrapperBuilder, labelIndex);

          Toggle toggle = row.getToggle();
          if (toggle != null) {
            wrapperBuilder.setIsToggleChecked(toggle.isChecked());
          }

          wrapperBuilder.setSelectedText(selectedText);
        }

        wrapperListBuilder.add(
            wrapperBuilder
                .setRowFlags(rowFlags)
                .setListFlags(listFlags)
                .setIsHalfList(isHalfList)
                .setSelectionGroup(selectionGroup)
                .setRowConstraints(rowConstraints)
                .build());

        expandedSize++;
      }
    }

    return wrapperListBuilder.build();
  }

  /**
   * Adds any metadata from the original {@link Row} to its {@link RowWrapper}.
   *
   * <p>If a {@link Row} contains a default marker, this updates the marker to render a string based
   * on the given {@code labelIndex}.
   *
   * @return the updated label index value that should be used for the next default marker
   */
  private static int addMetadataToRowWrapper(
      Row row, RowWrapper.Builder wrapperBuilder, int labelIndex) {
    Metadata metadata = row.getMetadata();
    if (metadata != null) {
      Place place = metadata.getPlace();
      if (place != null) {
        CarLocation location = place.getLocation();
        if (location != null) {
          // Assign any default markers (without text/icon) to show an integer value.
          PlaceMarker marker = place.getMarker();
          if (isDefaultMarker(marker)) {
            PlaceMarker.Builder markerBuilder =
                new PlaceMarker.Builder().setLabel(Integer.toString(labelIndex));
            if (marker != null) {
              CarColor markerColor = marker.getColor();
              if (markerColor != null) {
                markerBuilder.setColor(markerColor);
              }
              place = new Place.Builder(location).setMarker(markerBuilder.build()).build();
              metadata = new Metadata.Builder(metadata).setPlace(place).build();
            }
            labelIndex++;
          }
        }
      }

      // Sets the metadata in the wrapper with the updated marker if set.
      wrapperBuilder.setMetadata(metadata);
    }

    return labelIndex;
  }

  private static boolean isDefaultMarker(@Nullable PlaceMarker marker) {
    return marker != null && marker.getIcon() == null && marker.getLabel() == null;
  }

  @SuppressWarnings("unchecked")
  private static ImmutableList<RowWrapper> createRowWrappersForSublist(
      Context context,
      ItemList subList,
      int currentIndex,
      RowListConstraints rowListConstraints,
      @Nullable CarText selectedText,
      @RowFlags int rowFlags,
      @ListFlags int listFlags,
      boolean isHalfList) {
    // Create a selection group for this sub-list.
    // Offset its indices it by the expanded size to account for any previously expanded
    // sub-lists.
    OnSelectedDelegate onSelectedDelegate = subList.getOnSelectedDelegate();
    SelectionGroup subSelectionGroup =
        onSelectedDelegate != null
            ? SelectionGroup.create(
                currentIndex,
                currentIndex + subList.getItems().size() - 1,
                currentIndex + subList.getSelectedIndex(),
                onSelectedDelegate)
            : null;

    return buildRowWrappers(
        context,
        (List) subList.getItems(),
        subSelectionGroup,
        rowListConstraints,
        selectedText,
        rowFlags,
        listFlags == 0 ? getDefaultListFlags(subList) : listFlags,
        /* startIndex = */ currentIndex,
        isHalfList);
  }

  private RowListWrapper(Builder builder) {
    mIsLoading = builder.mIsLoading;
    mRowList = builder.mRowList;
    mImage = builder.mImage;
    mEmptyListText = builder.mEmptyListText;
    mOnRepeatedSelectionCallback = builder.mOnRepeatedSelectionCallback;
    mOnItemVisibilityChangedDelegate = builder.mOnItemVisibilityChangedDelegate;
    mRowListConstraints = builder.mRowListConstraints;
    mListFlags = builder.mListFlags;
    mIsRefresh = builder.mIsRefresh;
    mIsHalfList = builder.mIsHalfList;
    mRowWrappers =
        buildRowWrappers(
            builder.mContext,
            builder.mRowList,
            builder.mSelectionGroup,
            builder.mRowListConstraints,
            builder.mSelectedText,
            builder.mRowFlags,
            builder.mListFlags,
            /* startIndex = */ 0,
            builder.mIsHalfList);
  }

  /** The builder class for {@link RowListWrapper}. */
  public static class Builder {
    private final Context mContext;
    @Nullable SelectionGroup mSelectionGroup;
    @Nullable private List<Object> mRowList;
    @RowFlags private int mRowFlags = ROW_FLAG_NONE;
    @ListFlags private int mListFlags;
    private RowListConstraints mRowListConstraints =
        RowListConstraints.ROW_LIST_CONSTRAINTS_CONSERVATIVE;
    @Nullable private Runnable mOnRepeatedSelectionCallback;
    @Nullable private OnItemVisibilityChangedDelegate mOnItemVisibilityChangedDelegate;
    private boolean mIsLoading;
    private boolean mIsRefresh;
    @Nullable private CarText mEmptyListText;
    @Nullable private CarText mSelectedText;
    @Nullable private CarIcon mImage;
    private boolean mIsHalfList;

    private Builder(Context context) {
      mContext = context;
      mRowList = null;
    }

    private Builder(Context context, RowListWrapper rowListWrapper) {
      mContext = context;
      mRowList = rowListWrapper.mRowList;
      mImage = rowListWrapper.mImage;
      mListFlags = rowListWrapper.mListFlags;
      mRowListConstraints = rowListWrapper.mRowListConstraints;
      mIsLoading = rowListWrapper.mIsLoading;
      mIsRefresh = rowListWrapper.mIsRefresh;
      mIsHalfList = rowListWrapper.mIsHalfList;
      mEmptyListText = rowListWrapper.mEmptyListText;
      mOnItemVisibilityChangedDelegate = rowListWrapper.mOnItemVisibilityChangedDelegate;
      mOnRepeatedSelectionCallback = rowListWrapper.mOnRepeatedSelectionCallback;
    }

    /** Sets the set of rows that make up the list. */
    public Builder setRows(@Nullable List<Object> rowList) {
      mRowList = rowList;
      return this;
    }

    /** Sets the image to be shown alongside the rows. */
    public Builder setImage(@Nullable CarIcon image) {
      mImage = image;
      return this;
    }

    /** Set an extra callback for when a row in the list has been repeatedly selected. */
    public Builder setOnRepeatedSelectionCallback(@Nullable Runnable runnable) {
      mOnRepeatedSelectionCallback = runnable;
      return this;
    }

    /**
     * Sets the delegate to use to notify when when the visibility of items in the list change, for
     * example, during scroll.
     */
    public Builder setOnItemVisibilityChangedDelegate(
        @Nullable OnItemVisibilityChangedDelegate delegate) {
      mOnItemVisibilityChangedDelegate = delegate;
      return this;
    }

    /**
     * Sets whether the list is loading.
     *
     * <p>If set to {@code true}, the UI shows a loading indicator and ignore any rows added to the
     * list. If set to {@code false}, the UI shows the actual row contents.
     */
    public Builder setIsLoading(boolean isLoading) {
      mIsLoading = isLoading;
      return this;
    }

    /**
     * Sets whether the list is a refresh of the existing list.
     *
     * <p>If set to {@code true}, the UI will not scroll to top, otherwise it will.
     */
    public Builder setIsRefresh(boolean isRefresh) {
      mIsRefresh = isRefresh;
      return this;
    }

    /** Sets the text to add to a row when it is selected. */
    public Builder setRowSelectedText(@Nullable CarText selectedText) {
      mSelectedText = selectedText;
      return this;
    }

    /** Sets the text to display when the list is empty or {@code null} to not display any text. */
    public Builder setEmptyListText(@Nullable CarText emptyListText) {
      mEmptyListText = emptyListText;
      return this;
    }

    /**
     * Sets a selection group for this list.
     *
     * <p>Selection groups are used for defining a mutually-exclusive selectable range of rows,
     * which can be used for example for radio buttons.
     *
     * @see SelectionGroup
     */
    public Builder setSelectionGroup(@Nullable SelectionGroup selectionGroup) {
      mSelectionGroup = selectionGroup;
      return this;
    }

    /**
     * Set whether the list is a "half" list.
     *
     * <p>"Half list " is the term we use for lists that don't span the entire width of the screen
     * (e.g. inside of a card in a map template). Note these don't necessarily take exactly half the
     * width (depending on the screen width and how the card may adapt to it).
     */
    public Builder setIsHalfList(boolean isHalfList) {
      mIsHalfList = isHalfList;
      return this;
    }

    /** Sets the flags that control how to render individual rows. */
    public Builder setRowFlags(@RowFlags int rowFlags) {
      mRowFlags = rowFlags;
      return this;
    }

    /** Sets the flags that control how to render the list. */
    public Builder setListFlags(@ListFlags int listFlags) {
      mListFlags = listFlags;
      return this;
    }

    /** Sets the {@link RowListConstraints} that define the restrictions to apply to the list. */
    public Builder setRowListConstraints(RowListConstraints constraints) {
      mRowListConstraints = constraints;
      return this;
    }

    /** Constructs a {@link RowListWrapper} instance from this builder. */
    public RowListWrapper build() {
      return new RowListWrapper(this);
    }
  }
}
