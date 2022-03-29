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
package com.android.car.libraries.apphost.template;

import static com.android.car.libraries.apphost.common.EventManager.EventType.SURFACE_STABLE_AREA;
import static com.android.car.libraries.apphost.common.EventManager.EventType.SURFACE_VISIBLE_AREA;
import static com.android.car.libraries.apphost.logging.TelemetryHandler.getErrorType;
import static com.google.common.base.Preconditions.checkNotNull;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.location.Location;
import android.os.RemoteException;
import android.view.Surface;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.CarAppPermission;
import androidx.car.app.FailureResponse;
import androidx.car.app.IAppHost;
import androidx.car.app.IAppManager;
import androidx.car.app.ISurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.LongMessageTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import androidx.car.app.model.signin.SignInTemplate;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.PlaceListNavigationTemplate;
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.versioning.CarAppApiLevels;
import com.android.car.libraries.apphost.AbstractHost;
import com.android.car.libraries.apphost.Host;
import com.android.car.libraries.apphost.common.ANRHandler;
import com.android.car.libraries.apphost.common.CarAppError;
import com.android.car.libraries.apphost.common.LocationMediator;
import com.android.car.libraries.apphost.common.OnDoneCallbackStub;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.common.ThreadUtils;
import com.android.car.libraries.apphost.distraction.FlowViolationException;
import com.android.car.libraries.apphost.distraction.OverLimitFlowViolationException;
import com.android.car.libraries.apphost.distraction.TemplateValidator;
import com.android.car.libraries.apphost.distraction.checkers.GridTemplateChecker;
import com.android.car.libraries.apphost.distraction.checkers.ListTemplateChecker;
import com.android.car.libraries.apphost.distraction.checkers.MessageTemplateChecker;
import com.android.car.libraries.apphost.distraction.checkers.NavigationTemplateChecker;
import com.android.car.libraries.apphost.distraction.checkers.PaneTemplateChecker;
import com.android.car.libraries.apphost.distraction.checkers.PlaceListMapTemplateChecker;
import com.android.car.libraries.apphost.distraction.checkers.PlaceListNavigationTemplateChecker;
import com.android.car.libraries.apphost.distraction.checkers.RoutePreviewNavigationTemplateChecker;
import com.android.car.libraries.apphost.distraction.checkers.SignInTemplateChecker;
import com.android.car.libraries.apphost.distraction.checkers.TemplateChecker;
import com.android.car.libraries.apphost.logging.CarAppApi;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.TelemetryEvent;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import com.android.car.libraries.apphost.logging.TelemetryHandler;
import com.android.car.libraries.apphost.view.SurfaceProvider;
import com.android.car.libraries.apphost.view.SurfaceProvider.SurfaceProviderListener;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link Host} implementation that handles communication between the client app and the rest of
 * the host.
 *
 * <p>Host services are per app, and live for the duration of a car session.
 *
 * <p>A host service keeps a reference to a {@link UIController} object which it delegates UI calls
 * to, and is responsible for making all the necessary checks to let the operations go through e.g.
 * check that the backing context (an activity or fragment) is alive, the app is in started state,
 * etc.
 *
 * <p>The {@link UIController} instance may be updated when the backing context is re-created, e.g.
 * during config changes such as light/dark mode switches.
 */
public class AppHost extends AbstractHost {
  private final IAppHost.Stub mAppHostStub = new AppHostStub();
  private final AppManagerDispatcher mDispatcher;

  private UIController mUIController;
  @Nullable private ISurfaceCallback mSurfaceListener;
  @Nullable private SurfaceContainer mSurfaceContainer;
  private final AtomicBoolean mIsPendingGetTemplate = new AtomicBoolean(false);
  private final TemplateValidator mTemplateValidator;
  private final TelemetryHandler mTelemetryHandler;

