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
package com.android.car.libraries.templates.host.view.presenters.maps;

import static android.view.View.VISIBLE;
import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.TemplateWrapper;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import com.android.car.libraries.apphost.common.EventManager;
import com.android.car.libraries.apphost.common.EventManager.EventType;
import com.android.car.libraries.apphost.common.LocationMediator;
import com.android.car.libraries.apphost.common.StatusBarManager.StatusBarState;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.distraction.constraints.RowListConstraints;
import com.android.car.libraries.apphost.template.view.model.RowListWrapper;
import com.android.car.libraries.apphost.template.view.model.RowWrapper;
import com.android.car.libraries.apphost.view.AbstractTemplatePresenter;
import com.android.car.libraries.apphost.view.TemplatePresenter;
import com.android.car.libraries.apphost.view.widget.map.AbstractMapViewContainer;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.di.MapViewContainerFactory;
import com.android.car.libraries.templates.host.view.widgets.common.ActionStripView;
import com.android.car.libraries.templates.host.view.widgets.common.CardHeaderView;
import com.android.car.libraries.templates.host.view.widgets.common.ContentView;
import com.google.common.collect.ImmutableList;

/** A {@link TemplatePresenter} that shows a map view with pins for locations. */
public class PlaceListMapTemplatePresenter extends AbstractTemplatePresenter {
  private final ViewGroup mRootView;
  private final ViewGroup mCardContainer;
  private final ActionStripView mActionStripView;
  private final CardHeaderView mHeaderView;
  private final ContentView mContentView;
  // This is lazy-initiated during every onStart instead of just in the ctor. For some reason the
  // map view is not laid out when the user exits the app and comes back to the template,
  // preventing the map from updating from place changes. Therefore as a workaround we just
  // re-create the map evertime the user comes back. See b/178606261 for more details.
  @Nullable private AbstractMapViewContainer mMapContainer;
  private final OnGlobalLayoutListener mGlobalLayoutListener;
  private final MapViewContainerFactory mMapViewContainerFactory;

  /** Creates a {@link PlaceListMapTemplatePresenter}. */
  public static PlaceListMapTemplatePresenter create(
      TemplateContext templateContext,
      TemplateWrapper templateWrapper,
      MapViewContainerFactory mapViewContainerFactory) {
    PlaceListMapTemplatePresenter presenter =
        new PlaceListMapTemplatePresenter(
            templateContext, templateWrapper, mapViewContainerFactory);
    presenter.update();
    return presenter;
  }

  @VisibleForTesting
  @Nullable
  public AbstractMapViewContainer getMapContainer() {
    return mMapContainer;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    updateMapContainerLifeCycle(State.CREATED);
  }

  @Override
  public void onDestroy() {
    updateMapContainerLifeCycle(State.DESTROYED);
    super.onDestroy();
  }

  @Override
  public void onStart() {
    super.onStart();
    updateMapContainerLifeCycle(State.STARTED);

    EventManager eventManager = getTemplateContext().getEventManager();
    eventManager.subscribeEvent(this, EventType.CONFIGURATION_CHANGED, this::refreshViews);
    eventManager.subscribeEvent(this, EventType.PLACE_LIST, this::updatePlaces);

    // Instantiating the map views during onStart as otherwise the map may not get laid out
    // properly. See b/178606261 for more details.
    refreshViews();
  }

  @Override
  public void onStop() {
    updateMapContainerLifeCycle(State.CREATED);
    TemplateContext templateContext = getTemplateContext();

    // Clear the list of places when transitioning out of this presenter.
    // This prevents a flow when the app enters this presenter again, it will temporarily show
    // the previous markers that were set.
    requireNonNull(templateContext.getAppHostService(LocationMediator.class))
        .setCurrentPlaces(ImmutableList.of());

    templateContext.getEventManager().unsubscribeEvent(this, EventType.CONFIGURATION_CHANGED);
    templateContext.getEventManager().unsubscribeEvent(this, EventType.PLACE_LIST);

    super.onStop();
  }

  @Override
  public void onPause() {
    getView().getViewTreeObserver().removeOnGlobalLayoutListener(mGlobalLayoutListener);
    updateMapContainerLifeCycle(State.STARTED);
    super.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
    getView().getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
    updateMapContainerLifeCycle(State.RESUMED);
  }

