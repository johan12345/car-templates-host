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

import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.os.Build.VERSION_CODES;

import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.OnCheckedChangeDelegate;
import androidx.car.app.model.OnClickDelegate;
import androidx.car.app.model.OnItemVisibilityChangedDelegate;
import androidx.car.app.model.Place;
import androidx.car.app.model.Row;
import androidx.car.app.model.Toggle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import com.android.car.libraries.apphost.common.LocationMediator;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.RowListConstraints;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.TelemetryEvent;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import com.android.car.libraries.apphost.logging.TelemetryHandler;
import com.android.car.libraries.apphost.template.view.model.RowListWrapper;
import com.android.car.libraries.apphost.template.view.model.RowWrapper;
import com.android.car.libraries.apphost.template.view.model.SelectionGroup;
import com.android.car.libraries.apphost.view.common.ImageUtils;
import com.android.car.libraries.apphost.view.common.ImageViewParams;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.MarkerFactory.MarkerAppearance;
import com.android.car.libraries.templates.host.view.widgets.common.RowHolder.RowListener;
import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiListItem;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.recyclerview.CarUiRecyclerView.OnScrollListener;
import com.android.car.ui.widget.CarUiTextView;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/**
 * A view that can render a list of {@link androidx.car.app.model.Row} (wrapped inside a {@link
 * RowListWrapper}.
 */
public class RowListView extends FrameLayout {
  private final AdapterDataObserver mAdapterDataObserver =
      new AdapterDataObserver() {
        // call to update() not allowed on the given receiver.
        @SuppressWarnings("nullness:method.invocation")
        @Override
        public void onChanged() {
          super.onChanged();
          update();
        }

        @SuppressWarnings("nullness:method.invocation")
        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
          super.onItemRangeChanged(positionStart, itemCount, payload);
          update();
        }

