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
package com.android.car.libraries.apphost.internal;

import static com.android.car.libraries.apphost.common.EventManager.EventType.APP_DISCONNECTED;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import androidx.annotation.AnyThread;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.AppInfo;
import androidx.car.app.CarContext;
import androidx.car.app.HandshakeInfo;
import androidx.car.app.IAppManager;
import androidx.car.app.ICarApp;
import androidx.car.app.ICarHost;
import androidx.car.app.navigation.INavigationManager;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.core.util.Consumer;
import androidx.lifecycle.Lifecycle.Event;
import com.android.car.libraries.apphost.common.ANRHandler.ANRToken;
import com.android.car.libraries.apphost.common.AppDispatcher;
import com.android.car.libraries.apphost.common.CarAppError;
import com.android.car.libraries.apphost.common.CarHostConfig;
import com.android.car.libraries.apphost.common.IncompatibleApiException;
import com.android.car.libraries.apphost.common.IntentUtils;
import com.android.car.libraries.apphost.common.NamedAppServiceCall;
import com.android.car.libraries.apphost.common.OnDoneCallbackStub;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.common.ThreadUtils;
import com.android.car.libraries.apphost.logging.CarAppApi;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.StatusReporter;
import com.android.car.libraries.apphost.logging.TelemetryEvent;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import com.android.car.libraries.apphost.logging.TelemetryHandler;
import java.io.PrintWriter;
import java.security.InvalidParameterException;

/** Manages a binding to the {@link ICarApp} and handles the communication with it. */
public class CarAppBinding implements StatusReporter {

  private static final int MSG_UNBIND = 1;
  private static final int MSG_REBIND = 2;

  private enum BindingState {
    UNBOUND,
    BINDING,
    BOUND
  }

  private final Handler mMainHandler = new Handler(Looper.getMainLooper(), new HandlerCallback());
  private final ComponentName mAppName;
  private final ICarHost mCarHost;
  private final CarAppBindingCallback mCarAppBindingCallback;
  private final ServiceConnection mServiceConnection = new ServiceConnectionImpl();
  private final TelemetryHandler mTelemetryHandler;

  // The following fields can be updated by different threads, therefore they are volatile so that
  // readers use the latest value.

  private volatile TemplateContext mTemplateContext;

  @Nullable private volatile ICarApp mCarApp;
  @Nullable private volatile IInterface mAppManager;
  @Nullable private volatile IInterface mNavigationManager;

  @Nullable private volatile Intent mOriginalIntent;
  @Nullable private volatile ANRToken mANRToken;

  @Nullable private AppInfo mAppInfo;

  /**
   * The current state of the binding with the client app service. Use {@link
   * #setBindingState(BindingState)} to update it.
   */
  private volatile BindingState mBindingState = BindingState.UNBOUND;

  /**
   * Creates a {@link CarAppBinding} for binding to and communicating with {@code appName}.
   *
   * @param templateContext the context to retrieve template helpers from
   * @param carHost the host to send to the app when it is bound
   * @param carAppBindingCallback callback to perform once the app is bound
   */
  public static CarAppBinding create(
      TemplateContext templateContext,
      ICarHost carHost,
      CarAppBindingCallback carAppBindingCallback) {
    return new CarAppBinding(templateContext, carHost, carAppBindingCallback);
  }

  @Override
  public void reportStatus(PrintWriter pw, Pii piiHandling) {
    pw.printf("- state: %s\n", mBindingState.name());
    mTemplateContext.getCarHostConfig().reportStatus(pw, piiHandling);
  }

  /** Returns the name of the app this binding is managing. */
  @AnyThread
  public ComponentName getAppName() {
    return mAppName;
  }

  @Override
  public String toString() {
    return "[" + mAppName.flattenToShortString() + ", state: " + mBindingState + "]";
  }

  /** Sets the {@link TemplateContext} instance attached to this binding. */
  @AnyThread
  public void setTemplateContext(TemplateContext templateContext) {
    mTemplateContext = templateContext;

    AppInfo appInfo = mAppInfo;
    if (appInfo != null) {
      try {
        mTemplateContext.getCarHostConfig().updateNegotiatedApi(appInfo);
      } catch (IncompatibleApiException exception) {
        unbind(CarAppError.builder(mAppName).setCause(exception).build());
      }
    }
  }

