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

import static com.google.common.base.Strings.nullToEmpty;

import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.SearchCallbackDelegate;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.input.InputManager;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.toolbar.SearchMode;
import com.android.car.ui.toolbar.ToolbarController;

/** A view that displays the header for the search templates. */
public class SearchHeaderView extends AbstractHeaderView {
  private final CarEditTextWrapper mEditableSearchBar;
  private final EditText mSearchBar;

  private SearchHeaderView(
      TemplateContext templateContext,
      ToolbarController toolbarController,
      View rootView,
      @Nullable String initialSearchText,
      @Nullable SearchCallbackDelegate searchCallbackDelegate,
      boolean keyboardOpened) {
    super(templateContext, toolbarController);

    InputManager mInputManager = templateContext.getInputManager();
    mToolbarController.setSearchMode(SearchMode.SEARCH);
    mSearchBar = rootView.requireViewById(com.android.car.ui.R.id.car_ui_toolbar_search_bar);
    mEditableSearchBar = new CarEditTextWrapper(mSearchBar, mInputManager);

    toolbarController.setSearchQuery(nullToEmpty(initialSearchText));

    if (searchCallbackDelegate != null) {
      mToolbarController.registerOnSearchListener(
          query ->
              templateContext
                  .getAppDispatcher()
                  .dispatchSearchTextChanged(searchCallbackDelegate, query));

      toolbarController.registerOnSearchCompletedListener(
          () -> {
            String query = mSearchBar.getText().toString();
            templateContext
                .getAppDispatcher()
                .dispatchSearchSubmitted(searchCallbackDelegate, query);
          });
    }

    if (keyboardOpened) {
      mInputManager.startInput(mEditableSearchBar);
    }

    // TODO(b/179220417): Handle disabling search while driving
  }

  /** Returns the searchBar of the header */
  public EditText getSearchBar() {
    return mSearchBar;
  }

  /** Returns the {@link InputConnection} for the search bar. */
  public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
    return mEditableSearchBar.onCreateInputConnection(editorInfo);
  }

  /** Updates the optional button in the header. */
  @Override
  public void setAction(@Nullable Action action) {
    super.setAction(action);
  }

  /** Updates the {@link ActionStrip} associated with this toolbar */
  @Override
  public void setActionStrip(@Nullable ActionStrip actionStrip, ActionsConstraints constraints) {
    super.setActionStrip(actionStrip, constraints);
    boolean hasMenuItems = actionStrip != null && !actionStrip.getActions().isEmpty();
    mToolbarController.setShowMenuItemsWhileSearching(hasMenuItems);
  }

  /** Updates the search hint. */
  public void setHint(@Nullable String searchHint) {
    mToolbarController.setSearchHint(searchHint != null ? searchHint : "");
  }

  /** Installs a {@link HeaderView} around the given container view */
  @SuppressWarnings("nullness:argument") // InsetsChangedListener is nullable.
  public static SearchHeaderView install(
      TemplateContext templateContext,
      View container,
      View rootView,
      @Nullable String initialSearchText,
      @Nullable SearchCallbackDelegate searchCallbackDelegate,
      boolean keyboardOpened) {
    ToolbarController toolbarController = CarUi.installBaseLayoutAround(container, null, true);
    if (toolbarController == null) {
      throw new NullPointerException("Toolbar Controller could not be created.");
    }
    return new SearchHeaderView(
        templateContext,
        toolbarController,
        rootView,
        initialSearchText,
        searchCallbackDelegate,
        keyboardOpened);
  }
}
