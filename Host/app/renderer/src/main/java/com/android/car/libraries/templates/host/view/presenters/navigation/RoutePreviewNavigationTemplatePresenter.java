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
package com.android.car.libraries.templates.host.view.presenters.navigation;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.android.car.libraries.apphost.distraction.constraints.RowListConstraints.ROW_LIST_CONSTRAINTS_ROUTE_PREVIEW;
import static com.android.car.libraries.apphost.template.view.model.RowListWrapper.LIST_FLAGS_SELECTABLE_FOCUS_SELECT_ROW;
import static com.android.car.libraries.apphost.template.view.model.RowListWrapper.LIST_FLAGS_SELECTABLE_HIGHLIGHT_ROW;
import static com.android.car.libraries.apphost.template.view.model.RowListWrapper.LIST_FLAGS_SELECTABLE_SCROLL_TO_ROW;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.OnClickDelegate;
import androidx.car.app.model.TemplateWrapper;
import androidx.car.app.navigation.model.PanModeDelegate;
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate;
import com.android.car.libraries.apphost.common.EventManager.EventType;
import com.android.car.libraries.apphost.common.StatusBarManager.StatusBarState;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.common.ThreadUtils;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.template.view.model.ActionStripWrapper;
import com.android.car.libraries.apphost.template.view.model.RowListWrapper;
import com.android.car.libraries.apphost.template.view.model.RowWrapper;
import com.android.car.libraries.apphost.view.AbstractSurfaceTemplatePresenter;
import com.android.car.libraries.apphost.view.TemplatePresenter;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.ActionStripView;
import com.android.car.libraries.templates.host.view.widgets.common.CardHeaderView;
import com.android.car.libraries.templates.host.view.widgets.common.ContentView;
import com.android.car.libraries.templates.host.view.widgets.common.PanOverlayView;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A {@link TemplatePresenter} that shows a {@link RoutePreviewNavigationTemplate}. */
public class RoutePreviewNavigationTemplatePresenter extends AbstractSurfaceTemplatePresenter {
  private final ViewGroup mRootView;
  private final ViewGroup mContentContainer;
  private final CardHeaderView mHeaderView;
  private final ContentView mContentView;
  private final ActionStripView mActionStripView;
  private final ActionStripView mMapActionStripView;
  private final PanOverlayView mPanOverlay;

  /** Creates a {@link RoutePreviewNavigationTemplatePresenter}. */
  public static RoutePreviewNavigationTemplatePresenter create(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    RoutePreviewNavigationTemplatePresenter presenter =
        new RoutePreviewNavigationTemplatePresenter(templateContext, templateWrapper);
    presenter.update();
    return presenter;
  }

  @Override
  public View getView() {
    return mRootView;
  }

  @Override
  public void onStart() {
    super.onStart();

    getTemplateContext()
        .getEventManager()
        .subscribeEvent(
            this,
            EventType.CONFIGURATION_CHANGED,
            () -> {
              RoutePreviewNavigationTemplate template =
                  (RoutePreviewNavigationTemplate) getTemplate();
              updateActionStrip(template.getActionStrip());
              updateMapActionStrip(template.getMapActionStrip());
            });
  }

  @Override
  public void onStop() {
    getTemplateContext().getEventManager().unsubscribeEvent(this, EventType.CONFIGURATION_CHANGED);

    super.onStop();
  }

  @Override
  public void onTemplateChanged() {
    update();
  }

  @Override
  public boolean isPanAndZoomEnabled() {
    return getTemplateContext().getCarHostConfig().isPoiRoutePreviewPanZoomEnabled();
  }

  @Override
  public void onPanModeChanged(boolean isInPanMode) {
    updateVisibility(isInPanMode);
    dispatchPanModeChange(isInPanMode);
  }