  private final SurfaceProvider.SurfaceProviderListener mSurfaceProviderListener =
      new SurfaceProviderListener() {
        @Override
        public void onSurfaceCreated() {
          L.d(LogTags.TEMPLATE, "SurfaceProvider: Surface created");
        }

        // call to onVisibleAreaChanged() not allowed on the given receiver.
        // call to onStableAreaChanged() not allowed on the given receiver.
        @SuppressWarnings("nullness:method.invocation")
        @Override
        public void onSurfaceChanged() {
          SurfaceContainer container = createOrReuseContainer();
          mSurfaceContainer = container;

          ISurfaceCallback listener = mSurfaceListener;
          if (listener != null) {
            mTemplateContext.getAppDispatcher().dispatchSurfaceAvailable(listener, container);
          }
          L.d(LogTags.TEMPLATE, "SurfaceProvider: Surface updated: %s.", container);

          onVisibleAreaChanged();
          onStableAreaChanged();
        }

        @Override
        public void onSurfaceDestroyed() {
          SurfaceContainer container = createOrReuseContainer();
          mSurfaceContainer = container;

          ISurfaceCallback listener = mSurfaceListener;
          if (listener != null) {
            mTemplateContext.getAppDispatcher().dispatchSurfaceDestroyed(listener, container);
          }

          mSurfaceContainer = null;
          L.d(LogTags.TEMPLATE, "SurfaceProvider: Surface destroyed");
        }

        // call to onSurfaceScroll(float,float) not allowed on the given receiver.
        @SuppressWarnings("nullness:method.invocation")
        @Override
        public void onSurfaceScroll(float distanceX, float distanceY) {
          AppHost.this.onSurfaceScroll(distanceX, distanceY);
        }

        // call to onSurfaceScroll(float,float) not allowed on the given receiver.
        @SuppressWarnings("nullness:method.invocation")
        @Override
        public void onSurfaceFling(float velocityX, float velocityY) {
          AppHost.this.onSurfaceFling(velocityX, velocityY);
        }

        // call to onSurfaceScroll(float,float) not allowed on the given receiver.
        @SuppressWarnings("nullness:method.invocation")
        @Override
        public void onSurfaceScale(float focusX, float focusY, float scaleFactor) {
          AppHost.this.onSurfaceScale(focusX, focusY, scaleFactor);
        }

        private SurfaceContainer createOrReuseContainer() {
          // dereference of possibly-null reference uiController
          // dereference of possibly-null reference dispatcher
          @SuppressWarnings("nullness:dereference.of.nullable")
          SurfaceProvider provider = mUIController.getSurfaceProvider(mDispatcher.getAppName());

          Surface surface = provider.getSurface();
          int width = provider.getWidth();
          int height = provider.getHeight();
          int dpi = provider.getDpi();

          if (mSurfaceContainer != null
              && mSurfaceContainer.getSurface() == surface
              && mSurfaceContainer.getWidth() == width
              && mSurfaceContainer.getHeight() == height
              && mSurfaceContainer.getDpi() == dpi) {
            return mSurfaceContainer;
          }

          return new SurfaceContainer(surface, width, height, dpi);
        }
      };

  /**
   * Creates a template host service.
   *
   * @param uiController the controller to delegate UI calls to. Can be updated with {@link
   *     #setUIController(UIController)} henceforth
   * @param appBinding the binding to use to dispatch client calls
   */
  public static AppHost create(
      UIController uiController, Object appBinding, TemplateContext templateContext) {
    return new AppHost(uiController, AppManagerDispatcher.create(appBinding), templateContext);
  }

  @Override
  public IAppHost.Stub getBinder() {
    assertIsValid();
    return mAppHostStub;
  }

  @Override
  public void onCarAppBound() {
    super.onCarAppBound();

    updateUiControllerListener();
    mTemplateValidator.reset();
    getTemplate();
  }

  @Override
  public void onNewIntentDispatched() {
    super.onNewIntentDispatched();

    getTemplate();
  }

  @Override
  public void onBindToApp(Intent intent) {
    super.onBindToApp(intent);

    if (mTemplateContext.getCarHostConfig().isNewTaskFlowIntent(intent)) {
      mTemplateValidator.reset();
    }
  }

  @Override
  public void reportStatus(PrintWriter pw, Pii piiHandling) {
    pw.printf("- flow validator: %s\n", mTemplateValidator);
    pw.printf("- surface: %s\n", mSurfaceContainer);
  }

  /** Dispatches an on-back-pressed event. */
  public void onBackPressed() {
    assertIsValid();
    mDispatcher.dispatchOnBackPressed(mTemplateContext);
  }

  /** Informs the app to start or stop sending location updates. */
  public void trySetEnableLocationUpdates(boolean enable) {
    assertIsValid();

    // The enableLocationUpdates API is only available for API level 4+.
    int apiLevel = mTemplateContext.getCarHostConfig().getNegotiatedApi();
    if (apiLevel <= CarAppApiLevels.LEVEL_3) {
      L.e(LogTags.APP_HOST, "Attempt to request location updates for app Api level %s", apiLevel);
      return;
    }

    if (enable) {
      mDispatcher.dispatchStartLocationUpdates(mTemplateContext);
    } else {
      mDispatcher.dispatchStopLocationUpdates(mTemplateContext);
    }
  }