  /** Binds to the app, if not bound already. */
  @AnyThread
  public void bind(Intent binderIntent) {
    L.d(LogTags.APP_HOST, "Binding to %s with intent %s", this, binderIntent);
    mMainHandler.removeMessages(MSG_UNBIND);
    mMainHandler.removeMessages(MSG_REBIND);
    final Intent originalIntent = IntentUtils.extractOriginalIntent(binderIntent);
    mOriginalIntent = originalIntent;

    switch (mBindingState) {
      case UNBOUND:
        setBindingState(BindingState.BINDING);

        try {
          // We bind to the app with host's capabilities, which allows the "while-in-use"
          // permission capabilities (e.g. location) in the app's process for the duration of
          // the binding.
          // See go/watevra-nav-location for more information on the process capabilities.
          if (mTemplateContext
              .getApplicationContext()
              .bindService(
                  binderIntent,
                  mServiceConnection,
                  Context.BIND_AUTO_CREATE | Context.BIND_INCLUDE_CAPABILITIES)) {
            mTemplateContext
                .getAnrHandler()
                .callWithANRCheck(CarAppApi.BIND, (currentAnrToken) -> mANRToken = currentAnrToken);
          } else {
            failedToBind(null);
          }
        } catch (SecurityException e) {
          L.e(LogTags.APP_HOST, e, "Cannot bind to the service.");
          failedToBind(e);
        }

        return;
      case BOUND:
        dispatch(
            CarContext.CAR_SERVICE,
            NamedAppServiceCall.create(
                CarAppApi.ON_NEW_INTENT,
                (ICarApp carApp, ANRToken anrToken) ->
                    carApp.onNewIntent(
                        originalIntent,
                        new OnDoneCallbackStub(mTemplateContext, anrToken) {
                          @Override
                          public void onSuccess(@Nullable Bundleable response) {
                            super.onSuccess(response);
                            ThreadUtils.runOnMain(mCarAppBindingCallback::onNewIntentDispatched);
                          }
                        })));
        return;
      case BINDING:
        L.d(LogTags.APP_HOST, "Already binding to %s", mAppName);
    }
  }

  /** Dispatches the lifecycle call for the given {@code event} to the app. */
  @AnyThread
  public void dispatchAppLifecycleEvent(Event event) {
    L.d(
        LogTags.APP_HOST,
        "Dispatching lifecycle event: %s, app: %s",
        event,
        mAppName.toShortString());

    dispatch(
        CarContext.CAR_SERVICE,
        NamedAppServiceCall.create(
            CarAppApi.DISPATCH_LIFECYCLE,
            (ICarApp carApp, ANRToken anrToken) -> {
              switch (event) {
                case ON_START:
                  carApp.onAppStart(new OnDoneCallbackStub(mTemplateContext, anrToken));
                  return;
                case ON_RESUME:
                  carApp.onAppResume(new OnDoneCallbackStub(mTemplateContext, anrToken));
                  return;
                case ON_PAUSE:
                  carApp.onAppPause(new OnDoneCallbackStub(mTemplateContext, anrToken));
                  return;
                case ON_STOP:
                  mMainHandler.removeMessages(MSG_UNBIND);
                  mMainHandler.removeMessages(MSG_REBIND);
                  mMainHandler.sendMessageDelayed(
                      mMainHandler.obtainMessage(MSG_UNBIND),
                      SECONDS.toMillis(mTemplateContext.getCarHostConfig().getAppUnbindSeconds()));
                  carApp.onAppStop(new OnDoneCallbackStub(mTemplateContext, anrToken));
                  return;
                default:
                  // fall-through
              }
              throw new InvalidParameterException("Received unexpected lifecycle event: " + event);
            }));
  }

