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

package com.android.car.libraries.apphost.common;

import android.content.res.Resources;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

/** Handles event dispatch and subscription. */
public class EventManager {
  /** The type of events. */
  public enum EventType {
    /** Unknown event type */
    UNKNOWN,

    /** Signifies that the visible area of the view has changed. */
    SURFACE_VISIBLE_AREA,

    /** Signifies that the stable area of the view has changed. */
    SURFACE_STABLE_AREA,

    /**
     * Signifies that one of the descendants of the template view hierarchy has been interacted
     * with.
     */
    TEMPLATE_TOUCHED_OR_FOCUSED,

    /** Signifies that the focus state of the window that contains the template view has changed. */
    WINDOW_FOCUS_CHANGED,

    /** Signifies that the Car UX Restrictions constraints on the template view have changed. */
    CONSTRAINTS,

    /**
     * Signifies that the configuration of the view has changed.
     *
     * <p>The most up-to-date configuration can be retrieved via {@link
     * Resources#getConfiguration()}
     */
    CONFIGURATION_CHANGED,

    /** Signifies that the app is now unbound. */
    APP_UNBOUND,

    /** Signifies that the app has disconnected and will be rebound. */
    APP_DISCONNECTED,

    /**
     * Signifies that the current list of places has changed.
     *
     * <p>This is used by the PlaceListMapTemplate to synchronize places between the list and the
     * map views.
     */
    PLACE_LIST,

    /** Signifies that WindowInsets has changed. */
    WINDOW_INSETS,
  }

  // A weak-referenced map is used here so that subscribers do not have to explicitly unsubscribe
  // themselves.
  private final WeakHashMap<Object, List<Dependency>> mDependencyMap = new WeakHashMap<>();

  /**
   * Subscribes to an {@link EventType} and trigger the given {@link Runnable} when the event is
   * fired.
   *
   * <p>The input weakReference instance should be used to associate and clean up the {@link
   * Runnable} so that the event subscriber will automatically unsubscribe itself when the
   * weak-referenced object is GC'd. However, if earlier un-subscription is preferred, {@link
   * #unsubscribeEvent} can be called instead.
   */
  public void subscribeEvent(Object weakReference, EventType eventType, Runnable runnable) {
    List<Dependency> objectDependencies = mDependencyMap.get(weakReference);
    if (objectDependencies == null) {
      objectDependencies = new ArrayList<>();
      mDependencyMap.put(weakReference, objectDependencies);
    }
    objectDependencies.add(new Dependency(eventType, runnable));
  }

  /** Unsubscribes the given object (weakReference) to a certain {@link EventType}. */
  public void unsubscribeEvent(Object weakReference, EventType eventType) {
    List<Dependency> objectDependencies = mDependencyMap.get(weakReference);
    if (objectDependencies != null) {
      Iterator<Dependency> itr = objectDependencies.iterator();
      while (itr.hasNext()) {
        Dependency dependency = itr.next();
        if (dependency.mEventType == eventType) {
          itr.remove();
        }
      }
    }
  }

  /** Dispatches the given {@link EventType} so subscriber can react to it. */
  public void dispatchEvent(EventType eventType) {
    // TODO(b/163634344): Avoid creating a temp collection. This is needed to prevent concurrent
    // modifications that could happen if something subscribe to an event while
    // listening/handling
    // an existing event.
    Collection<List<Dependency>> dependencySet = new ArrayList<>(mDependencyMap.values());
    for (List<Dependency> dependencies : dependencySet) {
      for (Dependency dependency : dependencies) {
        if (dependency.mEventType == eventType) {
          dependency.mRunnable.run();
        }
      }
    }
  }

  /** An internal container for associating an {@link EventType} with a {@link Runnable}. */
  private static class Dependency {
    private final EventType mEventType;
    private final Runnable mRunnable;

    Dependency(EventType eventType, Runnable runnable) {
      mEventType = eventType;
      mRunnable = runnable;
    }
  }
}