  /** Dispatches a surface scroll event. */
  public void onSurfaceScroll(float distanceX, float distanceY) {
    ISurfaceCallback listener = mSurfaceListener;
    if (listener != null) {
      mTemplateContext.getAppDispatcher().dispatchOnSurfaceScroll(listener, distanceX, distanceY);
      L.d(LogTags.TEMPLATE, "SurfaceProvider: Surface scroll: [%f, %f]", distanceX, distanceY);
    }
  }

  /** Dispatches a surface fling event. */
  public void onSurfaceFling(float velocityX, float velocityY) {
    ISurfaceCallback listener = mSurfaceListener;
    if (listener != null) {
      mTemplateContext.getAppDispatcher().dispatchOnSurfaceFling(listener, velocityX, velocityY);
      L.d(LogTags.TEMPLATE, "SurfaceProvider: Surface fling: [%f, %f]", velocityX, velocityY);
    }
  }

  /** Dispatches a surface scale event. */
  public void onSurfaceScale(float focusX, float focusY, float scaleFactor) {
    ISurfaceCallback listener = mSurfaceListener;
    if (listener != null) {
      mTemplateContext
          .getAppDispatcher()
          .dispatchOnSurfaceScale(listener, focusX, focusY, scaleFactor);
      L.d(LogTags.TEMPLATE, "SurfaceProvider: Surface scale: [%f]", scaleFactor);
    }
  }

  /**
   * Updates the current {@link UIController}.
   *
   * <p>This is normally called when the caller detects that the controller set in the service is
   * stale due to its backing context being destroyed.
   */
  public void setUIController(UIController uiController) {
    assertIsValid();

    removeUiControllerListener();
    mUIController = uiController;

    updateUiControllerListener();
  }

  /** Returns the {@link UIController} attached to this app host. */
  public UIController getUIController() {
    assertIsValid();
    return mUIController;
  }

  /**
   * Returns the {@link TemplateValidator} to use to validate whether the templates handled by this
   * host \ abide by the flow rules.
   */
  @VisibleForTesting
  public TemplateValidator getTemplateValidator() {
    return mTemplateValidator;
  }

  /** Registers a {@link TemplateChecker} for a host-only {@link Template}. */
  public <T extends Template> void registerHostTemplateChecker(
      Class<T> templateClass, TemplateChecker<T> templateChecker) {
    mTemplateValidator.registerTemplateChecker(templateClass, templateChecker);
  }

  @Override
  public void setTemplateContext(TemplateContext templateContext) {
    removeEventSubscriptions();

    super.setTemplateContext(templateContext);
    templateContext.registerAppHostService(TemplateValidator.class, mTemplateValidator);
    updateEventSubscriptions();
  }

  @Override
  public void onDisconnectedEvent() {
    removeUiControllerListener();
  }

  private void getTemplate() {
    boolean wasPendingTemplate = mIsPendingGetTemplate.getAndSet(true);
    if (wasPendingTemplate) {
      // Ignore extra invalidate calls between templates being returned.
      return;
    }

    mDispatcher.dispatchGetTemplate(this::getTemplateAppServiceCall);
  }