  /**
   * Dispatches the {@code call} to the appropriate manager.
   *
   * @param managerType one of the CarServiceType as defined in {@link CarContext}
   * @param call the call to dispatch
   */
  @SuppressWarnings({"unchecked", "cast.unsafe"}) // Cannot check if instanceof ServiceT
  @AnyThread
  public <ServiceT extends IInterface> void dispatch(
      String managerType, NamedAppServiceCall<ServiceT> call) {

    ICarApp carApp = mCarApp;

    if (mBindingState != BindingState.BOUND || carApp == null) {
      mTemplateContext
          .getErrorHandler()
          .showError(
              CarAppError.builder(mAppName)
                  .setDebugMessage(
                      "App is not bound when attempting to get service: "
                          + managerType
                          + ", call: "
                          + call)
                  .build());
      return;
    }

    AppDispatcher appDispatcher = mTemplateContext.getAppDispatcher();

    switch (managerType) {
      case CarContext.APP_SERVICE:
        if (mAppManager == null) {
          dispatchGetManager(
              appDispatcher,
              managerType,
              carApp,
              manager -> {
                mAppManager = (IAppManager) manager;
                dispatchCall(appDispatcher, call, (ServiceT) mAppManager);
              });
        } else {
          dispatchCall(appDispatcher, call, (ServiceT) mAppManager);
        }
        break;
      case CarContext.NAVIGATION_SERVICE:
        if (mNavigationManager == null) {
          dispatchGetManager(
              appDispatcher,
              managerType,
              carApp,
              manager -> {
                mNavigationManager = (INavigationManager) manager;
                dispatchCall(appDispatcher, call, (ServiceT) mNavigationManager);
              });
        } else {
          dispatchCall(appDispatcher, call, (ServiceT) mNavigationManager);
        }
        break;
      case CarContext.CAR_SERVICE:
        dispatchCall(appDispatcher, call, (ServiceT) carApp);
        break;
      default:
        mTemplateContext
            .getErrorHandler()
            .showError(
                CarAppError.builder(mAppName)
                    .setDebugMessage("No manager was found for type: " + managerType)
                    .build());
        break;
    }
  }

  /** Returns whether the app is currently bound to. */
  @AnyThread
  public boolean isBound() {
    return mBindingState == BindingState.BOUND;
  }

  /** Returns whether the binder is in unbound state. */
  @AnyThread
  @VisibleForTesting
  public boolean isUnbound() {
    return mBindingState == BindingState.UNBOUND;
  }

  /** Returns the {@link ServiceConnection} instance used by this binding. */
  @VisibleForTesting
  public ServiceConnection getServiceConnection() {
    return mServiceConnection;
  }

  /**
   * Unbinds from the app.
   *
   * <p>Will not set an error screen.
   *
   * <p>If already unbound the call will be a no-op.
   */
  @AnyThread
  public void unbind() {
    L.d(LogTags.APP_HOST, "Unbinding from %s", this);
    internalUnbind(null);
  }

  /**
   * Unbinds from the app and sets an error screen.
   *
   * <p>If already unbound the call will be a no-op.
   */
  private void unbind(CarAppError error) {
    L.d(LogTags.APP_HOST, "Unbinding from %s with error: %s", this, error);

    internalUnbind(error);
  }

  private void internalUnbind(@Nullable CarAppError errorToShow) {
    if (mBindingState != BindingState.UNBOUND) {
      // Run on main thread so that we can unregister from listening for surface changes on
      // the main thread before an error message is shown which could cause a onSurfaceChanged
      // callback.
      ThreadUtils.runOnMain(
          () -> {
            mOriginalIntent = null;
            setBindingState(BindingState.UNBOUND);
            if (errorToShow != null) {
              mTemplateContext.getErrorHandler().showError(errorToShow);
            }
            resetAppServices();
            mCarAppBindingCallback.onCarAppUnbound();
            // Perform tear down logic first, then actually unbind.
            mTemplateContext.getApplicationContext().unbindService(mServiceConnection);
          });
    }
  }

  private CarAppBinding(
      TemplateContext templateContext,
      ICarHost carHost,
      CarAppBindingCallback carAppBindingCallback) {
    mTemplateContext = templateContext;
    mAppName = templateContext.getCarAppPackageInfo().getComponentName();
    mCarHost = carHost;
    mCarAppBindingCallback = carAppBindingCallback;
    mTelemetryHandler = templateContext.getTelemetryHandler();
  }

