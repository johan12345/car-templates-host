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
import static com.android.car.libraries.templates.host.view.widgets.common.ActionStripView.ACTIONSTRIP_ACTIVE_STATE_DURATION_MILLIS;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.annotation.StyleableRes;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarText;
import androidx.car.app.model.TemplateWrapper;
import androidx.car.app.navigation.model.MessageInfo;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.NavigationTemplate.NavigationInfo;
import androidx.car.app.navigation.model.PanModeDelegate;
import androidx.car.app.navigation.model.RoutingInfo;
import androidx.car.app.navigation.model.TravelEstimate;
import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.apphost.common.EventManager;
import com.android.car.libraries.apphost.common.EventManager.EventType;
import com.android.car.libraries.apphost.common.StatusBarManager.StatusBarState;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.common.ThreadUtils;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.template.view.model.ActionStripWrapper;
import com.android.car.libraries.apphost.view.AbstractSurfaceTemplatePresenter;
import com.android.car.libraries.apphost.view.TemplatePresenter;
import com.android.car.libraries.apphost.view.common.CarTextParams;
import com.android.car.libraries.apphost.view.common.ImageUtils;
import com.android.car.libraries.apphost.view.common.ImageViewParams;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.animation.AnimationListenerAdapter;
import com.android.car.libraries.templates.host.view.widgets.common.ActionStripView;
import com.android.car.libraries.templates.host.view.widgets.common.BleedingCardView;
import com.android.car.libraries.templates.host.view.widgets.common.PanOverlayView;
import com.android.car.libraries.templates.host.view.widgets.navigation.CompactStepView;
import com.android.car.libraries.templates.host.view.widgets.navigation.DetailedStepView;
import com.android.car.libraries.templates.host.view.widgets.navigation.MessageView;
import com.android.car.libraries.templates.host.view.widgets.navigation.ProgressView;
import com.android.car.libraries.templates.host.view.widgets.navigation.TravelEstimateView;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link TemplatePresenter} that shows various navigation cards such as routing cards,
 * destination cards etc.
 */