  private void getTemplateAppServiceCall(IAppManager manager, ANRHandler.ANRToken anrToken)
      throws RemoteException {
    manager.getTemplate(
        new OnDoneCallbackStub(mTemplateContext, anrToken) {
          @Override
          public void onSuccess(@Nullable Bundleable response) {
            super.onSuccess(response);
            mIsPendingGetTemplate.set(false);
            ComponentName appName = mDispatcher.getAppName();

            RuntimeException toThrow = null;
            try {
              TemplateWrapper wrapper = (TemplateWrapper) checkNotNull(response).get();

              // This checks whether this template meets our task flow
              // restriction guideline and will throw if the template should not
              // be added.
              mTemplateValidator.validateFlow(wrapper);
              mTemplateValidator.validateHasRequiredPermissions(mTemplateContext, wrapper);

              mUIController.setTemplate(appName, wrapper);
            } catch (BundlerException e) {
              mTelemetryHandler.logCarAppApiFailureTelemetry(
                  appName, CarAppApi.GET_TEMPLATE, getErrorType(new FailureResponse(e)));

              mTemplateContext
                  .getErrorHandler()
                  .showError(
                      CarAppError.builder(appName)
                          .setCause(e)
                          .setDebugMessage("Invalid template")
                          .build());
            } catch (SecurityException e) {
              mTelemetryHandler.logCarAppApiFailureTelemetry(
                  appName, CarAppApi.GET_TEMPLATE, getErrorType(new FailureResponse(e)));

              mTemplateContext
                  .getErrorHandler()
                  .showError(
                      CarAppError.builder(appName)
                          .setCause(e)
                          .setType(CarAppError.Type.MISSING_PERMISSION)
                          .build());
              toThrow = e;
            } catch (FlowViolationException e) {
              mTelemetryHandler.logCarAppTelemetry(
                  TelemetryEvent.newBuilder(
                      e instanceof OverLimitFlowViolationException
                          ? UiAction.TEMPLATE_FLOW_LIMIT_EXCEEDED
                          : UiAction.TEMPLATE_FLOW_INVALID_BACK,
                      appName));

              mTemplateContext
                  .getErrorHandler()
                  .showError(
                      CarAppError.builder(appName)
                          .setCause(e)
                          .setDebugMessage("Template flow restrictions violated")
                          .build());
              toThrow = new IllegalStateException(e);
            } catch (RuntimeException e) {
              mTelemetryHandler.logCarAppApiFailureTelemetry(
                  appName, CarAppApi.GET_TEMPLATE, getErrorType(new FailureResponse(e)));

              mTemplateContext
                  .getErrorHandler()
                  .showError(CarAppError.builder(appName).setCause(e).build());
              toThrow = e;
            }

            if (toThrow != null) {
              // Crash the client process if the template returned does not pass validations.
              throw toThrow;
            }
          }

          @Override
          public void onFailure(Bundleable failureResponse) {
            super.onFailure(failureResponse);
            mIsPendingGetTemplate.set(false);
          }
        });
  }

  /**
   * Dispatches a call to the template app if the surface has been created and there is a visible
   * area available.
   */
  private void onVisibleAreaChanged() {
    // Do not fire the visible area changed event until at least after the surfaceContainer
    // is created, which is triggered by the onSurfaceChanged callback.
    if (mSurfaceContainer == null) {
      return;
    }

    Rect visibleArea = mTemplateContext.getSurfaceInfoProvider().getVisibleArea();
    if (visibleArea == null) {
      return;
    }
    ISurfaceCallback listener = mSurfaceListener;
    if (listener != null) {
      mTemplateContext.getAppDispatcher().dispatchVisibleAreaChanged(listener, visibleArea);
    }
    L.d(LogTags.TEMPLATE, "SurfaceProvider: onVisibleAreaChanged: visibleArea: [%s]", visibleArea);
  }

  /**
   * Dispatches a call to the template app if the surface has been created and there is a stable
   * area visible.
   */
  private void onStableAreaChanged() {
    // Do not fire the Insets changed event until at least after the surfaceContainer
    // is created, which is triggered by the onSurfaceChanged callback.
    if (mSurfaceContainer == null) {
      return;
    }

    Rect stableArea = mTemplateContext.getSurfaceInfoProvider().getStableArea();
    if (stableArea == null) {
      return;
    }
    ISurfaceCallback listener = mSurfaceListener;
    if (listener != null) {
      mTemplateContext.getAppDispatcher().dispatchStableAreaChanged(listener, stableArea);
    }
    L.d(LogTags.DISTRACTION, "SurfaceProvider: onStableAreaChanged: stableArea: [%s]", stableArea);
  }

  private void registerTemplateValidators() {
    mTemplateValidator.registerTemplateChecker(GridTemplate.class, new GridTemplateChecker());
    mTemplateValidator.registerTemplateChecker(ListTemplate.class, new ListTemplateChecker());
    mTemplateValidator.registerTemplateChecker(MessageTemplate.class, new MessageTemplateChecker());
    mTemplateValidator.registerTemplateChecker(
        NavigationTemplate.class, new NavigationTemplateChecker());
    mTemplateValidator.registerTemplateChecker(PaneTemplate.class, new PaneTemplateChecker());
    mTemplateValidator.registerTemplateChecker(
        PlaceListMapTemplate.class, new PlaceListMapTemplateChecker());
    mTemplateValidator.registerTemplateChecker(
        PlaceListNavigationTemplate.class, new PlaceListNavigationTemplateChecker());
    mTemplateValidator.registerTemplateChecker(
        RoutePreviewNavigationTemplate.class, new RoutePreviewNavigationTemplateChecker());
    mTemplateValidator.registerTemplateChecker(SignInTemplate.class, new SignInTemplateChecker());

    // Templates that don't require refresh and permission checks.
    mTemplateValidator.registerTemplateChecker(
        LongMessageTemplate.class, (newTemplate, oldTemplate) -> true);
    mTemplateValidator.registerTemplateChecker(
        SearchTemplate.class, (newTemplate, oldTemplate) -> true);
  }

