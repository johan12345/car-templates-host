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

import static com.android.car.libraries.apphost.template.view.model.RowListWrapper.LIST_FLAGS_TEMPLATE_HAS_LARGE_IMAGE;
import static com.android.car.ui.utils.CarUiUtils.requireViewByRefId;
import static java.lang.Math.min;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Switch;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.ForegroundCarColorSpan;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints;
import com.android.car.libraries.apphost.distraction.constraints.RowConstraints;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.template.view.model.RowListWrapper;
import com.android.car.libraries.apphost.template.view.model.RowWrapper;
import com.android.car.libraries.apphost.template.view.model.SelectionGroup;
import com.android.car.libraries.apphost.view.common.ActionButtonListParams;
import com.android.car.libraries.apphost.view.common.CarTextParams;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.libraries.apphost.view.common.ImageUtils;
import com.android.car.libraries.apphost.view.common.ImageViewParams;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.RowHolder.RowListener;
import com.android.car.ui.CarUiText;
import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiContentListItem.IconType;
import com.android.car.ui.recyclerview.CarUiListItem;
import com.android.car.ui.recyclerview.CarUiListItemAdapter;
import com.android.car.ui.widget.CarUiTextView;
import java.util.ArrayList;
import java.util.List;

/** Adapter for {@link ContentView} to display {@link CarUiListItem}s. */
public class RowAdapter extends CarUiListItemAdapter {
  /**
   * Start id for non-Chassis items. This value should be higher than any view type in {@link
   * CarUiListItemAdapter}.
   */
  private static final int ROW_LIST_VIEW_TYPE_BASE = 1000;

  /**
   * Empty payload used with {@link #notifyItemChanged(int, Object)} to selectively disable item
   * change animations.
   */
  private static final Object EMPTY_ITEM_CHANGED_PAYLOAD = new Object();

  private static final int ROW_LIST_VIEW_TYPE_ACTION_BUTTON = ROW_LIST_VIEW_TYPE_BASE + 1;
  private static final int ROW_LIST_VIEW_TYPE_SECTION_HEADER = ROW_LIST_VIEW_TYPE_BASE + 2;
  private static final int ROW_LIST_VIEW_TYPE_ROW = ROW_LIST_VIEW_TYPE_BASE + 3;
  private static final CarColor SELECTED_TEXT_COLOR = CarColor.BLUE;
  private static final int TITLE_MAX_LINE_COUNT = 2;
  private static final int ONE_BODY_MAX_LINE_COUNT = 2;
  private static final int MULTI_BODY_MAX_LINE_COUNT = 1;
  private static final int MAX_IMAGES_PER_TEXT_LINE = 2;

  @ColorInt private final int mDefaultIconTint;
  @Nullable private final Drawable mPlaceholderDrawable;
  @Nullable private final Drawable mFullRowChevronDrawable;
  @Nullable private final Drawable mHalfRowChevronDrawable;
  @ColorInt private final int mRowBackgroundColor;

  private RowListener mRowListener;
  private List<RowHolder> mRowHolders;
  private TemplateContext mTemplateContext;
  private final MarkerFactory mMarkerFactory;
  private final boolean mUseCompactLayout;
  private final int mTitleTextSize;
  private final int mSecondaryTextSize;
  private final int mSectionHeaderTextSize;

  static RowAdapter create(
      Context context,
      List<CarUiListItem> items,
      MarkerFactory markerFactory,
      boolean useCompactLayout) {
    return new RowAdapter(context, items, markerFactory, useCompactLayout);
  }

  private RowAdapter(
      Context context,
      List<CarUiListItem> items,
      MarkerFactory markerFactory,
      boolean useCompactLayout) {
    super(items, useCompactLayout);

    mUseCompactLayout = useCompactLayout;

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateRowDefaultIconTint,
      R.attr.templateRowImagePlaceholder,
      R.attr.templateFullRowChevronIcon,
      R.attr.templateHalfRowChevronIcon,
      R.attr.templateRowBackgroundColor,
    };

    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    mDefaultIconTint = ta.getColor(0, 0);
    mPlaceholderDrawable = ta.getDrawable(1);
    mFullRowChevronDrawable = ta.getDrawable(2);
    mHalfRowChevronDrawable = ta.getDrawable(3);
    mRowBackgroundColor = ta.getColor(4, 0);
    ta.recycle();

    mTitleTextSize = getTextSizeFromAttribute(context, R.attr.templateRowTitleStyle);
    mSecondaryTextSize = getTextSizeFromAttribute(context, R.attr.templateRowSecondaryTextStyle);
    mSectionHeaderTextSize =
        getTextSizeFromAttribute(context, R.attr.templateRowSectionHeaderStyle);

