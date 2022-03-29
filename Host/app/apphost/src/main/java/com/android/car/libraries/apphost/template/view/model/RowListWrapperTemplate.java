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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarText;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Template;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.distraction.constraints.RowListConstraints;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * A template that wraps {@link RowListWrapper}-based templates.
 *
 * <p>This template is used to to render full-screen homogeneous lists, or panes (which are also
 * built with lists).
 *
 * @see #wrap
 */
public class RowListWrapperTemplate implements Template {
  private final RowListWrapper mList;
  @Nullable private final CarText mTitle;
  @Nullable private final Action mHeaderAction;
  @Nullable private final ActionStrip mActionStrip;
  @Nullable private final List<Action> mActionList;
  private final ActionsConstraints mActionsconstraints;

  /** The original template being wrapped. */
  private final Template mTemplate;

  /** Returns the list used by the template. */
  public RowListWrapper getList() {
    return mList;
  }

  /** Returns the title of the template. */
  @Nullable
  public CarText getTitle() {
    return mTitle;
  }

  /**
   * Returns the {@link Action} to display in the template's header or {@code null} if one is not to
   * be displayed.
   */
  @Nullable
  public Action getHeaderAction() {
    return mHeaderAction;
  }

  /**
   * Returns the {@link ActionStrip} to display in the template or {@code null} if one is not to be
   * displayed.
   */
  @Nullable
  public ActionStrip getActionStrip() {
    return mActionStrip;
  }

  /**
   * Returns the list of {@link Action}s to display in the template or {@code null} if one is not to
   * be displayed.
   */
  @Nullable
  public List<Action> getActionList() {
    return mActionList;
  }

  /**
   * Returns the constraints for the actions in the template.
   *
   * @see ActionsConstraints
   */
  public ActionsConstraints getActionsConstraints() {
    return mActionsconstraints;
  }

  @NonNull
  @Override
  public String toString() {
    return "RowListWrapperTemplate(" + mTemplate + ")";
  }

  /**
   * Returns a {@link RowListWrapperTemplate} instance that wraps the given {@code template}.
   *
   * @throws IllegalArgumentException if the {@code template} is not of a type that can be wrapped
   */
  public static RowListWrapperTemplate wrap(Context context, Template template, boolean isRefresh) {
    if (template instanceof PaneTemplate) {
      PaneTemplate paneTemplate = (PaneTemplate) template;
      Pane pane = paneTemplate.getPane();
      return new RowListWrapperTemplate(
          template,
          RowListWrapper.wrap(context, pane)
              .setRowListConstraints(RowListConstraints.ROW_LIST_CONSTRAINTS_PANE)
              .setIsRefresh(isRefresh)
              .build(),
          paneTemplate.getTitle(),
          paneTemplate.getHeaderAction(),
          paneTemplate.getActionStrip(),
          paneTemplate.getPane().getActions(),
          ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE);
    } else if (template instanceof ListTemplate) {
      ListTemplate listTemplate = (ListTemplate) template;
      RowListWrapper.Builder listWrapperBuilder;
      if (listTemplate.isLoading()) {
        listWrapperBuilder =
            RowListWrapper.wrap(context, ImmutableList.of())
                .setIsLoading(true)
                .setIsRefresh(isRefresh);
      } else {
        ItemList singleList = listTemplate.getSingleList();
        listWrapperBuilder =
            singleList == null
                ? RowListWrapper.wrap(context, listTemplate.getSectionedLists())
                    .setIsRefresh(isRefresh)
                : RowListWrapper.wrap(context, singleList).setIsRefresh(isRefresh);
      }

      return new RowListWrapperTemplate(
          template,
          listWrapperBuilder
              .setRowListConstraints(RowListConstraints.ROW_LIST_CONSTRAINTS_FULL_LIST)
              .setRowFlags(RowWrapper.DEFAULT_UNIFORM_LIST_ROW_FLAGS)
              .build(),
          listTemplate.getTitle(),
          listTemplate.getHeaderAction(),
          listTemplate.getActionStrip(),
          /* actionList= */ null,
          ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE);
    } else {
      throw new IllegalArgumentException(
          "Unknown template class: " + template.getClass().getName());
    }
  }

  /** Returns the template wrapped by this instance of a {@link RowListWrapperTemplate}. */
  @VisibleForTesting
  public Template getTemplate() {
    return mTemplate;
  }

  private RowListWrapperTemplate(
      Template template,
      RowListWrapper list,
      @Nullable CarText title,
      @Nullable Action headerAction,
      @Nullable ActionStrip actionStrip,
      @Nullable List<Action> actionList,
      ActionsConstraints actionsConstraints) {
    mTemplate = template;
    mList = list;
    mTitle = title;
    mHeaderAction = headerAction;
    mActionStrip = actionStrip;
    mActionList = actionList;
    mActionsconstraints = actionsConstraints;
  }
}