  private void resetAppServices() {
    mCarApp = null;
    mAppManager = null;
    mNavigationManager = null;
  }

  private void setBindingState(BindingState bindingState) {
    if (mBindingState == bindingState) {
      return;
    }
    BindingState previousState = mBindingState;
    mBindingState = bindingState;
    L.d(
        LogTags.APP_HOST,
        "Binding state changed from %s to %s for %s",
        previousState,
        bindingState,
        mAppName.flattenToShortString());
  }

  /**
   * Retrieves a car service manager from the app
   *
   * @param appDispatcher the dispatcher used for making the getManager call
   * @param managerType one of the CarServiceType as defined in {@link CarContext}
   * @param carApp the car app to retrieve the manager from
   * @param callback the callback to trigger on receiving the result from the app
   */
  private void dispatchGetManager(
      AppDispatcher appDispatcher, String managerType, ICarApp carApp, Consumer<Object> callback) {
    appDispatcher.dispatch(
        anrToken ->
            carApp.getManager(
                managerType,
                new OnDoneCallbackStub(mTemplateContext, anrToken) {
                  @Override
                  public void onSuccess(@Nullable Bundleable response) {
                    super.onSuccess(checkNotNull(response));

                    try {
                      callback.accept(response.get());
                    } catch (BundlerException e) {
                      mTemplateContext
                          .getErrorHandler()
                          .showError(CarAppError.builder(mAppName).setCause(e).build());
                      return;
                    }
                  }
                }),
        CarAppApi.GET_MANAGER);
  }

  @SuppressWarnings("cast.unsafe") // Cannot check if instanceof ServiceT
  private static <ServiceT extends IInterface> void dispatchCall(
      AppDispatcher appDispatcher, NamedAppServiceCall<ServiceT> call, ServiceT serviceT) {
    appDispatcher.dispatch(anrToken -> call.dispatch(serviceT, anrToken), call.getCarAppApi());
  }

  private final class ServiceConnectionImpl implements ServiceConnection {
    private boolean mHasConnectedSinceLastBind;

    @Override
    public void onServiceConnected(ComponentName appName, IBinder service) {
      L.d(LogTags.APP_HOST, "App service connected: %s", appName.flattenToShortString());
      ANRToken token = mANRToken;
      if (token != null) {
        token.dismiss();
      }
      mHasConnectedSinceLastBind = true;

      resetAppServices();
      mCarApp = ICarApp.Stub.asInterface(service);
      dispatchGetAppInfo(mCarApp);
    }

    @Override
    public void onServiceDisconnected(ComponentName appName) {
      L.d(LogTags.APP_HOST, "App service disconnected: %s", appName.flattenToShortString());

      if (mHasConnectedSinceLastBind) {
        mHasConnectedSinceLastBind = false;
        setBindingState(BindingState.BINDING);
        resetAppServices();
        mTemplateContext.getEventManager().dispatchEvent(APP_DISCONNECTED);
      } else {
        unbind(
            CarAppError.builder(appName)
                .setDebugMessage("The app has crashed multiple times")
                .build());
      }
    }

    @Override
    public void onBindingDied(ComponentName appName) {
      L.d(LogTags.APP_HOST, "App binding died: %s", appName.flattenToShortString());

      mMainHandler.removeMessages(MSG_REBIND);

      setBindingState(BindingState.UNBOUND);
      resetAppServices();
      mTemplateContext.getEventManager().dispatchEvent(APP_DISCONNECTED);

      mMainHandler.sendMessageDelayed(mMainHandler.obtainMessage(MSG_REBIND), 500);
    }

    @Override
    public void onNullBinding(ComponentName name) {
      unbind(CarAppError.builder(mAppName).setDebugMessage("Null binding from app").build());
    }

    private void dispatchGetAppInfo(ICarApp carApp) {
      mTemplateContext
          .getAppDispatcher()
          .dispatch(
              anrToken -> sendAppInfoIPC(carApp, anrToken),
              CarAppBinding.this::unbind,
              CarAppApi.GET_APP_VERSION);
    }