  private void updateUiControllerListener() {
    SurfaceProvider surfaceProvider =
        mUIController.getSurfaceProvider(
            mTemplateContext.getCarAppPackageInfo().getComponentName());
    if (surfaceProvider == null) {
      // We should always be able to access the surface provider at the point where the ui
      // controller is set.
      throw new IllegalStateException(
          "Can't get surface provider for "
              + mTemplateContext.getCarAppPackageInfo().getComponentName().flattenToShortString());
    }
    surfaceProvider.setListener(mSurfaceProviderListener);
  }

  private void removeUiControllerListener() {
    // Remove any outstanding surface listeners whenever the app crashes, otherwise the listener
    // may send onSurfaceDestroyed calls when the app is not bound.
    mUIController
        .getSurfaceProvider(mTemplateContext.getCarAppPackageInfo().getComponentName())
        .setListener(null);
  }

  private void updateEventSubscriptions() {
    mTemplateContext
        .getEventManager()
        .subscribeEvent(this, SURFACE_VISIBLE_AREA, AppHost.this::onVisibleAreaChanged);
    mTemplateContext
        .getEventManager()
        .subscribeEvent(this, SURFACE_STABLE_AREA, AppHost.this::onStableAreaChanged);
  }

  private void removeEventSubscriptions() {
    mTemplateContext.getEventManager().unsubscribeEvent(this, SURFACE_VISIBLE_AREA);
    mTemplateContext.getEventManager().unsubscribeEvent(this, SURFACE_STABLE_AREA);
  }

  @SuppressWarnings("nullness")
  private AppHost(
      UIController uiController, AppManagerDispatcher dispatcher, TemplateContext templateContext) {
    super(templateContext, LogTags.APP_HOST);
    mUIController = uiController;
    mDispatcher = dispatcher;
    mTemplateValidator =
        TemplateValidator.create(
            templateContext.getConstraintsProvider().getTemplateStackMaxSize());
    mTelemetryHandler = templateContext.getTelemetryHandler();

    templateContext.registerAppHostService(TemplateValidator.class, mTemplateValidator);

    registerTemplateValidators();
    updateUiControllerListener();
    updateEventSubscriptions();
  }

  /**
   * A {@link IAppHost.Stub} implementation that used to receive calls to the app host API from the
   * client.
   */
  private final class AppHostStub extends IAppHost.Stub {
    @Override
    public void invalidate() {
      runIfValid("invalidate", AppHost.this::getTemplate);
    }

    @Override
    public void showToast(CharSequence text, int duration) {
      ThreadUtils.runOnMain(
          () ->
              runIfValid(
                  "showToast",
                  () -> mTemplateContext.getToastController().showToast(text, duration)));
    }

    @Override
    public void setSurfaceCallback(@Nullable ISurfaceCallback listener) {
      runIfValid(
          "setSurfaceCallback",
          () -> {
            ComponentName appName = mDispatcher.getAppName();
            L.d(LogTags.TEMPLATE, "setSurfaceListener for %s", appName);

            Context appConfigurationContext = mTemplateContext.getAppConfigurationContext();
            if (appConfigurationContext == null) {
              L.e(LogTags.TEMPLATE, "App configuration context is null");
              return;
            }

            try {
              CarAppPermission.checkHasLibraryPermission(
                  appConfigurationContext, CarAppPermission.ACCESS_SURFACE);
            } catch (SecurityException e) {
              // Catch the Exception here to log in host before throwing to the client
              // app.
              L.w(
                  LogTags.TEMPLATE,
                  e,
                  "App %s trying to access surface when the permission was not" + " granted",
                  appName);

              throw new SecurityException(e);
            }

            ThreadUtils.runOnMain(
                () -> {
                  mSurfaceListener = listener;
                  if (mSurfaceListener == null) {
                    return;
                  }

                  if (mSurfaceContainer != null) {
                    mSurfaceProviderListener.onSurfaceChanged();
                  }
                });
          });
    }

    @Override
    public void sendLocation(Location location) {
      ThreadUtils.runOnMain(
          () ->
              runIfValid(
                  "sendLocation",
                  () ->
                      Objects.requireNonNull(
                              mTemplateContext.getAppHostService(LocationMediator.class))
                          .setAppLocation(location)));
    }
  }
}