        @SuppressWarnings("nullness:method.invocation")
        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
          super.onItemRangeMoved(fromPosition, toPosition, itemCount);
          update();
        }

        @SuppressWarnings("nullness:method.invocation")
        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
          super.onItemRangeInserted(positionStart, itemCount);
          update();
        }
      };

  private MarkerFactory mMarkerFactory;
  private RowAdapter mListAdapter;
  private RowVisibilityObserver mRowVisibilityObserver;
  private ViewGroup mProgressContainer;
  private CarUiTextView mEmptyListTextView;
  private CarUiRecyclerView mListView;
  private RowListWrapper mRowList;
  private boolean mIsLoading;
  private final boolean mUseCompactRowLayout;

  // This is only present in the full list view layout.
  @Nullable private ViewGroup mLargeImageContainer;

  // This is only present in the full list view layout.
  @Nullable private CarImageView mLargeImageView;
  private final float mLargeImageWidthRatio;
  private final int mLargeImageMaxWidth;
  private final float mLargeImageAspectRatio;
  private final int mRowListAndImagePadding;
  private final int mLargeImageEndPadding;
  private final int mLargeImageTopMargin;
  private boolean hasLaidOutLargeImage;

  public RowListView(Context context) {
    this(context, null);
  }

  public RowListView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RowListView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  @SuppressWarnings({"ResourceType", "nullness:method.invocation", "nullness:argument"})
  public RowListView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateRowListToLargeImageRatio,
      R.attr.templateRowListLargeImageContainerMaxWidth,
      R.attr.templateRowListLargeImageAspectRatio,
      R.attr.templateRowListAndImagePadding,
      R.attr.templateFullRowEndPadding,
    };
    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    mLargeImageWidthRatio = ta.getFloat(0, 0.f);
    int largeImageContainerMaxWidth = ta.getDimensionPixelSize(1, 0);
    mLargeImageAspectRatio = ta.getFloat(2, 0.f);
    mRowListAndImagePadding = ta.getDimensionPixelSize(3, 0);
    // The end padding should be consistent with what was used for the row item's end padding.
    mLargeImageEndPadding = ta.getDimensionPixelSize(4, 0);
    ta.recycle();
    mLargeImageMaxWidth =
        largeImageContainerMaxWidth - (mRowListAndImagePadding + mLargeImageEndPadding);
    mLargeImageTopMargin = context.getResources().getDimensionPixelSize(R.dimen.template_padding_2);

    ta = context.obtainStyledAttributes(attrs, R.styleable.RowListView, defStyleAttr, defStyleRes);
    mUseCompactRowLayout = ta.getBoolean(R.styleable.RowListView_listUseCompactRowLayout, false);
    ta.recycle();

    mMarkerFactory =
        MarkerFactory.create(
            context, new MarkerAppearance(context, attrs, defStyleAttr, defStyleAttr));
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    mProgressContainer = findViewById(R.id.progress_container);
    mEmptyListTextView = findViewById(R.id.list_no_items_text);
    mListView = findViewById(R.id.list_view);
    mRowVisibilityObserver = RowVisibilityObserver.create(requireNonNull(mListView));
    mListAdapter =
        RowAdapter.create(getContext(), new ArrayList<>(), mMarkerFactory, mUseCompactRowLayout);
    mListView.setAdapter(mListAdapter);

    // TODO(b/210167386): setItemAnimator will deprecate for sc+. We can still use the code below to
    // control the itemAnimator for qt and rvc
    if (Build.VERSION.SDK_INT <= VERSION_CODES.R) {
      ItemAnimator itemAnimatorNoDuration = new DefaultItemAnimator();
      itemAnimatorNoDuration.setAddDuration(0);
      itemAnimatorNoDuration.setChangeDuration(0);
      itemAnimatorNoDuration.setMoveDuration(0);
      itemAnimatorNoDuration.setRemoveDuration(0);
      mListView.setItemAnimator(itemAnimatorNoDuration);
    }

    mListAdapter.registerAdapterDataObserver(mAdapterDataObserver);

    ViewGroup imageViewContainer = findViewById(R.id.large_image_container);
    if (imageViewContainer != null) {
      mLargeImageContainer = imageViewContainer;
      mLargeImageView = imageViewContainer.findViewById(R.id.large_image);

      // Synchronize the scrolling of the list with the vertical offset of the image.
      mListView.addOnScrollListener(
          new OnScrollListener() {
            @Override
            public void onScrolled(CarUiRecyclerView recyclerView, int dx, int dy) {
              FrameLayout.LayoutParams layoutParams =
                  (FrameLayout.LayoutParams) imageViewContainer.getLayoutParams();
              layoutParams.topMargin -= dy;
              imageViewContainer.setLayoutParams(layoutParams);
            }

            @Override
            public void onScrollStateChanged(CarUiRecyclerView recyclerView, int newState) {
              // No-op.
            }
          });
    }

    update();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public CarUiRecyclerView getRecyclerView() {
    return mListView;
  }

  @Nullable
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public RowAdapter getAdapter() {
    return mListAdapter;
  }

  void setRowList(TemplateContext templateContext, RowListWrapper rowList) {
    mRowList = rowList;

    boolean isLoading = rowList.isLoading();
    if (mIsLoading != isLoading) {
      // Trigger a visibility update if the loading state has changed.
      mIsLoading = isLoading;
      update();

      if (mIsLoading) {
        // Scroll to the top so we will show the first row when the loading finishes.
        mListView.scrollToPosition(0);
      }
    }

    CarText emptyListCarText = rowList.getEmptyListText();
    CharSequence emptyText;
    if (emptyListCarText != null && !emptyListCarText.isEmpty()) {
      mEmptyListTextView.setText(
          CarUiTextUtils.fromCarText(
              templateContext, emptyListCarText, mEmptyListTextView.getMaxLines()));
    } else {
      emptyText =
          templateContext.getText(
              templateContext.getHostResourceIds().getTemplateListNoItemsText());
      mEmptyListTextView.setText(
          CarUiTextUtils.fromCharSequence(
              templateContext, emptyText, mEmptyListTextView.getMaxLines()));
    }

    CarIcon paneImage = rowList.getImage();
    ViewGroup imageViewContainer = mLargeImageContainer;
    if (imageViewContainer != null) {
      if (paneImage != null) {
        ImageUtils.setImageSrc(
            templateContext, paneImage, requireNonNull(mLargeImageView), ImageViewParams.DEFAULT);
      }
    }

    RowListConstraints constraints = rowList.getRowListConstraints();
    List<RowHolder> rowHolders =
        RowHolder.createHolders(templateContext, rowList.getRowWrappers(), constraints);

    mRowVisibilityObserver.setOnItemVisibilityChangedListener(
        (startIndexInclusive, endIndexExclusive) -> {
          OnItemVisibilityChangedDelegate onItemVisibilityChangedDelegate =
              rowList.getOnItemVisibilityChangedDelegate();
          if (onItemVisibilityChangedDelegate != null) {
            templateContext
                .getAppDispatcher()
                .dispatchItemVisibilityChanged(
                    onItemVisibilityChangedDelegate, startIndexInclusive, endIndexExclusive);
          }
          publishMetadata(
              templateContext, rowList.getRowWrappers(), startIndexInclusive, endIndexExclusive);
        });

    mListAdapter.setRows(
        templateContext,
        rowHolders,
        new RowListener() {
          @Override
          public void onRowClicked(int index) {
            TelemetryHandler telemetry = templateContext.getTelemetryHandler();
            telemetry.logCarAppTelemetry(
                TelemetryEvent.newBuilder(TelemetryEvent.UiAction.ROW_CLICKED).setPosition(index));

            onRowSelected(templateContext, index, /* clicked= */ true);
          }

          @Override
          public void onCheckedChange(int index) {
            TelemetryHandler telemetry = templateContext.getTelemetryHandler();
            telemetry.logCarAppTelemetry(
                TelemetryEvent.newBuilder(TelemetryEvent.UiAction.ROW_CLICKED).setPosition(index));

            maybeSwitchToggleState(templateContext, index);
          }

          @Override
          public void onRowFocusChanged(int index, boolean hasFocus) {
            RowListView.this.onRowFocused(templateContext, index, hasFocus);
          }
        });

    if (!rowList.isRefresh()) {
      mListView.scrollToPosition(0);
    }

    ViewUtils.logCarAppTelemetry(templateContext, UiAction.LIST_SIZE, rowHolders.size());
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    // The layout of the large image container is dependent on the final size and paddings of the
    // list items insider the RecyclerView. Here we obtain the bounds of the first item in the
    // RecyclerView and lines up the image container based on that.
    //
    // We only need to do this once at the beginning to place the image at the right position.
    // Subsequent synchronization is handled via the OnScrollListener.
    ViewGroup imageViewContainer = mLargeImageContainer;
    if (imageViewContainer == null || hasLaidOutLargeImage) {
      return;
    }

    int firstActualRowIndex = -1;
    List<? extends CarUiListItem> items = mListAdapter.getItems();
    // Find the first item that is a CarUiContentListItem which is used for an actual Row.
    // Action button lists and section headers use different CarUiListItem types.
    for (int i = 0; i < items.size(); i++) {
      CarUiListItem item = items.get(i);
      if (item instanceof CarUiContentListItem) {
        firstActualRowIndex = i;
        break;
      }
    }

    if (firstActualRowIndex == -1) {
      return;
    }

    View itemView = mListView.getRecyclerViewChildAt(firstActualRowIndex);
    if (itemView != null) {
      // Get the item view bounds relative to the RowListView container, and use that
      // to determine the offset for the image view.
      Rect itemViewBound = new Rect();
      itemView.getDrawingRect(itemViewBound);
      RowListView.this.offsetDescendantRectToMyCoords(itemView, itemViewBound);

      // Sets the bounding box based on desired width and aspect ratio.
      int imageWidth =
          min(
              mLargeImageMaxWidth,
              // Image width is a ratio of the total container width, accounting for the
              // padding we want from the row and the edge of the screen.
              (int) (mLargeImageWidthRatio * itemViewBound.width())
                  - (mRowListAndImagePadding + mLargeImageEndPadding));

      FrameLayout.LayoutParams imageParams =
          (FrameLayout.LayoutParams) imageViewContainer.getLayoutParams();
      imageParams.topMargin = itemViewBound.top + mLargeImageTopMargin;
      imageParams.setMarginEnd(
          RowListView.this.getRight() - itemViewBound.right + mLargeImageEndPadding);
      imageParams.width = imageWidth;
      imageParams.height = (int) (imageWidth * mLargeImageAspectRatio);
      imageViewContainer.setLayoutParams(imageParams);

      hasLaidOutLargeImage = true;
    }
  }

  private void onRowSelected(TemplateContext templateContext, int newIndex, boolean clicked) {
    RowWrapper rowWrapper = getRowWrapperIfValid(newIndex);
    if (rowWrapper == null) {
      return;
    }

    if (rowWrapper.getRow() instanceof Row) {
      Row row = (Row) rowWrapper.getRow();
      final OnClickDelegate onClickDelegate = row.getOnClickDelegate();
      if (onClickDelegate != null) {
        templateContext.getAppDispatcher().dispatchClick(onClickDelegate);
      }
    }

    SelectionGroup selectionGroup = rowWrapper.getSelectionGroup();

    // If the row belongs to a selection group, change the selection in the group if necessary.
    // This is done here in the host without having to do a round-trip to the client to change
    // the
    // model and re-fresh the entire list, which is much faster and more convenient for apps.
    if (selectionGroup != null) {
      int currentSelectionIndex = selectionGroup.getSelectedIndex();

      // If the selected index changed, deselect the previously selected row, and select the
      // new
      // one.
      boolean isRowPreviouslySelected = currentSelectionIndex == newIndex;
      if (!isRowPreviouslySelected) {
        // Store the new selection. This is important also in case the rows get re-created
        // during recycling so that they maintain the proper state.
        selectionGroup.setSelectedIndex(newIndex);

        mListAdapter.updateRow(currentSelectionIndex);
        mListAdapter.updateRow(newIndex);

        boolean shouldScrollToSelectedRow =
            (mRowList.getListFlags() & RowListWrapper.LIST_FLAGS_SELECTABLE_SCROLL_TO_ROW) != 0;
        if (shouldScrollToSelectedRow) {
          // Post to the main thread so that the scroll happens after the UI changes for
          // the selected state is completed.
          post(() -> mListView.smoothScrollToPosition(newIndex));
        }
      }

      // Dispatch the selection callbacks.
      // Note the selection event is dispatched regardless of selection index actually
      // changing.
      templateContext
          .getAppDispatcher()
          .dispatchSelected(
              selectionGroup.getOnSelectedDelegate(), selectionGroup.getRelativeIndex(newIndex));
      if (isRowPreviouslySelected && clicked) {
        Runnable runnable = mRowList.getRepeatedSelectionCallback();
        if (runnable != null) {
          runnable.run();
        }
      }
    }
  }

  private void onRowFocused(TemplateContext templateContext, int index, boolean hasFocus) {
    // Select the row if moving the focus should change the selection, and we have a new focus.
    boolean focusChangeSelection =
        (mRowList.getListFlags() & RowListWrapper.LIST_FLAGS_SELECTABLE_FOCUS_SELECT_ROW) != 0;
    if (focusChangeSelection && hasFocus) {
      onRowSelected(templateContext, index, /* clicked= */ false);
    }
  }

  /** Switches the toggle state of a row if it does contain a toggle. */
  private void maybeSwitchToggleState(TemplateContext templateContext, int index) {
    RowWrapper rowWrapper = getRowWrapperIfValid(index);
    if (rowWrapper == null) {
      return;
    }

    Object rowObj = rowWrapper.getRow();

    // Only rows can contain toggles.
    if (rowObj instanceof Row) {
      Row row = (Row) rowObj;

      // Does the row contain a toggle ? if so, switch its state.
      Toggle toggle = row.getToggle();
      if (toggle != null) {
        rowWrapper.switchToggleState();
        // Dispatch the checked change callback to the app.
        OnCheckedChangeDelegate delegate = toggle.getOnCheckedChangeDelegate();
        templateContext
            .getAppDispatcher()
            .dispatchCheckedChanged(delegate, rowWrapper.isToggleChecked());
      }
    }
  }

  private void update() {
    boolean isLoading = mIsLoading;
    ViewGroup largeImageContainer = mLargeImageContainer;
    if (isLoading) {
      mProgressContainer.setVisibility(VISIBLE);

      // Mark the content views as invisible so that the size of the container remains the
      // same while the progress bar is showing.
      mEmptyListTextView.setVisibility(INVISIBLE);
      mListView.setVisibility(INVISIBLE);

      if (largeImageContainer != null) {
        largeImageContainer.setVisibility(INVISIBLE);
      }

      return;
    }

    mProgressContainer.setVisibility(GONE);

    // If the list is empty, hide it and display a message instead.
    boolean isEmpty = mListAdapter.getItemCount() == 0;
    if (isEmpty) {
      mEmptyListTextView.setVisibility(VISIBLE);
      mListView.setVisibility(GONE);

      if (largeImageContainer != null) {
        largeImageContainer.setVisibility(GONE);
      }

      mEmptyListTextView.setFocusable(true);
    } else {
      mEmptyListTextView.setVisibility(GONE);
      mListView.setVisibility(VISIBLE);

      if (largeImageContainer != null) {
        largeImageContainer.setVisibility(mRowList.getImage() != null ? VISIBLE : GONE);
      }
    }
  }

  /** Publish any non-null {@link Place}s from the list of {@link RowWrapper}. */
  private void publishMetadata(
      TemplateContext templateContext,
      List<RowWrapper> rowWrappers,
      int startIndexInclusive,
      int endIndexExclusive) {
    if (templateContext == null) {
      L.e(LogTags.TEMPLATE, "TemplateContext is null");
      return;
    }

    // Return if the range is empty.
    if (startIndexInclusive < 0) {
      return;
    }

    if (endIndexExclusive > rowWrappers.size()) {
      L.e(LogTags.TEMPLATE, "Index out of bound: (%d > %d)", endIndexExclusive, rowWrappers.size());
      return;
    }

    ImmutableList.Builder<Place> builder = new ImmutableList.Builder<>();
    for (int index = startIndexInclusive; index < endIndexExclusive; index++) {
      RowWrapper rowWrapper = rowWrappers.get(index);
      Place place = rowWrapper.getMetadata().getPlace();
      if (place != null) {
        builder.add(place);
      }
    }

    ImmutableList<Place> places = builder.build();
    L.v(LogTags.TEMPLATE, "Updating %d visible places", places.size());
    requireNonNull(templateContext.getAppHostService(LocationMediator.class))
        .setCurrentPlaces(places);
  }

  @Nullable
  private RowWrapper getRowWrapperIfValid(int index) {
    // The user may click on a row that is transitioning out and the index here may be invalid
    // for the new rows being transitioned in. Ignore those cases.
    // Theoretically this means that we may trigger a click on a new row that was
    // not clicked on (e.g. if the user double-taps really fast on the previously row), but that
    // seems like a low-probability scenario in real HU so we are not doing extra checks here.
    List<RowWrapper> rowWrappers = mRowList.getRowWrappers();
    if (index >= rowWrappers.size()) {
      return null;
    }

    return rowWrappers.get(index);
  }
}
