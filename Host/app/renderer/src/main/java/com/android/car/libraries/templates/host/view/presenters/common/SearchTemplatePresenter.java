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
package com.android.car.libraries.templates.host.view.presenters.common;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
import static android.view.View.VISIBLE;
import static com.android.car.libraries.apphost.template.view.model.RowListWrapper.LIST_FLAGS_RENDER_TITLE_AS_SECONDARY;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import androidx.annotation.Nullable;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarText;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.TemplateWrapper;
import com.android.car.libraries.apphost.common.EventManager;
import com.android.car.libraries.apphost.common.EventManager.EventType;
import com.android.car.libraries.apphost.common.StatusBarManager.StatusBarState;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.distraction.constraints.RowListConstraints;
import com.android.car.libraries.apphost.input.CarEditable;
import com.android.car.libraries.apphost.input.CarEditableListener;
import com.android.car.libraries.apphost.input.InputManager;
import com.android.car.libraries.apphost.template.view.model.RowListWrapper;
import com.android.car.libraries.apphost.template.view.model.RowWrapper;
import com.android.car.libraries.apphost.view.AbstractTemplatePresenter;
import com.android.car.libraries.apphost.view.TemplatePresenter;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.ContentView;
import com.android.car.libraries.templates.host.view.widgets.common.SearchHeaderView;

/**
 * A {@link TemplatePresenter} presenter which controls the {@link InputManager} based on values in
 * the {@link SearchTemplate} model provided via {@link #update}.
 */
public class SearchTemplatePresenter extends AbstractTemplatePresenter implements CarEditable {

  private final InputManager mInputManager;
  private final ViewGroup mRootView;
  private final SearchHeaderView mHeaderView;
  private final ContentView mContentView;

  private String mSearchHint;
  private final String mDisabledSearchHint;

  private boolean mInputWasActiveOnLastWindowFocus;

  /** Creates a SearchTemplatePresenter */
  public static SearchTemplatePresenter create(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    SearchTemplatePresenter presenter =
        new SearchTemplatePresenter(templateContext, templateWrapper);
    presenter.update();

    return presenter;
  }

  @Override
  public void onStart() {
    super.onStart();
    EventManager eventManager = getTemplateContext().getEventManager();
    eventManager.subscribeEvent(
        this,
        EventType.WINDOW_FOCUS_CHANGED,
        () -> {
          if (hasWindowFocus()) {
            // If the input was active the last time the window was focused, it means
            // that the user just dismissed the car screen keyboard. In this case, focus
            // on the search result list.
            if (mInputWasActiveOnLastWindowFocus) {
              mContentView.requestFocus();
            }
          }
          mInputWasActiveOnLastWindowFocus = mInputManager.isInputActive();
        });
  }

  @Override
  public void onStop() {
    // TODO(b/182232738): Reenable keyboard listener
    // LocationManager locationManager = LocationManager.getInstance();
    // locationManager.removeKeyboardEnabledListener(driveStatusEventListener);
    getTemplateContext().getEventManager().unsubscribeEvent(this, EventType.WINDOW_FOCUS_CHANGED);
    super.onStop();
  }

  @Override
  public void onPause() {
    mInputManager.stopInput();
    super.onPause();
  }

  @Override
  public View getView() {
    return mRootView;
  }

  @Override
  public void onTemplateChanged() {
    update();
  }

  @Override
  protected View getDefaultFocusedView() {
    // Hide the cursor by clearing the edit text focus if input is not active
    if (!mInputManager.isInputActive()) {
      mHeaderView.getSearchBar().clearFocus();
    }
    if (mContentView.getVisibility() == VISIBLE) {
      return mContentView;
    }

    return super.getDefaultFocusedView();
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
    return mHeaderView.onCreateInputConnection(editorInfo);
  }

  @Override
  public void setCarEditableListener(CarEditableListener listener) {}

  @Override
  public void setInputEnabled(boolean enabled) {}