  @Override
  public View getView() {
    return mRootView;
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
    // Move between the card view and the action button view on left or right rotary nudge.
    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
      // If the focus is in the card view (back button or row list), request focus in the
      // action strip.
      if (moveFocusIfPresent(
          ImmutableList.of(mCardContainer), ImmutableList.of(mActionStripView))) {
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

  @Override
  public void onTemplateChanged() {
    update();
  }

  @Override
  public boolean handlesTemplateChangeAnimation() {
    // PlaceListMapTemplate has special behavior since we don't want to destroy the MapView for
    // a refresh, and it handles the update motion correctly internally.
    return true;
  }

  @Override
  public boolean isFullScreen() {
    return false;
  }

  /** Updates the locations in the map */
  private void update() {
    PlaceListMapTemplate mapTemplate = (PlaceListMapTemplate) getTemplate();
    TemplateContext templateContext = getTemplateContext();

    if (mapTemplate.isLoading()) {
      // Clear the last list of places if we are in loading state so that we are not showing
      // stale markers that do not correspond to the current list.
      requireNonNull(templateContext.getAppHostService(LocationMediator.class))
          .setCurrentPlaces(ImmutableList.of());
    }

    TransitionManager.beginDelayedTransition(
        mRootView,
        TransitionInflater.from(templateContext)
            .inflateTransition(R.transition.map_template_transition));

    mHeaderView.setContent(
        templateContext,
        mapTemplate.getTitle(),
        mapTemplate.getHeaderAction(),
        mapTemplate.getOnContentRefreshDelegate());

    ItemList itemList = mapTemplate.getItemList();
    RowListWrapper rowListWrapper =
        RowListWrapper.wrap(templateContext, itemList)
            .setIsLoading(mapTemplate.isLoading())
            .setRowFlags(RowWrapper.DEFAULT_UNIFORM_LIST_ROW_FLAGS)
            .setRowListConstraints(RowListConstraints.ROW_LIST_CONSTRAINTS_SIMPLE)
            .setIsRefresh(getTemplateWrapper().isRefresh())
            .setIsHalfList(true)
            .build();
    mContentView.setRowListContent(templateContext, rowListWrapper);

    updateMapSettings(mapTemplate);
    updateActionStrip(mapTemplate.getActionStrip());
  }

  // TODO(b/159908673): add tests for the lifecycle management logic in here.
  private void refreshViews() {
    // Destroy the previous MapView based on this presenter's currently lifecycle events.
    AbstractMapViewContainer previousMapContainer = mMapContainer;
    if (previousMapContainer != null) {
      previousMapContainer.getLifecycleRegistry().setCurrentState(State.DESTROYED);
      mRootView.removeView(previousMapContainer);
    }

    Lifecycle lifecycle = getLifecycle();
    if (lifecycle.getCurrentState() == State.DESTROYED) {
      // View already destroyed. Don't bother refreshing the views.
      return;
    }

    mMapContainer = mMapViewContainerFactory.create(getTemplateContext(), R.style.Theme_Template);

    if (mMapContainer != null) {
      mMapContainer.setTemplateContext(getTemplateContext());
      mMapContainer.setId(R.id.map_container);
      mRootView.addView(mMapContainer, 0);

      // Update the new MapView's lifecycle events to match this presenter's, as that is
      // required for the map instance to be initiated and shown.
      mMapContainer.getLifecycleRegistry().setCurrentState(lifecycle.getCurrentState());
    }

    PlaceListMapTemplate mapTemplate = (PlaceListMapTemplate) getTemplate();
    updateMapSettings(mapTemplate);
    updateActionStrip(mapTemplate.getActionStrip());
  }

  private void updateMapSettings(PlaceListMapTemplate mapTemplate) {
    AbstractMapViewContainer container = mMapContainer;
    if (container != null) {
      container.setCurrentLocationEnabled(mapTemplate.isCurrentLocationEnabled());
      container.setAnchor(mapTemplate.getAnchor());
      updatePlaces();
    }
  }

  private void updatePlaces() {
    AbstractMapViewContainer container = mMapContainer;
    if (container != null) {
      container.setPlaces(
          requireNonNull(getTemplateContext().getAppHostService(LocationMediator.class))
              .getCurrentPlaces());
    }
  }

  private void updateActionStrip(@Nullable ActionStrip actionStrip) {
    mActionStripView.setActionStrip(
        getTemplateContext(), actionStrip, ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE);
  }

  private void updateMapContainerLifeCycle(State state) {
    AbstractMapViewContainer container = mMapContainer;
    // TODO(b/180162594): Use ifNotNull when available.
    if (container != null) {
      container.getLifecycleRegistry().setCurrentState(state);
    }
  }

  @SuppressLint("InflateParams")
  @SuppressWarnings({"methodref.receiver.bound.invalid", "nullness"})
  private PlaceListMapTemplatePresenter(
      TemplateContext templateContext,
      TemplateWrapper templateWrapper,
      MapViewContainerFactory mapViewContainerFactory) {
    super(templateContext, templateWrapper, StatusBarState.OVER_SURFACE);

    mRootView =
        (ViewGroup)
            LayoutInflater.from(templateContext).inflate(R.layout.map_template_layout, null);
    mCardContainer = mRootView.findViewById(R.id.card_container);
    mHeaderView = mRootView.findViewById(R.id.header_view);
    mContentView = mRootView.findViewById(R.id.content_view);
    mActionStripView = mRootView.findViewById(R.id.action_strip);
    mMapViewContainerFactory = mapViewContainerFactory;
    // Note that the map container is instantiated during onStart.

    // We should always show an ItemList.
    mCardContainer.setVisibility(View.VISIBLE);

    // Dynamically update the visible area inset. This allows the MapViewContainer to account
    // for the insets when adjusting zoom levels to show all the place markers.
    mGlobalLayoutListener =
        () -> {
          Rect safeAreaInset = new Rect();
          // The content container is always visible so just use its right.
          safeAreaInset.left = mCardContainer.getRight();
          safeAreaInset.top =
              mActionStripView.getVisibility() == VISIBLE
                  ? mActionStripView.getBottom()
                  : mRootView.getTop() + mRootView.getPaddingTop();
          safeAreaInset.bottom = mRootView.getBottom() - mRootView.getPaddingBottom();
          safeAreaInset.right = mRootView.getRight() - mRootView.getPaddingRight();
          templateContext.getSurfaceInfoProvider().setVisibleArea(safeAreaInset);
        };
  }
}