public class NavigationTemplatePresenter extends AbstractSurfaceTemplatePresenter
    implements ActionStripView.ActiveStateDelegate {
  private static final int MAX_IMAGES_PER_TEXT_LINE = 2;

  /** Percentage to lower the brightness of the card's background. */
  private static final float CARD_BACKGROUND_DARKEN_PERCENTAGE = 0.2f;

  /** Percentage to lower the brightness of the compact step section of the card's background. */
  private static final float COMPACT_STEP_CARD_BACKGROUND_DARKEN_PERCENTAGE = 0.4f;

  /** The ratio between the junction image max height to the routing card. */
  private static final float JUNCTION_IMAGE_MAX_HEIGHT_TO_CARD_WIDTH_RATIO = 0.625f;

  /** The ratio between the lanes image container height to the routing card. */
  private static final float LANES_IMAGE_CONTAINER_HEIGHT_TO_CARD_WIDTH_RATIO = 0.175f;

  /**
   * {@link #showActionStripViews()} is called in {@link #onStart()}, but a bug in GMS core causes
   * {@link View#isInTouchMode()} in {@link #onStart()} to return {@code true} even in rotary or
   * touchpad mode (b/128031459), which prevents the action strip from taking the input focus. We
   * use this listener to call {@link #showActionStripViews()} after the touch mode changes to the
   * correct value.
   */
  private final OnGlobalFocusChangeListener mOnGlobalFocusChangeListener =
      new OnGlobalFocusChangeListener() {
        // call to showActionStripView() not allowed on the given receiver.
        @SuppressWarnings("nullness:method.invocation")
        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
          if (newFocus != null) {
            showActionStripViews();
          }
        }
      };

  @ColorInt private final int mNavCardFallbackContentColor;

  private int mStepsCardContainerVisibility = GONE;
  private int mTravelEstimateContainerVisibility = GONE;

  private final ViewGroup mRootView;
  private final BleedingCardView mStepsCardContainer;
  private final ViewGroup mStepsContainer;
  private final MessageView mMessageView;
  private final ProgressView mProgressView;
  private final ViewGroup mTravelEstimateContainer;
  private final ImageView mJunctionImageView;
  private final FrameLayout mJunctionImageContainer;
  private final FrameLayout mLanesImageContainerView;
  private final DetailedStepView mDetailedStepView;
  private final CompactStepView mCompactStepView;
  private final TravelEstimateView mTravelEstimateView;
  private final ActionStripView mActionStripView;
  private final ActionStripView mMapActionStripView;
  private final PanOverlayView mPanOverlay;
  private final CarTextParams mCurrentStepParams;
  private final CarTextParams mNextStepParams;
  @ColorInt private final int mDefaultCardBackgroundColor;

  /** Creates a {@link NavigationTemplatePresenter}. */
  public static NavigationTemplatePresenter create(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    NavigationTemplatePresenter presenter =
        new NavigationTemplatePresenter(templateContext, templateWrapper);
    presenter.update();
    return presenter;
  }

  @Override
  public void onStart() {
    super.onStart();

    showActionStripViews();
    EventManager eventManager = getTemplateContext().getEventManager();
    eventManager.subscribeEvent(
        this, EventType.TEMPLATE_TOUCHED_OR_FOCUSED, this::showActionStripViews);
    eventManager.subscribeEvent(this, EventType.WINDOW_FOCUS_CHANGED, this::showActionStripViews);
    eventManager.subscribeEvent(
        this,
        EventType.CONFIGURATION_CHANGED,
        () -> {
          NavigationTemplate template = (NavigationTemplate) getTemplate();
          updateActionStrip(template.getActionStrip());
          updateMapActionStrip(template.getMapActionStrip());
          wrapActionStripsIfNeeded();
        });
    getView().getViewTreeObserver().addOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);
    getTemplateContext().getRoutingInfoState().setIsRoutingInfoVisible(true);
  }

  @Override
  public void onStop() {
    getTemplateContext().getRoutingInfoState().setIsRoutingInfoVisible(false);
    EventManager eventManager = getTemplateContext().getEventManager();
    eventManager.unsubscribeEvent(this, EventType.TEMPLATE_TOUCHED_OR_FOCUSED);
    eventManager.unsubscribeEvent(this, EventType.WINDOW_FOCUS_CHANGED);
    eventManager.unsubscribeEvent(this, EventType.CONFIGURATION_CHANGED);
    getView().getViewTreeObserver().removeOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);

    super.onStop();
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
  public boolean isPanAndZoomEnabled() {
    return getTemplateContext().getCarHostConfig().isNavPanZoomEnabled();
  }

  @Override
  public void onPanModeChanged(boolean isInPanMode) {
    showActionStripViews();
    updateVisibility(isInPanMode);
    dispatchPanModeChange(isInPanMode);
  }

  @Override
  protected View getDefaultFocusedView() {
    // Check the action strip visibility because the action buttons can take focus even when the
    // action strip is gone.
    if (mActionStripView.getVisibility() == VISIBLE) {
      return mActionStripView;
    }
    if (mMapActionStripView.getVisibility() == VISIBLE) {
      return mMapActionStripView;
    }
    return super.getDefaultFocusedView();
  }

  @Override
  public void calculateAdditionalInset(Rect inset) {
    // The portrait inset is more favorable to portrait screens it is calculated as the following
    // bounding box:
    // * left: inset left
    // * right: Min(inset right, mapActionStrip left)
    // * top: Max(inset top, actionStrip bottom, steps card bottom)
    // * bottom: Min(inset bottom, travelEstimateContainer)
    Rect portraitScreenInset = new Rect(inset);

    // The landscape inset is more favorable to landscape screens it is calculated as the following
    // bounding box:
    // * left: Max(inset left, travelEstimateContainer, stepsContainer)
    // * right: Min(inset right, mapActionStrip left)
    // * top: Max(inset top, actionStrip bottom)
    // * bottom: inset bottom
    Rect landscapeScreenInset = new Rect(inset);

    if (mMapActionStripView.getVisibility() == View.VISIBLE) {
      landscapeScreenInset.right = min(landscapeScreenInset.right, mMapActionStripView.getLeft());
      portraitScreenInset.right = min(portraitScreenInset.right, mMapActionStripView.getLeft());
    }
    if (mActionStripView.getVisibility() == VISIBLE) {
      landscapeScreenInset.top = max(landscapeScreenInset.top, mActionStripView.getBottom());
      portraitScreenInset.top = max(portraitScreenInset.top, mActionStripView.getBottom());
    }
    if (mTravelEstimateContainerVisibility == VISIBLE) {
      portraitScreenInset.bottom =
          min(portraitScreenInset.bottom, mTravelEstimateContainer.getTop());
      landscapeScreenInset.left =
          max(landscapeScreenInset.left, mTravelEstimateContainer.getRight());
    }
    if (mStepsCardContainerVisibility == View.VISIBLE) {
      landscapeScreenInset.left = max(landscapeScreenInset.left, mStepsCardContainer.getRight());
      portraitScreenInset.top = max(portraitScreenInset.top, mStepsCardContainer.getBottom());
    }
    int landscapeScreenArea = landscapeScreenInset.height() * landscapeScreenInset.width();
    int portraitScreenArea = portraitScreenInset.height() * portraitScreenInset.width();
    inset.set(
        landscapeScreenArea > portraitScreenArea ? landscapeScreenInset : portraitScreenInset);
  }

  @Override
  public void onActiveStateVisibilityChanged() {
    requestVisibleAreaUpdate();
  }

  private void update() {
    NavigationTemplate template = (NavigationTemplate) getTemplate();
    updateActionStrip(template.getActionStrip());
    updateMapActionStrip(template.getMapActionStrip());
    getPanZoomManager().setEnabled(hasPanButton());
    setStepsCardBackgroundColor();
    setStepsCardContentColor();

    TransitionManager.beginDelayedTransition(
        mRootView,
        TransitionInflater.from(getTemplateContext())
            .inflateTransition(R.transition.routing_card_transition));

    @ColorInt int cardBackgroundColor = mStepsCardContainer.getCardBackgroundColor();
    boolean shouldHideTravelEstimate = false;
    NavigationInfo navigationInfo = template.getNavigationInfo();
    if (navigationInfo == null) {
      mStepsCardContainerVisibility = GONE;
      mProgressView.setVisibility(GONE);
      mStepsContainer.setVisibility(GONE);
      mMessageView.setVisibility(GONE);
    } else if (navigationInfo instanceof RoutingInfo) {
      RoutingInfo routingInfo = (RoutingInfo) navigationInfo;

      if (routingInfo.isLoading()) {
        mStepsCardContainerVisibility = VISIBLE;
        mProgressView.setVisibility(VISIBLE);
        mStepsContainer.setVisibility(GONE);
        mMessageView.setVisibility(GONE);
      } else {

        boolean shouldShowJunctionImage =
            ImageUtils.setImageSrc(
                getTemplateContext(),
                routingInfo.getJunctionImage(),
                mJunctionImageView,
                ImageViewParams.DEFAULT);

        boolean shouldShowNextStep = routingInfo.getNextStep() != null;

        mDetailedStepView.setStepAndDistance(
            getTemplateContext(),
            routingInfo.getCurrentStep(),
            routingInfo.getCurrentDistance(),
            mCurrentStepParams,
            cardBackgroundColor,
            shouldShowJunctionImage);
        mCompactStepView.setStep(
            getTemplateContext(), routingInfo.getNextStep(), mNextStepParams, cardBackgroundColor);

        if (shouldShowJunctionImage) {
          mJunctionImageContainer.setVisibility(VISIBLE);
          mCompactStepView.setVisibility(GONE);
          shouldHideTravelEstimate = true;
        } else {
          mJunctionImageContainer.setVisibility(GONE);
          mCompactStepView.setVisibility(shouldShowNextStep ? VISIBLE : GONE);
        }

        boolean hasNextStepOrJunction = shouldShowJunctionImage || shouldShowNextStep;
        mStepsCardContainer
            .findViewById(R.id.divider)
            .setVisibility(hasNextStepOrJunction ? VISIBLE : GONE);

        mStepsCardContainerVisibility = VISIBLE;
        mProgressView.setVisibility(GONE);
        mStepsContainer.setVisibility(VISIBLE);
        mMessageView.setVisibility(GONE);
      }
    } else if (navigationInfo instanceof MessageInfo) {
      MessageInfo messageInfo = (MessageInfo) navigationInfo;
      CarText title = messageInfo.getTitle();
      if (title == null) {
        L.w(LogTags.TEMPLATE, "Title for the message is expected but not set");
        title = CarText.create("");
      }
      mMessageView.setMessage(
          getTemplateContext(),
          messageInfo.getImage(),
          title,
          messageInfo.getText(),
          cardBackgroundColor);
      mStepsCardContainerVisibility = VISIBLE;
      mProgressView.setVisibility(GONE);
      mStepsContainer.setVisibility(GONE);
      mMessageView.setVisibility(VISIBLE);
    } else {
      L.w(LogTags.TEMPLATE, "Unknown navigation info: %s", navigationInfo);
    }

    TravelEstimate travelEstimate = template.getDestinationTravelEstimate();
    if (travelEstimate == null || shouldHideTravelEstimate) {
      mTravelEstimateContainerVisibility = GONE;
    } else {
      mTravelEstimateView.setTravelEstimate(getTemplateContext(), travelEstimate);
      mTravelEstimateContainerVisibility = VISIBLE;
    }

    updateVisibility(getPanZoomManager().isInPanMode());

    // Wrap action strips after the visibility update, because we need to know if the routing
    // card is visible in order to decide whether the action strips need to be wrapped.
    wrapActionStripsIfNeeded();

    getTemplateContext().getSurfaceInfoProvider().invalidateStableArea();
    requestVisibleAreaUpdate();
  }

  /**
   * Navigation template allows up to 4 buttons, which may overlap with the routing card container
   * in small screens. In this case, draw the buttons in 2 lines to avoid the overlap.
   */
  // TODO(b/191828230): Determine the action strip overlaps properly
  private void wrapActionStripsIfNeeded() {
    ThreadUtils.runOnMain(
        () -> {
          NavigationTemplate template = (NavigationTemplate) getTemplate();
          int screenWidth = getTemplateContext().getResources().getDisplayMetrics().widthPixels;
          int screenHeight = getTemplateContext().getResources().getDisplayMetrics().heightPixels;

          // Measure and layout manually to get the correct view widths.
          mRootView.measure(
              MeasureSpec.makeMeasureSpec(screenWidth, MeasureSpec.EXACTLY),
              MeasureSpec.makeMeasureSpec(screenHeight, MeasureSpec.EXACTLY));
          mRootView.layout(0, 0, screenWidth, screenHeight);

          // We calculate the right side of the card container and the left side of the
          // action strip because the manual measure and layout calls do not produce the
          // correct view position in the window.
          MarginLayoutParams stepsCardContainerLayoutParams =
              (MarginLayoutParams) mStepsCardContainer.getLayoutParams();
          int stepsCardContainerRight =
              stepsCardContainerLayoutParams.getMarginStart()
                  + stepsCardContainerLayoutParams.width;
          int actionStripViewLeft = screenWidth - mActionStripView.getWidth();

          // If the card container and the action strip view overlap, draw the action
          // strip in 2 lines to avoid the overlap.
          if (mStepsCardContainer.getVisibility() == VISIBLE
              && mActionStripView.getVisibility() == VISIBLE
              && stepsCardContainerRight > actionStripViewLeft) {
            updateActionStrip(template.getActionStrip(), /* allowTwoLines= */ true);
          }

          // We calculate the bottom side of the action strip and the top side of the map
          // action strip because the manual measure and layout calls do not produce the
          // correct view position in the window.
          int actionStripViewBottom = mActionStripView.getBottom();
          int mapActionStripViewTop = mMapActionStripView.getTop();

          // If the action strip and the map action strip views overlap, draw the map
          // action strip in 2 lines to avoid the overlap.
          if (mActionStripView.getVisibility() == VISIBLE
              && mMapActionStripView.getVisibility() == VISIBLE
              && actionStripViewBottom > mapActionStripViewTop) {
            updateMapActionStrip(template.getMapActionStrip(), /* allowTwoLines= */ true);
          }
        });
  }

  private void updateActionStrip(@Nullable ActionStrip actionStrip) {
    updateActionStrip(actionStrip, /* allowTwoLines= */ false);
  }

  private void updateActionStrip(@Nullable ActionStrip actionStrip, boolean allowTwoLines) {
    mActionStripView.setActionStrip(
        getTemplateContext(),
        actionStrip,
        ActionsConstraints.ACTIONS_CONSTRAINTS_NAVIGATION,
        allowTwoLines);
  }

  private void updateMapActionStrip(@Nullable ActionStrip actionStrip) {
    updateMapActionStrip(actionStrip, /* allowTwoLines= */ false);
  }

  private void updateMapActionStrip(@Nullable ActionStrip actionStrip, boolean allowTwoLines) {
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
        allowTwoLines);
  }

  private void showActionStripViews() {
    boolean isInPanMode = getPanZoomManager().isInPanMode();

    // Show the action strip when not in the pan mode.
    mActionStripView.setActiveState(!isInPanMode);
    mMapActionStripView.setActiveState(true);

    // If nothing was focused, set the default focus.
    if (!mRootView.hasFocus()) {
      setDefaultFocus();
    }

    // The action strip view should fade if the action strip or the window is not focused.
    if (!(mActionStripView.hasFocus() || mMapActionStripView.hasFocus()) || !hasWindowFocus()) {
      mActionStripView.setActiveStateWithDelay(false, ACTIONSTRIP_ACTIVE_STATE_DURATION_MILLIS);

      // Fade the map action strip only when not in the pan mode.
      if (!isInPanMode) {
        mMapActionStripView.setActiveStateWithDelay(
            false, ACTIONSTRIP_ACTIVE_STATE_DURATION_MILLIS);
      }
    }
  }

  private void attachActiveStateDelegate() {
    mActionStripView.setActiveStateDelegate(this);
    mMapActionStripView.setActiveStateDelegate(this);
  }

  @SuppressLint("InflateParams")
  @SuppressWarnings("nullness:method.invocation")
  private NavigationTemplatePresenter(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    super(templateContext, templateWrapper, StatusBarState.OVER_SURFACE);

    // Read the fallback color to use with the app-defined card background color.
    @StyleableRes final int[] themeAttrs = {R.attr.templateNavCardFallbackContentColor};
    TypedArray ta = templateContext.obtainStyledAttributes(themeAttrs);
    mNavCardFallbackContentColor = ta.getColor(0, Color.WHITE);
    ta.recycle();

    mRootView =
        (ViewGroup)
            LayoutInflater.from(templateContext).inflate(R.layout.navigation_template_layout, null);
    mStepsCardContainer = mRootView.findViewById(R.id.content_container);
    mMessageView = mRootView.findViewById(R.id.message_view);
    mProgressView = mRootView.findViewById(R.id.progress_view);
    mStepsContainer = mRootView.findViewById(R.id.steps_container);

    mJunctionImageContainer = mRootView.findViewById(R.id.junction_image_container);
    mJunctionImageView = mRootView.findViewById(R.id.junction_image);
    mLanesImageContainerView = mRootView.findViewById(R.id.lanes_image_container);
    mDetailedStepView = mRootView.findViewById(R.id.detailed_step_view);
    mCompactStepView = mRootView.findViewById(R.id.compact_step_view);
    mTravelEstimateContainer = mRootView.findViewById(R.id.travel_estimate_card_container);
    mTravelEstimateView = mRootView.findViewById(R.id.travel_estimate_view);
    mActionStripView = mRootView.findViewById(R.id.action_strip);
    mMapActionStripView = mRootView.findViewById(R.id.map_action_strip);
    mPanOverlay = mRootView.findViewById(R.id.pan_overlay);

    mCurrentStepParams = createStepTextParams(/* isNextStep= */ false);
    mNextStepParams = createStepTextParams(/* isNextStep= */ true);
    mDefaultCardBackgroundColor = mStepsCardContainer.getCardBackgroundColor();

    setStepsCardBackgroundColor();

    // Set the junction image max height and lanes image container height.
    setJunctionImageMaxHeight();
    setLanesImageContainerHeight();

    attachActiveStateDelegate();
  }

  /**
   * Returns a {@link CarTextParams} instance to use for the text of a step.
   *
   * <p>Unlike other text elsewhere, image spans are allowed in these strings.
   */
  @SuppressLint("ResourceType")
  private CarTextParams createStepTextParams(boolean isNextStep) {
    TemplateContext templateContext = getTemplateContext();

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateRoutingImageSpanRatio,
      R.attr.templateRoutingImageSpanBody2MaxHeight,
      R.attr.templateRoutingImageSpanBody3MaxHeight,
    };
    TypedArray ta = templateContext.obtainStyledAttributes(themeAttrs);
    float imageRatio = ta.getFloat(0, 0.f);
    int body2MaxHeight = ta.getDimensionPixelSize(1, 0);
    int body3MaxHeight = ta.getDimensionPixelSize(2, 0);
    ta.recycle();

    int maxHeight = isNextStep ? body3MaxHeight : body2MaxHeight;
    int maxWidth = (int) (maxHeight * imageRatio);
    return CarTextParams.builder()
        .setImageBoundingBox(new Rect(0, 0, maxWidth, maxHeight))
        .setMaxImages(MAX_IMAGES_PER_TEXT_LINE)
        .setColorSpanConstraints(CarColorConstraints.NO_COLOR)
        .build();
  }

  private void showTravelEstimateContainer() {
    if (mTravelEstimateContainer.getVisibility() == VISIBLE) {
      return;
    }

    mTravelEstimateContainer.setVisibility(VISIBLE);
    Animation animation =
        AnimationUtils.loadAnimation(
            getTemplateContext(), R.anim.travel_estimate_card_show_animation);
    mTravelEstimateContainer.setAnimation(animation);
  }

  private void hideTravelEstimateContainer() {
    if (mTravelEstimateContainer.getVisibility() == GONE) {
      return;
    }
    Animation animation =
        AnimationUtils.loadAnimation(
            getTemplateContext(), R.anim.travel_estimate_card_hide_animation);
    // TODO(b/180455232): Create default AnimationListenerListener with empty methods.
    animation.setAnimationListener(
        new AnimationListenerAdapter() {
          @Override
          public void onAnimationEnd(Animation animation) {
            mTravelEstimateContainer.setVisibility(GONE);
          }
        });
    mTravelEstimateContainer.setAnimation(animation);
  }

  private void setStepsCardBackgroundColor() {
    // Set the card's background color to the one provided in the template, if any.
    CarColor backgroundColor = ((NavigationTemplate) getTemplate()).getBackgroundColor();
    @ColorInt int backgroundColorInt;
    if (backgroundColor != null) {
      backgroundColorInt =
          CarColorUtils.resolveColor(
              getTemplateContext(),
              backgroundColor,
              false,
              Color.BLACK,
              CarColorConstraints.UNCONSTRAINED);
    } else {
      backgroundColorInt = mDefaultCardBackgroundColor;
    }

    // Darken the background of the card.
    mStepsCardContainer.setCardBackgroundColor(
        CarColorUtils.darkenColor(backgroundColorInt, CARD_BACKGROUND_DARKEN_PERCENTAGE));

    // Darken the background of the compat step view.
    // We also create a drawable for it that has bottom rounded corners because otherwise the
    // background of the card won't clip within the parent's outline. It is probably possible to
    // do the clipping using a convex path (getting the card's background outline and using that
    // does not work as it returns a rect and not a path), and setting it through an outline
    // provider but this is cheaper regardless as clipping is an expensive operation.
    float bottomRadius = mStepsCardContainer.getCardRadius();
    GradientDrawable drawable = new GradientDrawable();
    drawable.setCornerRadii(
        new float[] {0, 0, 0, 0, bottomRadius, bottomRadius, bottomRadius, bottomRadius});
    drawable.setColor(
        CarColorUtils.darkenColor(
            backgroundColorInt, COMPACT_STEP_CARD_BACKGROUND_DARKEN_PERCENTAGE));
    mCompactStepView.setBackground(drawable);
  }

  private void setStepsCardContentColor() {
    if (((NavigationTemplate) getTemplate()).getBackgroundColor() != null) {
      // Use the fallback content color if the app-defined card background color is used,
      // because the OEM-defined text color may not have the adequate contrast ratio with the
      // card background color.
      mDetailedStepView.setTextColor(mNavCardFallbackContentColor);
      mCompactStepView.setTextColor(mNavCardFallbackContentColor);
      mMessageView.setTextColor(mNavCardFallbackContentColor);
      mProgressView.setColor(mNavCardFallbackContentColor);
    } else {
      mDetailedStepView.setDefaultTextColor();
      mCompactStepView.setDefaultTextColor();
      mMessageView.setDefaultTextColor();
      mProgressView.setDefaultColor();
    }
  }

  private void setJunctionImageMaxHeight() {
    int stepsCardContainerWidth = mStepsCardContainer.getLayoutParams().width;
    int junctionImageMaxHeight =
        (int) (stepsCardContainerWidth * JUNCTION_IMAGE_MAX_HEIGHT_TO_CARD_WIDTH_RATIO);
    mJunctionImageView.setMaxHeight(junctionImageMaxHeight);
  }

  private void setLanesImageContainerHeight() {
    int stepsCardContainerWidth = mStepsCardContainer.getLayoutParams().width;
    int lanesImageContainerHeight =
        (int) (stepsCardContainerWidth * LANES_IMAGE_CONTAINER_HEIGHT_TO_CARD_WIDTH_RATIO);
    mLanesImageContainerView.getLayoutParams().height = lanesImageContainerHeight;
  }

  private boolean hasPanButton() {
    NavigationTemplate template = (NavigationTemplate) getTemplate();
    ActionStrip mapActionStrip = template.getMapActionStrip();
    return mapActionStrip != null && mapActionStrip.getFirstActionOfType(Action.TYPE_PAN) != null;
  }

  private void dispatchPanModeChange(boolean isInPanMode) {
    NavigationTemplate template = (NavigationTemplate) getTemplate();
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
            mStepsCardContainer.setVisibility(GONE);
            mActionStripView.setActiveState(false);
            hideTravelEstimateContainer();
          } else {
            mPanOverlay.setVisibility(GONE);
            mStepsCardContainer.setVisibility(mStepsCardContainerVisibility);
            if (mTravelEstimateContainerVisibility == VISIBLE) {
              showTravelEstimateContainer();
            } else {
              hideTravelEstimateContainer();
            }
          }
        });
  }
}