  private void update() {
    SearchTemplate searchTemplate = (SearchTemplate) getTemplate();
    ActionStrip actionStrip = searchTemplate.getActionStrip();
    TemplateContext templateContext = getTemplateContext();

    // Store the hint so we can set it again when the keyboard is enabled. Use a local variable
    // so only one call to getSearchHint and null checker doesn't complain.
    String tempSearchHint = searchTemplate.getSearchHint();
    mSearchHint =
        tempSearchHint == null
            ? templateContext.getString(templateContext.getHostResourceIds().getSearchHintText())
            : tempSearchHint;
    updateSearchHint(mHeaderView.getSearchBar().isEnabled());

    mHeaderView.setActionStrip(actionStrip, ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE);

    mHeaderView.setAction(searchTemplate.getHeaderAction());

    ItemList itemList = searchTemplate.getItemList();
    boolean isEmptyList = false;
    if (itemList != null && itemList.getItems().isEmpty()) {
      // If the list is empty, use the first row to display the no-items message.
      itemList = getItemListWithEmptyTextRow(itemList.getNoItemsMessage());
      isEmptyList = true;
    }

    RowListWrapper.Builder builder =
        RowListWrapper.wrap(templateContext, itemList)
            .setIsLoading(searchTemplate.isLoading())
            .setRowFlags(RowWrapper.DEFAULT_UNIFORM_LIST_ROW_FLAGS)
            .setRowListConstraints(RowListConstraints.ROW_LIST_CONSTRAINTS_SIMPLE)
            .setIsRefresh(getTemplateWrapper().isRefresh());
    if (isEmptyList) {
      builder.setListFlags(LIST_FLAGS_RENDER_TITLE_AS_SECONDARY);
    }

    mContentView.setRowListContent(templateContext, builder.build());
  }

  private void updateSearchHint(boolean searchEnabled) {
    mHeaderView.setHint(searchEnabled ? mSearchHint : mDisabledSearchHint);
  }

  /**
   * Returns an {@link ItemList} that has the no-item message sent as the text on the first row.
   *
   * <p>If the input no-item message is {@code null}, a default message will be added instead.
   */
  private ItemList getItemListWithEmptyTextRow(@Nullable CarText customNoItemMessage) {
    // Set the title to be empty because the no-item message should be rendered as secondary
    // text.
    String message =
        getTemplateContext()
            .getString(getTemplateContext().getHostResourceIds().getTemplateListNoItemsText());
    return new ItemList.Builder()
        .addItem(
            new Row.Builder()
                .setTitle(
                    customNoItemMessage == null ? message : customNoItemMessage.toCharSequence())
                .build())
        .build();
  }

  @SuppressLint("InflateParams")
  @SuppressWarnings({"argument.type.incompatible", "method.invocation.invalid"})
  private SearchTemplatePresenter(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    super(templateContext, templateWrapper, StatusBarState.GONE);

    mInputManager = templateContext.getInputManager();

    mRootView =
        (ViewGroup) LayoutInflater.from(templateContext).inflate(R.layout.search_layout, null);
    mContentView = mRootView.findViewById(R.id.content_view);
    mDisabledSearchHint =
        templateContext.getString(templateContext.getHostResourceIds().getSearchHintDisabledText());

    SearchTemplate searchTemplate = (SearchTemplate) templateWrapper.getTemplate();
    View contentContainer = mRootView.findViewById(R.id.content_container);
    mHeaderView =
        SearchHeaderView.install(
            templateContext,
            contentContainer,
            mRootView,
            searchTemplate.getInitialSearchText(),
            searchTemplate.getSearchCallbackDelegate(),
            searchTemplate.isShowKeyboardByDefault());
    int inputType =
        mHeaderView.getSearchBar().getInputType() | TYPE_CLASS_TEXT | TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    mHeaderView.getSearchBar().setInputType(inputType);
  }
}