    private void sendAppInfoIPC(ICarApp carApp, ANRToken anrToken) throws RemoteException {
      carApp.getAppInfo(
          new OnDoneCallbackStub(mTemplateContext, anrToken) {
            @Override
            public void onSuccess(@Nullable Bundleable response) {
              super.onSuccess(checkNotNull(response));
              CarHostConfig hostConfig = mTemplateContext.getCarHostConfig();
              try {
                AppInfo appInfo = (AppInfo) response.get();
                mTelemetryHandler.logCarAppTelemetry(
                    TelemetryEvent.newBuilder(UiAction.CLIENT_SDK_VERSION, mAppName)
                        .setCarAppSdkVersion(appInfo.getLibraryDisplayVersion()));
                dispatchOnHandshakeCompleted(carApp, hostConfig.updateNegotiatedApi(appInfo));
                mAppInfo = appInfo;

              } catch (BundlerException e) {
                unbind(CarAppError.builder(mAppName).setCause(e).build());
              } catch (IncompatibleApiException e) {
                unbind(
                    CarAppError.builder(mAppName)
                        .setType(CarAppError.Type.INCOMPATIBLE_CLIENT_VERSION)
                        .setCause(e)
                        .build());
              }
            }
          });
    }

    @SuppressWarnings("RestrictTo")
    private void dispatchOnHandshakeCompleted(ICarApp carApp, int negotiatedApiLevel) {
      HandshakeInfo handshakeInfo =
          new HandshakeInfo(mTemplateContext.getPackageName(), negotiatedApiLevel);
      mTemplateContext
          .getAppDispatcher()
          .dispatch(
              anrToken ->
                  carApp.onHandshakeCompleted(
                      Bundleable.create(handshakeInfo),
                      new OnDoneCallbackStub(mTemplateContext, anrToken) {
                        @Override
                        public void onSuccess(@Nullable Bundleable response) {
                          super.onSuccess(response);
                          dispatchOnAppCreate(carApp);
                        }
                      }),
              CarAppBinding.this::unbind,
              CarAppApi.ON_HANDSHAKE_COMPLETED);
    }

    private void dispatchOnAppCreate(ICarApp carApp) {
      mTemplateContext
          .getAppDispatcher()
          .dispatch(
              anrToken ->
                  carApp.onAppCreate(
                      mCarHost,
                      checkNotNull(mOriginalIntent),
                      mTemplateContext.getResources().getConfiguration(),
                      new OnDoneCallbackStub(mTemplateContext, anrToken) {
                        @Override
                        public void onSuccess(@Nullable Bundleable response) {
                          super.onSuccess(response);
                          setBindingState(BindingState.BOUND);
                          ThreadUtils.runOnMain(mCarAppBindingCallback::onCarAppBound);
                        }

                        @Override
                        public void onFailure(Bundleable failureResponse) {
                          super.onFailure(failureResponse);
                          L.d(LogTags.APP_HOST, "OnAppCreate Failure");
                          internalUnbind(null);
                        }
                      }),
              CarAppBinding.this::unbind,
              CarAppApi.ON_APP_CREATE);
    }
  }

  /** A {@link Handler.Callback} used to implement unbinding. */
  private class HandlerCallback implements Handler.Callback {
    @Override
    public boolean handleMessage(Message msg) {
      if (msg.what == MSG_UNBIND) {
        if (mTemplateContext.getCarAppPackageInfo().isNavigationApp()) {
          L.d(LogTags.APP_HOST, "Not unbinding due to the app being a navigation app");
          return true;
        }
        unbind();
        return true;
      } else if (msg.what == MSG_REBIND) {
        bind(new Intent().setComponent(mAppName));
        return true;
      }

      L.w(LogTags.APP_HOST, "Unknown message: %s", msg);
      return false;
    }
  }

  /** Updates the internal state and shows an error. */
  private void failedToBind(@Nullable Throwable cause) {
    // Set the state to unbound as the binding was unsuccessful.
    setBindingState(BindingState.UNBOUND);

    CarAppError.Builder builder =
        CarAppError.builder(mAppName).setDebugMessage("Failed to bind to " + mAppName);

    if (cause != null) {
      builder.setCause(cause);
    }

    mTemplateContext.getErrorHandler().showError(builder.build());
  }
}