    mMarkerFactory = markerFactory;
  }

  private static int getTextSizeFromAttribute(Context context, int attr) {
    TypedArray ta = context.obtainStyledAttributes(new int[] {attr});
    int titleTextStyleResourceId = ta.getResourceId(0, 0);
    ta.recycle();

    ta =
        context.obtainStyledAttributes(
            titleTextStyleResourceId, new int[] {android.R.attr.textAppearance});
    int titleTextAppearanceResourceId = ta.getResourceId(0, 0);
    ta.recycle();

    ta =
        context.obtainStyledAttributes(
            titleTextAppearanceResourceId, new int[] {android.R.attr.textSize});
    int result = ta.getDimensionPixelSize(androidx.appcompat.R.styleable.TextAppearance_android_textSize, 0);
    ta.recycle();

    return result;
  }

  public List<RowHolder> getRowHolders() {
    return mRowHolders;
  }

  /** Updates the rows of the adapter. */
  @SuppressWarnings("unchecked")
  public void setRows(
      TemplateContext templateContext, List<RowHolder> rowHolders, RowListener rowListener) {
    int previousItemCount = mRowHolders == null ? 0 : mRowHolders.size();
    mTemplateContext = templateContext;
    mRowHolders = rowHolders;
    mRowListener = rowListener;

    List<CarUiListItem> items = new ArrayList<>(rowHolders.size());
    for (int index = 0; index < rowHolders.size(); index++) {
      RowHolder holder = rowHolders.get(index);
      RowWrapper rowWrapper = holder.getRowWrapper();
      CarUiListItem item = createCarUiListItem(templateContext, rowWrapper, index);
      if (item == null) {
        Log.e(LogTags.TEMPLATE, "Cannot create item for the row " + rowWrapper);
        continue;
      }
      items.add(item);
    }

    getItems().clear();
    ((List<CarUiListItem>) getItems()).addAll(items);
    if (previousItemCount == items.size()) {
      notifyItemRangeChanged(0, items.size(), EMPTY_ITEM_CHANGED_PAYLOAD);
    } else {
      notifyDataSetChanged();
    }
  }

  /** Updates row at index {@code index} of the adapter. */
  @SuppressWarnings("unchecked")
  public void updateRow(int index) {
    if (index < 0 || index >= mRowHolders.size()) {
      Log.e(LogTags.TEMPLATE, "Index out of bound " + index);
      return;
    }
    RowWrapper rowWrapper = mRowHolders.get(index).getRowWrapper();
    CarUiListItem item = createCarUiListItem(mTemplateContext, rowWrapper, index);
    if (item == null) {
      Log.e(LogTags.TEMPLATE, "Cannot create item for the row " + rowWrapper);
      return;
    }
    ((List<CarUiListItem>) getItems()).set(index, item);
    // By passing a non-null payload to notifyItemChanged, we avoid ItemAnimator's onChange
    // animation. This is needed when updating list items because otherwise the ViewHolder's
    // contents flicker every time they are updated.
    notifyItemChanged(index, EMPTY_ITEM_CHANGED_PAYLOAD);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == ROW_LIST_VIEW_TYPE_ACTION_BUTTON) {
      return new ActionButtonListViewHolder(
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.action_button_list_row, parent, false));
    } else if (viewType == ROW_LIST_VIEW_TYPE_SECTION_HEADER) {
      return new SectionHeaderViewHolder(
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.row_section_header_view, parent, false));
    } else if (viewType == ROW_LIST_VIEW_TYPE_ROW) {
      if (mUseCompactLayout) {
        return new ListItemViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(
                    R.layout.half_list_row_view, /* root= */ parent, /* attachToRoot= */ false),
            mHalfRowChevronDrawable);
      } else {
        return new ListItemViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(
                    R.layout.full_list_row_view, /* root= */ parent, /* attachToRoot= */ false),
            mFullRowChevronDrawable);
      }
    } else {
      return super.onCreateViewHolder(parent, viewType);
    }
  }

  @Override
  public int getItemViewType(int position) {
    if (getItems().get(position) instanceof ActionButtonListItem) {
      return ROW_LIST_VIEW_TYPE_ACTION_BUTTON;
    } else if (getItems().get(position) instanceof SectionHeaderItem) {
      return ROW_LIST_VIEW_TYPE_SECTION_HEADER;
    } else if (getItems().get(position) instanceof CarUiContentListItem) {
      return ROW_LIST_VIEW_TYPE_ROW;
    } else {
      return super.getItemViewType(position);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    if (holder.getItemViewType() == ROW_LIST_VIEW_TYPE_ACTION_BUTTON) {
      CarUiListItem item = getItems().get(position);
      if (!(item instanceof ActionButtonListItem)) {
        Log.e(LogTags.TEMPLATE, "Incorrect item view type for item " + item);
        return;
      }
      if (!(holder instanceof ActionButtonListViewHolder)) {
        Log.e(LogTags.TEMPLATE, "Incorrect view holder type for list item.");
        return;
      }

      ((ActionButtonListViewHolder) holder).bind((ActionButtonListItem) item);
    } else if (holder.getItemViewType() == ROW_LIST_VIEW_TYPE_SECTION_HEADER) {
      CarUiListItem item = getItems().get(position);
      if (!(item instanceof SectionHeaderItem)) {
        Log.e(LogTags.TEMPLATE, "Incorrect item view type for item " + item);
        return;
      }
      if (!(holder instanceof SectionHeaderViewHolder)) {
        Log.e(LogTags.TEMPLATE, "Incorrect view holder type for section header item.");
        return;
      }

      ((SectionHeaderViewHolder) holder).bind((SectionHeaderItem) item);
    } else if (holder.getItemViewType() == ROW_LIST_VIEW_TYPE_ROW) {
      CarUiListItem item = getItems().get(position);
      if (!(item instanceof CarUiContentListItem)) {
        Log.e(LogTags.TEMPLATE, "Incorrect item view type for item " + item);
        return;
      }
      if (!(holder instanceof ListItemViewHolder)) {
        Log.e(LogTags.TEMPLATE, "Incorrect view holder type for row item " + item);
        return;
      }

      // TODO(b/205602000): investigate switching to ConstraintLayout for this instead of
      // calculating the margin in code.
      RowHolder rowHolder = getRowHolders().get(position);
      boolean hasTemplateImageBesidesRow =
          (rowHolder.getRowWrapper().getListFlags() & LIST_FLAGS_TEMPLATE_HAS_LARGE_IMAGE) != 0;
      ((ListItemViewHolder) holder).bind((CarUiContentListItem) item, hasTemplateImageBesidesRow);
    } else {
      super.onBindViewHolder(holder, position);
    }
  }

  /** Converts a {@link Row} to a {@link CarUiListItem}. */
  @Nullable
  public CarUiListItem createCarUiListItem(
      TemplateContext templateContext, RowWrapper rowWrapper, int index) {
    if (rowWrapper.getRow() instanceof Row) {
      return createRowCarUiListItem(templateContext, rowWrapper, index);
    } else if (ActionListUtils.isActionList(rowWrapper.getRow())) {
      return createActionsCarUiListItem(templateContext, rowWrapper);
    } else {
      Log.i(LogTags.TEMPLATE, "Unknown row type ${rowWrapper.row.javaClass.name}");
      return null;
    }
  }

  /** Converts the {@link Row} to a header */
  private SectionHeaderItem createCarUiHeaderListItem(TemplateContext templateContext, Row row) {
    CarTextParams params =
        new CarTextParams.Builder()
            .setMaxImages(MAX_IMAGES_PER_TEXT_LINE)
            .setImageBoundingBox(new Rect(0, 0, mSectionHeaderTextSize, mSectionHeaderTextSize))
            .build();
    return new SectionHeaderItem(
        CarTextUtils.toCharSequenceOrEmpty(templateContext, row.getTitle(), params));
  }

  @Nullable
  private CarUiListItem createActionsCarUiListItem(
      TemplateContext templateContext, RowWrapper rowWrapper) {
    List<Action> actions = ActionListUtils.getActionList(rowWrapper.getRow());
    int maxActions = rowWrapper.getRowConstraints().getMaxActionsExclusive();

    return new RowAdapter.ActionButtonListItem(
        templateContext, actions, maxActions, mRowBackgroundColor);
  }

  private CarUiListItem createRowCarUiListItem(
      TemplateContext templateContext, RowWrapper rowWrapper, int index) {
    Row row = (Row) rowWrapper.getRow();

    // If this row is a header, create a header item instead
    if ((rowWrapper.getRowFlags() & RowWrapper.ROW_FLAG_SECTION_HEADER) != 0) {
      return createCarUiHeaderListItem(templateContext, row);
    }

    CarUiContentListItem.Action action = createAction(rowWrapper);
    CarUiContentListItem item = new CarUiContentListItem(action);
    boolean colorContrastCheckPassed =
        checkColorContrast(templateContext, row, mRowBackgroundColor);
    updateItemText(
        item, templateContext, rowWrapper, index, /* allowColor= */ colorContrastCheckPassed);

    // Only update the item image if there is no place marker.
    if (!updateItemPlaceMarker(item, templateContext, rowWrapper)) {
      updateItemImage(
          item, templateContext, rowWrapper, index, /* allowTint= */ colorContrastCheckPassed);
    }
    updateCheckedState(item, rowWrapper, index);
    updateActivationState(item, rowWrapper, index);
    updateClickListener(item, rowWrapper, index);

    item.setOnCheckedChangeListener(
        (v, checked) -> {
          if (mRowListener != null) {
            mRowListener.onCheckedChange(index);
          }
        });

    return item;
  }

  /** Checks the color contrast between contents of the given row and the background color. */
  private static boolean checkColorContrast(
      TemplateContext templateContext, Row row, @ColorInt int backgroundColor) {
    // Only the secondary texts can be colored, so check them
    for (CarText carText : row.getTexts()) {
      if (!CarTextUtils.checkColorContrast(templateContext, carText, backgroundColor)) {
        return false;
      }
    }

    CarIcon image = row.getImage();
    if (image == null) {
      return true;
    }
    CarColor tint = image.getTint();
    if (tint == null) {
      return true;
    }

    return CarColorUtils.checkColorContrast(templateContext, tint, backgroundColor);
  }

  /**
   * Sets the click listener if the row is actionable. An actionable row is one that has a click
   * delegate, selection state, or toggle.
   */
  private void updateClickListener(CarUiContentListItem item, RowWrapper rowWrapper, int index) {
    Row row = (Row) rowWrapper.getRow();

    boolean isClickable =
        row.getOnClickDelegate() != null
            && rowWrapper.getRowConstraints().isOnClickListenerAllowed();
    boolean isSelectable = rowWrapper.getSelectionGroup() != null;
    boolean isToggle = row.getToggle() != null;
    if (isClickable || isSelectable || isToggle) {
      item.setOnItemClickedListener(
          (v) -> {
            if (mRowListener != null) {
              mRowListener.onRowClicked(index);
            }
          });
    } else {
      item.setOnItemClickedListener(null);
    }
  }

  /** Updates the text fields of the item using properties of the {@link RowWrapper}. */
  private void updateItemText(
      CarUiContentListItem item,
      TemplateContext templateContext,
      RowWrapper rowWrapper,
      int index,
      boolean allowColor) {
    Row row = (Row) rowWrapper.getRow();
    RowConstraints constraints = rowWrapper.getRowConstraints();
    int listFlags = rowWrapper.getListFlags();

    boolean renderTitleAsSecondaryText =
        (listFlags & RowListWrapper.LIST_FLAGS_RENDER_TITLE_AS_SECONDARY) != 0;

    // Create a copy because the row model returns unmodifiable list.
    List<CarText> texts = new ArrayList<>(row.getTexts());

    CarText title = row.getTitle();
    if (title != null) {
      CarTextParams titleParams =
          new CarTextParams.Builder()
              .setMaxImages(MAX_IMAGES_PER_TEXT_LINE)
              .setImageBoundingBox(new Rect(0, 0, mTitleTextSize, mTitleTextSize))
              .build();
      CharSequence titleString =
          CarTextUtils.toCharSequenceOrEmpty(templateContext, title, titleParams);
      if (titleString.length() > 0) {
        if (renderTitleAsSecondaryText) {
          texts.add(0, title);
        } else {
          item.setTitle(
              CarUiTextUtils.fromCarText(
                  templateContext, title, titleParams, TITLE_MAX_LINE_COUNT));
        }
      }
    }

    int lineCount = texts.size();
    int maxLineCount = min(constraints.getMaxTextLinesPerRow(), lineCount);

    if (maxLineCount < lineCount) {
      Log.d(
          LogTags.TEMPLATE,
          "Number of secondary text lines " + lineCount + " over limit of " + maxLineCount);
    }

    while (!texts.isEmpty() && texts.size() > maxLineCount) {
      texts.remove(texts.size() - 1);
    }

    // Add selected text to the body if available.
    CarText selectedText = createSelectedText(rowWrapper, index);
    if (selectedText != null) {
      texts.add(selectedText);
    }

    if (!texts.isEmpty()) {
      List<CarUiText> bodyTexts = createCarUiTextList(templateContext, texts, allowColor);
      item.setBody(bodyTexts);
    }
  }

  /**
   * Creates a {@link CarText} representing {@code selectedText} for the given {@link RowWrapper}.
   *
   * <p>Returns {@code null} if selected text is not available or the row is not selected.
   */
  @Nullable
  private CarText createSelectedText(RowWrapper rowWrapper, int index) {
    CarText selectedText = rowWrapper.getSelectedText();
    if (selectedText == null) {
      return null;
    }

    SelectionGroup selectionGroup = rowWrapper.getSelectionGroup();
    if (selectionGroup == null || !selectionGroup.isSelected(index)) {
      return null;
    }

    SpannableString spannableSelectedText = new SpannableString(selectedText.toCharSequence());
    int start = 0;
    int end = spannableSelectedText.length();
    spannableSelectedText.setSpan(
        ForegroundCarColorSpan.create(SELECTED_TEXT_COLOR),
        start,
        end,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    return new CarText.Builder(spannableSelectedText).build();
  }

  /**
   * Updates the place marker image of the item using properties of the {@link RowWrapper}.
   *
   * <p>Returns true iff a place marker was found.
   */
  private boolean updateItemPlaceMarker(
      CarUiContentListItem item, TemplateContext templateContext, RowWrapper rowWrapper) {
    Place place = rowWrapper.getMetadata().getPlace();
    if (place == null) {
      return false;
    }

    PlaceMarker marker = place.getMarker();
    if (marker == null) {
      return false;
    }

    Bitmap bitmap = mMarkerFactory.getContentForListMarker(templateContext, marker);
    if (bitmap == null) {
      return false;
    }

    item.setPrimaryIconType(IconType.STANDARD);
    item.setIcon(new BitmapDrawable(templateContext.getResources(), bitmap));
    return true;
  }

  /** Updates the image of the item using properties of the {@link RowWrapper}. */
  private void updateItemImage(
      CarUiContentListItem item,
      TemplateContext templateContext,
      RowWrapper rowWrapper,
      int index,
      boolean allowTint) {
    Row row = (Row) rowWrapper.getRow();
    CarIcon image = row.getImage();
    if (image == null) {
      return;
    }

    CarUiContentListItem.IconType iconType = convertImageTypeToIconType(row.getRowImageType());
    if (iconType == null) {
      Log.e(LogTags.TEMPLATE, "Unknown icon type for row " + row);
      return;
    }
    item.setPrimaryIconType(iconType);
    int iconSize = (int) getIconSize(iconType);

    ImageViewParams imageParams =
        ImageViewParams.builder()
            .setPlaceholderDrawable(mPlaceholderDrawable)
            .setDefaultTint(mDefaultIconTint)
            .setForceTinting(row.getRowImageType() == Row.IMAGE_TYPE_ICON)
            .setIgnoreAppTint(!allowTint)
            .setBackgroundColor(mRowBackgroundColor)
            .setCarIconConstraints(rowWrapper.getRowConstraints().getCarIconConstraints())
            .build();
    ImageUtils.setImageTargetSrc(
        templateContext,
        row.getImage(),
        drawable -> {
          item.setIcon(drawable);
          // By passing a non-null payload to notifyItemChanged, we avoid ItemAnimator's
          // onChange animation. This is needed when updating list items because otherwise
          // the ViewHolder's contents flicker every time they are updated.
          notifyItemChanged(index, EMPTY_ITEM_CHANGED_PAYLOAD);
        },
        imageParams,
        iconSize,
        iconSize);
  }

  /** Updates the checked state of the item. */
  private void updateCheckedState(CarUiContentListItem item, RowWrapper rowWrapper, int index) {
    SelectionGroup selectionGroup = rowWrapper.getSelectionGroup();
    boolean isSelected = selectionGroup != null && selectionGroup.getSelectedIndex() == index;
    boolean hasRadioButton = item.getAction() == CarUiContentListItem.Action.RADIO_BUTTON;

    RowConstraints constraints = rowWrapper.getRowConstraints();
    boolean isToggleAllowed = constraints.isToggleAllowed();
    boolean hasToggle = item.getAction() == CarUiContentListItem.Action.SWITCH;
    boolean isToggleChecked = rowWrapper.isToggleChecked();

    item.setChecked(
        (isSelected && hasRadioButton) || (isToggleAllowed && hasToggle && isToggleChecked));
  }

  /** Updates the activation state of the item. */
  private void updateActivationState(CarUiContentListItem item, RowWrapper rowWrapper, int index) {
    SelectionGroup selectionGroup = rowWrapper.getSelectionGroup();
    boolean isSelected = selectionGroup != null && selectionGroup.getSelectedIndex() == index;
    boolean useRadioButton = item.getAction() == CarUiContentListItem.Action.RADIO_BUTTON;
    item.setActivated(isSelected && !useRadioButton && shouldHighlightSelectedRow(rowWrapper));
  }

  @Nullable
  private static CarUiContentListItem.IconType convertImageTypeToIconType(int imageType) {
    switch (imageType) {
      case Row.IMAGE_TYPE_LARGE:
        return IconType.CONTENT;
      case Row.IMAGE_TYPE_SMALL:
      case Row.IMAGE_TYPE_ICON:
        return IconType.STANDARD;
      default:
        return null;
    }
  }

  private float getIconSize(CarUiContentListItem.IconType imageType) {
    Resources res = mTemplateContext.getResources();
    switch (imageType) {
      case CONTENT:
        return res.getDimension(com.android.car.ui.R.dimen.car_ui_list_item_content_icon_width);
      case STANDARD:
        return res.getDimension(com.android.car.ui.R.dimen.car_ui_list_item_icon_size);
      case AVATAR:
        return res.getDimension(com.android.car.ui.R.dimen.car_ui_list_item_avatar_icon_width);
    }
    Log.e(LogTags.TEMPLATE, "Unknown imageType: " + imageType);
    return res.getDimension(com.android.car.ui.R.dimen.car_ui_list_item_icon_size);
  }

  /** Returns proper {@link CarUiContentListItem.Action} for a given {@link RowWrapper}. */
  private CarUiContentListItem.Action createAction(RowWrapper rowWrapper) {
    if (!(rowWrapper.getRow() instanceof Row)) {
      return CarUiContentListItem.Action.NONE;
    }

    Row row = (Row) rowWrapper.getRow();
    if (row.isBrowsable()) {
      return CarUiContentListItem.Action.CHEVRON;
    } else if (row.getToggle() != null) {
      return CarUiContentListItem.Action.SWITCH;
    } else if (rowWrapper.getSelectionGroup() != null && shouldUseRadioButtons(rowWrapper)) {
      return CarUiContentListItem.Action.RADIO_BUTTON;
    } else {
      return CarUiContentListItem.Action.NONE;
    }
  }

  /**
   * Returns true if the flag for using radio buttons is enabled for the provided {@link
   * RowWrapper}.
   */
  private boolean shouldUseRadioButtons(RowWrapper rowWrapper) {
    return (rowWrapper.getListFlags() & RowListWrapper.LIST_FLAGS_SELECTABLE_USE_RADIO_BUTTONS)
        != 0;
  }

  /** Returns true if the flag for highlighting the currently selected row is enabled */
  private boolean shouldHighlightSelectedRow(RowWrapper rowWrapper) {
    return (rowWrapper.getListFlags() & RowListWrapper.LIST_FLAGS_SELECTABLE_HIGHLIGHT_ROW) != 0;
  }

  /** Creates a list of {@link CarUiText} one for each given {@link CarText}. */
  private List<CarUiText> createCarUiTextList(
      TemplateContext templateContext, List<CarText> carTexts, boolean allowColor) {
    CarTextParams textParams =
        CarTextParams.builder()
            .setColorSpanConstraints(
                allowColor ? CarColorConstraints.STANDARD_ONLY : CarColorConstraints.NO_COLOR)
            .setMaxImages(MAX_IMAGES_PER_TEXT_LINE)
            .setImageBoundingBox(new Rect(0, 0, mSecondaryTextSize, mSecondaryTextSize))
            .setBackgroundColor(mRowBackgroundColor)
            .build();
    List<CarUiText> lines = new ArrayList<>();
    int maxLineCount = carTexts.size() > 1 ? MULTI_BODY_MAX_LINE_COUNT : ONE_BODY_MAX_LINE_COUNT;
    for (CarText carText : carTexts) {
      lines.add(CarUiTextUtils.fromCarText(templateContext, carText, textParams, maxLineCount));
    }
    return lines;
  }

  /** The {@link ViewHolder} for {@link ActionButtonListItem}. */
  static class ActionButtonListViewHolder extends RecyclerView.ViewHolder {
    private final ActionButtonListView mActionButtonListView;

    ActionButtonListViewHolder(View view) {
      super(view);
      mActionButtonListView = requireViewByRefId(view, R.id.action_button_list_view);
    }

    void bind(ActionButtonListItem item) {
      mActionButtonListView.setActionList(
          item.getTemplateContext(),
          item.getActions(),
          ActionButtonListParams.builder()
              .setMaxActions(item.getMaxActions())
              .setOemReorderingAllowed(true)
              .setOemColorOverrideAllowed(true)
              .setSurroundingColor(item.getSurroundingColor())
              .build());
    }
  }

  /** The {@link ViewHolder} for {@link SectionHeaderItem}. */
  static class SectionHeaderViewHolder extends RecyclerView.ViewHolder {
    SectionHeaderViewHolder(View view) {
      super(view);
    }

    void bind(SectionHeaderItem item) {
      CarUiTextView sectionHeaderView = (CarUiTextView) itemView;
      sectionHeaderView.setText(item.getText());
    }
  }

  /** View model for an {@link ActionButtonListView}. */
  public static class ActionButtonListItem extends CarUiListItem {
    private final TemplateContext mTemplateContext;
    private final List<Action> mActionList;
    private final int mMaxActions;
    @ColorInt private final int mSurroundingColor;

    ActionButtonListItem(
        TemplateContext templateContext,
        List<Action> actionList,
        int maxActions,
        @ColorInt int surroundingColor) {
      mActionList = actionList;
      mTemplateContext = templateContext;
      mMaxActions = maxActions;
      mSurroundingColor = surroundingColor;
    }

    /** Returns a list of {@link Action}s */
    List<Action> getActions() {
      return mActionList;
    }

    /** Returns the associated {@link TemplateContext} */
    TemplateContext getTemplateContext() {
      return mTemplateContext;
    }

    /** Returns the maximum number of actions allowed. */
    int getMaxActions() {
      return mMaxActions;
    }

    /** Returns the color of the surrounding region around the action button list. */
    @ColorInt
    int getSurroundingColor() {
      return mSurroundingColor;
    }
  }

  /** View model for a section header. */
  public static class SectionHeaderItem extends CarUiListItem {
    private final CharSequence mText;

    SectionHeaderItem(CharSequence text) {
      mText = text;
    }

    CharSequence getText() {
      return mText;
    }
  }

  /** Holds views of {@link CarUiContentListItem}. */
  static class ListItemViewHolder extends RecyclerView.ViewHolder {

    final CarUiTextView mTitle;
    final CarUiTextView mBody;
    final ImageView mIcon;
    final ImageView mContentIcon;
    final ImageView mAvatarIcon;
    final ViewGroup mIconContainer;
    final ViewGroup mActionContainer;
    final Switch mSwitch;
    final CheckBox mCheckBox;
    final RadioButton mRadioButton;
    final ImageView mSupplementalIcon;
    final View mTouchInterceptor;
    final View mReducedTouchInterceptor;
    final View mActionContainerTouchInterceptor;
    @Nullable final Drawable mChevronDrawable;
    @Nullable final View mLargeImageSpacer;

    ListItemViewHolder(@NonNull View itemView, @Nullable Drawable chevronDrawable) {
      super(itemView);
      mTitle = requireViewByRefId(itemView, R.id.car_ui_list_item_title);
      mBody = requireViewByRefId(itemView, R.id.car_ui_list_item_body);
      mIcon = requireViewByRefId(itemView, R.id.car_ui_list_item_icon);
      mContentIcon = requireViewByRefId(itemView, R.id.car_ui_list_item_content_icon);
      mAvatarIcon = requireViewByRefId(itemView, R.id.car_ui_list_item_avatar_icon);
      mIconContainer = requireViewByRefId(itemView, R.id.car_ui_list_item_icon_container);
      mActionContainer = requireViewByRefId(itemView, R.id.car_ui_list_item_action_container);
      mSwitch = requireViewByRefId(itemView, R.id.car_ui_list_item_switch_widget);
      mCheckBox = requireViewByRefId(itemView, R.id.car_ui_list_item_checkbox_widget);
      mRadioButton = requireViewByRefId(itemView, R.id.car_ui_list_item_radio_button_widget);
      mSupplementalIcon = requireViewByRefId(itemView, R.id.car_ui_list_item_supplemental_icon);
      mReducedTouchInterceptor =
          requireViewByRefId(itemView, R.id.car_ui_list_item_reduced_touch_interceptor);
      mTouchInterceptor = requireViewByRefId(itemView, R.id.car_ui_list_item_touch_interceptor);
      mActionContainerTouchInterceptor =
          requireViewByRefId(itemView, R.id.car_ui_list_item_action_container_touch_interceptor);
      mChevronDrawable = chevronDrawable;
      mLargeImageSpacer = itemView.findViewById(R.id.large_image_spacer);
    }

    void bind(@NonNull CarUiContentListItem item, boolean hasTemplateImageBesidesRow) {
      CarUiText title = item.getTitle();
      if (title != null) {
        mTitle.setText(title);
        mTitle.setVisibility(View.VISIBLE);
      } else {
        mTitle.setVisibility(View.GONE);
      }

      List<CarUiText> body = item.getBody();
      if (body != null) {
        mBody.setText(body);
        mBody.setVisibility(View.VISIBLE);
      } else {
        mBody.setVisibility(View.GONE);
      }

      mIcon.setVisibility(View.GONE);
      mContentIcon.setVisibility(View.GONE);
      mAvatarIcon.setVisibility(View.GONE);

      Drawable icon = item.getIcon();
      if (icon != null) {
        mIconContainer.setVisibility(View.VISIBLE);

        switch (item.getPrimaryIconType()) {
          case CONTENT:
            mContentIcon.setVisibility(View.VISIBLE);
            mContentIcon.setImageDrawable(icon);
            break;
          case STANDARD:
            mIcon.setVisibility(View.VISIBLE);
            mIcon.setImageDrawable(icon);
            break;
          case AVATAR:
            mAvatarIcon.setVisibility(View.VISIBLE);
            mAvatarIcon.setImageDrawable(icon);
            mAvatarIcon.setClipToOutline(true);
            break;
        }
      } else {
        mIconContainer.setVisibility(View.GONE);
      }

      mSwitch.setVisibility(View.GONE);
      mCheckBox.setVisibility(View.GONE);
      mRadioButton.setVisibility(View.GONE);
      mSupplementalIcon.setVisibility(View.GONE);

      CarUiContentListItem.OnClickListener itemOnClickListener = item.getOnClickListener();

      switch (item.getAction()) {
        case NONE:
          mActionContainer.setVisibility(View.GONE);

          // Display ripple effects across entire item when clicked by using full-sized
          // touch interceptor.
          mTouchInterceptor.setVisibility(View.VISIBLE);
          mTouchInterceptor.setOnClickListener(
              v -> {
                if (itemOnClickListener != null) {
                  itemOnClickListener.onClick(item);
                }
              });
          mTouchInterceptor.setClickable(itemOnClickListener != null);
          mReducedTouchInterceptor.setVisibility(View.GONE);
          mActionContainerTouchInterceptor.setVisibility(View.GONE);
          break;
        case SWITCH:
          bindCompoundButton(item, mSwitch, itemOnClickListener);
          break;
        case CHECK_BOX:
          bindCompoundButton(item, mCheckBox, itemOnClickListener);
          break;
        case RADIO_BUTTON:
          bindCompoundButton(item, mRadioButton, itemOnClickListener);
          break;
        case CHEVRON:
          mSupplementalIcon.setVisibility(View.VISIBLE);
          mSupplementalIcon.setImageDrawable(mChevronDrawable);
          mActionContainer.setVisibility(View.VISIBLE);
          mTouchInterceptor.setVisibility(View.VISIBLE);
          mTouchInterceptor.setOnClickListener(
              v -> {
                if (itemOnClickListener != null) {
                  itemOnClickListener.onClick(item);
                }
              });
          mTouchInterceptor.setClickable(itemOnClickListener != null);
          mReducedTouchInterceptor.setVisibility(View.GONE);
          mActionContainerTouchInterceptor.setVisibility(View.GONE);
          break;
        case ICON:
          mSupplementalIcon.setVisibility(View.VISIBLE);
          mSupplementalIcon.setImageDrawable(item.getSupplementalIcon());

          mActionContainer.setVisibility(View.VISIBLE);

          // If the icon has a click listener, use a reduced touch interceptor to create
          // two distinct touch area; the action container and the remainder of the list
          // item. Each touch area will have its own ripple effect. If the icon has no
          // click listener, it shouldn't be clickable.
          if (item.getSupplementalIconOnClickListener() == null) {
            mTouchInterceptor.setVisibility(View.VISIBLE);
            mTouchInterceptor.setOnClickListener(
                v -> {
                  if (itemOnClickListener != null) {
                    itemOnClickListener.onClick(item);
                  }
                });
            mTouchInterceptor.setClickable(itemOnClickListener != null);
            mReducedTouchInterceptor.setVisibility(View.GONE);
            mActionContainerTouchInterceptor.setVisibility(View.GONE);
          } else {
            mReducedTouchInterceptor.setVisibility(View.VISIBLE);
            mReducedTouchInterceptor.setOnClickListener(
                v -> {
                  if (itemOnClickListener != null) {
                    itemOnClickListener.onClick(item);
                  }
                });
            mReducedTouchInterceptor.setClickable(itemOnClickListener != null);
            mActionContainerTouchInterceptor.setVisibility(View.VISIBLE);
            mActionContainerTouchInterceptor.setOnClickListener(
                (container) -> {
                  CarUiContentListItem.OnClickListener listener =
                      item.getSupplementalIconOnClickListener();
                  if (listener != null) {
                    listener.onClick(item);
                  }
                });
            mActionContainerTouchInterceptor.setClickable(
                item.getSupplementalIconOnClickListener() != null);
            mTouchInterceptor.setVisibility(View.GONE);
          }
          break;
      }

      // Sets the right margin for the row to account for the space needed for the large image.
      View spacer = mLargeImageSpacer;
      if (spacer != null) {
        spacer.setVisibility(hasTemplateImageBesidesRow ? View.VISIBLE : View.GONE);
      }

      itemView.setActivated(item.isActivated());
      setEnabled(itemView, item.isEnabled());
    }

    void setEnabled(View view, boolean enabled) {
      view.setEnabled(enabled);
      if (view instanceof ViewGroup) {
        ViewGroup group = (ViewGroup) view;

        for (int i = 0; i < group.getChildCount(); i++) {
          setEnabled(group.getChildAt(i), enabled);
        }
      }
    }

    void bindCompoundButton(
        @NonNull CarUiContentListItem item,
        @NonNull CompoundButton compoundButton,
        @Nullable CarUiContentListItem.OnClickListener itemOnClickListener) {
      compoundButton.setVisibility(View.VISIBLE);
      compoundButton.setOnCheckedChangeListener(null);
      compoundButton.setChecked(item.isChecked());
      compoundButton.setOnCheckedChangeListener(
          (buttonView, isChecked) -> item.setChecked(isChecked));

      // Clicks anywhere on the item should toggle the checkbox state. Use full touch
      // interceptor.
      mTouchInterceptor.setVisibility(View.VISIBLE);
      mTouchInterceptor.setOnClickListener(
          v -> {
            compoundButton.toggle();
            if (itemOnClickListener != null) {
              itemOnClickListener.onClick(item);
            }
          });
      // Compound button list items should always be clickable
      mTouchInterceptor.setClickable(true);
      mReducedTouchInterceptor.setVisibility(View.GONE);
      mActionContainerTouchInterceptor.setVisibility(View.GONE);

      mActionContainer.setVisibility(View.VISIBLE);
      mActionContainer.setClickable(false);
    }
  }
}