  @Override
  protected View getDefaultFocusedView() {
    if (mContentView.getVisibility() == VISIBLE) {
      return mContentView;
    }
    return super.getDefaultFocusedView();
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
    if (getPanZoomManager().handlePanEventsIfNeeded(keyCode)) {
      return true;
    }

    // Move between the card view and the action button view on left or right rotary nudge.
    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
      // If the focus is in the card view (back button or row list), request focus in the
      // action
      // strip.
      if (moveFocusIfPresent(
          ImmutableList.of(mContentContainer), ImmutableList.of(mActionStripView))) {
        return true;
      }
    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
      // Request focus on the content view so that the first row in the list will take focus.
      if (moveFocusIfPresent(ImmutableList.of(mActionStripView), ImmutableList.of(mContentView))) {
        return true;
      }
    }
    return super.onKeyUp(keyCode, keyEvent);
  }

  public void calculateAdditionalInset(Rect inset) {
    // The portrait inset is more favorable to portrait screens and is calculated as the following
    // bounding box:
    // * left: inset left
    // * right: inset right
    // * top: Max(inset top, actionStrip bottom, content view bottom)
    // * bottom: inset bottom
    Rect portraitScreenInset = new Rect(inset);

    // The landscape inset is more favorable to landscape screens it is calculated as the following
    // bounding box:
    // * left: Max(inset left, content view right)
    // * right: inset right
    // * top: Max(inset top, actionStrip bottom)
    // * bottom: inset bottom
    Rect landscapeScreenInset = new Rect(inset);

    if (mMapActionStripView.getVisibility() == VISIBLE) {
      landscapeScreenInset.right = min(landscapeScreenInset.right, mMapActionStripView.getLeft());
      portraitScreenInset.right = min(portraitScreenInset.right, mMapActionStripView.getLeft());
    }
    if (mActionStripView.getVisibility() == VISIBLE) {
      landscapeScreenInset.top = max(landscapeScreenInset.top, mActionStripView.getBottom());
      portraitScreenInset.top = max(portraitScreenInset.top, mActionStripView.getBottom());
    }
    if (mContentContainer.getVisibility() == View.VISIBLE) {
      landscapeScreenInset.left = max(landscapeScreenInset.left, mContentContainer.getRight());
      portraitScreenInset.top = max(portraitScreenInset.top, mContentContainer.getBottom());
    }
    int landscapeScreenArea = landscapeScreenInset.height() * landscapeScreenInset.width();
    int portraitScreenArea = portraitScreenInset.height() * portraitScreenInset.width();
    inset.set(
        landscapeScreenArea > portraitScreenArea ? landscapeScreenInset : portraitScreenInset);
  }

  private void update() {
    RoutePreviewNavigationTemplate template = (RoutePreviewNavigationTemplate) getTemplate();

    updateActionStrip(template.getActionStrip());
    updateMapActionStrip(template.getMapActionStrip());
    getPanZoomManager().setEnabled(hasPanButton());
    mHeaderView.setContent(getTemplateContext(), template.getTitle(), template.getHeaderAction());

    ItemList itemList = template.getItemList();
    Action navigateAction = template.getNavigateAction();

    RowListWrapper.Builder builder =
        RowListWrapper.wrap(getTemplateContext(), itemList)
            .setIsLoading(template.isLoading())
            .setRowFlags(RowWrapper.DEFAULT_UNIFORM_LIST_ROW_FLAGS)
            .setRowListConstraints(ROW_LIST_CONSTRAINTS_ROUTE_PREVIEW)
            .setIsRefresh(getTemplateWrapper().isRefresh())
            .setIsHalfList(true)

            // For the route preview list, don't use radio buttons but rather show the
            // selection
            // by changing the row background.
            .setListFlags(
                LIST_FLAGS_SELECTABLE_HIGHLIGHT_ROW
                    | LIST_FLAGS_SELECTABLE_FOCUS_SELECT_ROW
                    | LIST_FLAGS_SELECTABLE_SCROLL_TO_ROW);
    if (!template.isLoading() && navigateAction != null) {
      builder.setRowSelectedText(navigateAction.getTitle());
      OnClickDelegate onClickDelegate = navigateAction.getOnClickDelegate();
      if (onClickDelegate != null) {
        TemplateContext templateContext = getTemplateContext();
        builder.setOnRepeatedSelectionCallback(
            () -> templateContext.getAppDispatcher().dispatchClick(onClickDelegate));
      }
    }
    mContentView.setRowListContent(getTemplateContext(), builder.build());

    updateVisibility(getPanZoomManager().isInPanMode());

    getTemplateContext().getSurfaceInfoProvider().invalidateStableArea();
    requestVisibleAreaUpdate();
  }

  private void updateActionStrip(@Nullable ActionStrip actionStrip) {
    mActionStripView.setActionStrip(
        getTemplateContext(), actionStrip, ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE);
  }

  private void updateMapActionStrip(@Nullable ActionStrip actionStrip) {
    ActionStripWrapper actionStripWrapper = null;
    if (actionStrip != null) {
      actionStripWrapper =
          getPanZoomManager()
              .getMapActionStripWrapper(
                  /* templateContext= */ getTemplateContext(), /* actionStrip= */ actionStrip);
    }

    mMapActionStripView.setActionStrip(
        getTemplateContext(),
        actionStripWrapper,
        ActionsConstraints.ACTIONS_CONSTRAINTS_NAVIGATION_MAP,
        /* allowTwoLines= */ false);
  }

  @SuppressLint("InflateParams")
  @SuppressWarnings({"methodref.receiver.bound.invalid"})
  private RoutePreviewNavigationTemplatePresenter(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    super(templateContext, templateWrapper, StatusBarState.OVER_SURFACE);

    mRootView =
        (ViewGroup)
            LayoutInflater.from(templateContext)
                .inflate(R.layout.list_navigation_template_layout, null);
    mContentContainer = mRootView.findViewById(R.id.content_container);
    mHeaderView = mRootView.findViewById(R.id.header_view);
    mContentView = mRootView.findViewById(R.id.content_view);
    mActionStripView = mRootView.findViewById(R.id.action_strip);
    mMapActionStripView = mRootView.findViewById(R.id.map_action_strip);
    mPanOverlay = mRootView.findViewById(R.id.pan_overlay);

    mContentContainer.setVisibility(View.VISIBLE);
  }

  private boolean hasPanButton() {
    RoutePreviewNavigationTemplate template = (RoutePreviewNavigationTemplate) getTemplate();
    ActionStrip mapActionStrip = template.getMapActionStrip();
    return mapActionStrip != null && mapActionStrip.getFirstActionOfType(Action.TYPE_PAN) != null;
  }

  private void dispatchPanModeChange(boolean isInPanMode) {
    RoutePreviewNavigationTemplate template = (RoutePreviewNavigationTemplate) getTemplate();
    PanModeDelegate panModeDelegate = template.getPanModeDelegate();
    if (panModeDelegate != null) {
      getTemplateContext().getAppDispatcher().dispatchPanModeChanged(panModeDelegate, isInPanMode);
    }
  }

  private void updateVisibility(boolean isInPanMode) {
    ThreadUtils.runOnMain(
        () -> {
          if (isInPanMode) {
            mPanOverlay.setVisibility(VISIBLE);
            mContentContainer.setVisibility(GONE);
            mActionStripView.setVisibility(GONE);
          } else {
            mPanOverlay.setVisibility(GONE);
            mContentContainer.setVisibility(VISIBLE);
            mActionStripView.setVisibility(VISIBLE);
          }
        });
  }
}
