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

import android.graphics.drawable.Drawable;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.OnClickDelegate;
import com.android.car.libraries.apphost.common.CarAppError;
import com.android.car.libraries.apphost.common.CommonUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.template.view.model.ActionStripWrapper;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.libraries.apphost.view.common.ImageUtils;
import com.android.car.libraries.apphost.view.common.ImageViewParams;
import com.android.car.ui.toolbar.MenuItem;
import com.android.car.ui.toolbar.Toolbar;
import com.android.car.ui.toolbar.Toolbar.NavButtonMode;
import com.android.car.ui.toolbar.ToolbarController;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** A view that displays the header for the templates. */
public abstract class AbstractHeaderView {
  protected final TemplateContext mTemplateContext;
  protected final ToolbarController mToolbarController;

  // TODO(b/183853224): Replace with equivalent ToolbarController, once is available
  @SuppressWarnings("deprecation")
  private final Toolbar.OnBackListener mBackListener =
      new Toolbar.OnBackListener() {
        @Override
        public boolean onBack() {
          if (mTemplateContext != null) {
            mTemplateContext.getBackPressedHandler().onBackPressed();
          }
          return true;
        }
      };

  protected AbstractHeaderView(
      TemplateContext templateContext, ToolbarController toolbarController) {
    mTemplateContext = templateContext;
    mToolbarController = toolbarController;
  }

  @VisibleForTesting
  protected ToolbarController getToolbarController() {
    return mToolbarController;
  }

  /** Updates the header action */
  protected void setAction(@Nullable Action action) {
    if (action != null && action.getType() == Action.TYPE_BACK) {
      mToolbarController.registerOnBackListener(mBackListener);
      mToolbarController.setNavButtonMode(NavButtonMode.BACK);
    } else {
      mToolbarController.unregisterOnBackListener(mBackListener);
      mToolbarController.setNavButtonMode(NavButtonMode.DISABLED);
    }

    if (action != null && action.getType() == Action.TYPE_APP_ICON) {
      mToolbarController.setLogo(mTemplateContext.getCarAppPackageInfo().getRoundAppIcon());
    } else {
      mToolbarController.setLogo(0);
    }
  }

  /** Updates the [ActionStrip] associated with this toolbar */
  public void setActionStrip(@Nullable ActionStrip actionStrip, ActionsConstraints constraints) {
    validateActionStrip(actionStrip, constraints);
    if (actionStrip == null) {
      mToolbarController.setMenuItems(null);
    } else {
      List<MenuItem> menuItems = createMenuItems(actionStrip, mTemplateContext);
      mToolbarController.setMenuItems(menuItems);
    }
  }

  /** Adds a toggle to this toolbar */
  public void addToggle(@Nullable Drawable icon, @Nullable Consumer<Boolean> onClickListener) {
    List<MenuItem> menuItemList = new ArrayList<>(mToolbarController.getMenuItems());
    if (icon != null) {
      menuItemList.add(new MenuItem.Builder(mTemplateContext).setIcon(icon).build());
    }
    menuItemList.add(
        new MenuItem.Builder(mTemplateContext)
            .setCheckable()
            .setOnClickListener(
                item -> {
                  if (onClickListener != null) {
                    onClickListener.accept(item.isChecked());
                  }
                })
            .build());
    mToolbarController.setMenuItems(menuItemList);
  }

  /** Ensure the model satisfies the input constraints. */
  private void validateActionStrip(
      @Nullable ActionStrip actionStrip, ActionsConstraints constraints) {
    ActionStripWrapper actionStripWrapper =
        actionStrip == null ? null : new ActionStripWrapper.Builder(actionStrip).build();
    try {
      ActionStripUtils.validateRequiredTypes(actionStripWrapper, constraints);
    } catch (ActionStripUtils.ValidationException exception) {
      mTemplateContext
          .getErrorHandler()
          .showError(
              CarAppError.builder(mTemplateContext.getCarAppPackageInfo().getComponentName())
                  .setCause(exception)
                  .build());
    }
  }

  /** Converts an [ActionStrip] to a list of [MenuItem]s. */
  protected static List<MenuItem> createMenuItems(
      ActionStrip actionStrip, TemplateContext templateContext) {
    List<MenuItem> menuItems = new ArrayList<>();
    for (Object action : actionStrip.getActions()) {
      if (action instanceof Action) {
        MenuItem menuItem = createMenuItem((Action) action, templateContext);
        menuItems.add(menuItem);
      } else {
        Log.e(LogTags.TEMPLATE, "Action is not supported: " + action);
      }
    }
    return menuItems;
  }

  /** Converts an {@link Action} to a {@link MenuItem}. */
  private static MenuItem createMenuItem(Action action, TemplateContext templateContext) {
    CarIcon carIcon = action.getIcon();
    boolean isTinted = carIcon == null || carIcon.getType() != CarIcon.TYPE_APP_ICON;
    MenuItem.Builder menuItemBuilder =
        new MenuItem.Builder(templateContext)
            .setPrimary(true)
            .setEnabled(true)
            .setTinted(isTinted)
            .setShowIconAndTitle(true)
            .setTitle(CarTextUtils.toCharSequenceOrEmpty(templateContext, action.getTitle()));
    OnClickDelegate onClickDelegate = action.getOnClickDelegate();
    if (onClickDelegate != null) {
      menuItemBuilder.setOnClickListener(
          item -> CommonUtils.dispatchClick(templateContext, onClickDelegate));
    }

    MenuItem menuItem = menuItemBuilder.build();

    int menuItemIconSize =
        (int)
            templateContext
                .getResources()
                .getDimension(com.android.car.ui.R.dimen.car_ui_toolbar_menu_item_icon_size);
    carIcon = ImageUtils.getIconFromAction(action);
    if (carIcon != null) {
      ImageViewParams imageViewParams;
      CarColor tintColor = carIcon.getTint();
      if (tintColor != null && tintColor.getColor() != 0) {
        imageViewParams =
            ImageViewParams.builder()
                .setDefaultTint(tintColor.getColor())
                .setForceTinting(true)
                .build();
      } else {
        imageViewParams = ImageViewParams.DEFAULT;
      }

      ImageUtils.setImageTargetSrc(
          templateContext,
          carIcon,
          menuItem::setIcon,
          imageViewParams,
          menuItemIconSize,
          menuItemIconSize);
    }
    return menuItem;
  }
}
