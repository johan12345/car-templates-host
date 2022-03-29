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
package com.android.car.libraries.apphost.logging;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Helper class for logging. */
public final class L {
  private static final String STRING_MEANING_NULL = "null";

  /** Builds a log message from a message format string and its arguments. */
  public static String buildMessage(@Nullable String message, @Nullable Object... args) {
    // If the message is null, ignore the args and return "null";
    if (message == null) {
      return STRING_MEANING_NULL;
    }

    // else if the args are null or 0-length, return message
    if (args == null || args.length == 0) {
      try {
        return String.format(Locale.US, message);
      } catch (IllegalFormatException ex) {
        return message;
      }
    }

    // Use deepToString to get a more useful representation of any arrays in args
    for (int i = 0; i < args.length; i++) {
      if (args[i] != null && args[i].getClass().isArray()) {
        // Wrap in an array, deepToString, then remove the extra [] from the wrapper. This
        // allows handling all array types rather than having separate branches for all
        // primitive array types plus Object[].
        String string = Arrays.deepToString(new Object[] {args[i]});
        // Strip the outer [] from the wrapper array.
        args[i] = string.substring(1, string.length() - 1);
      }
    }

    // else try formatting the string.
    try {
      return String.format(Locale.US, message, args);
    } catch (IllegalFormatException ex) {
      return message + Arrays.deepToString(args);
    }
  }

  /**
   * Log a verbose message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   */
  @FormatMethod
  public static void v(String tag, @NonNull @FormatString String message) {
    if (Log.isLoggable(tag, Log.VERBOSE)) {
      Log.v(tag, message);
    }
  }

  /**
   * Log a verbose message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message a supplier of the message to log. This will only be executed if the log level
   *     for the given tag is enabled.
   */
  public static void v(String tag, @NonNull Supplier<String> message) {
    if (Log.isLoggable(tag, Log.VERBOSE)) {
      Log.v(tag, message.get());
    }
  }

  /**
   * Log a verbose message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   * @param args the formatting args for the previous string.
   */
  @FormatMethod
  public static void v(
      String tag, @NonNull @FormatString String message, @Nullable Object... args) {
    if (Log.isLoggable(tag, Log.VERBOSE)) {
      Log.v(tag, buildMessage(message, args));
    }
  }

  /**
   * Log a verbose message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   */
  @FormatMethod
  public static void v(String tag, @Nullable Throwable th, @NonNull @FormatString String message) {
    if (Log.isLoggable(tag, Log.VERBOSE)) {
      Log.v(tag, message, th);
    }
  }

  /**
   * Log a verbose message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   * @param args the formatting args for the previous string.
   */
  @FormatMethod
  public static void v(
      String tag,
      @Nullable Throwable th,
      @NonNull @FormatString String message,
      @Nullable Object... args) {
    if (Log.isLoggable(tag, Log.VERBOSE)) {
      Log.v(tag, buildMessage(message, args), th);
    }
  }

  /**
   * Log a debug message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   */
  @FormatMethod
  public static void d(String tag, @NonNull @FormatString String message) {
    if (Log.isLoggable(tag, Log.DEBUG)) {
      Log.d(tag, message);
    }
  }

  /**
   * Log a debug message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message a supplier of the message to log. This will only be executed if the log level
   *     for the given tag is enabled.
   */
  public static void d(String tag, @NonNull Supplier<String> message) {
    if (Log.isLoggable(tag, Log.DEBUG)) {
      Log.d(tag, message.get());
    }
  }

  /**
   * Log a debug message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   * @param args the formatting args for the previous string.
   */
  @FormatMethod
  public static void d(
      String tag, @NonNull @FormatString String message, @Nullable Object... args) {
    if (Log.isLoggable(tag, Log.DEBUG)) {
      Log.d(tag, buildMessage(message, args));
    }
  }

  /**
   * Log a debug message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param th a throwable to log
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   */
  @FormatMethod
  public static void d(String tag, @Nullable Throwable th, @NonNull @FormatString String message) {

    if (Log.isLoggable(tag, Log.DEBUG)) {
      Log.d(tag, message, th);
    }
  }

  /**
   * Log a debug message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param th a throwable to log
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   * @param args the formatting args for the previous string.
   */
  @FormatMethod
  public static void d(
      String tag,
      @Nullable Throwable th,
      @NonNull @FormatString String message,
      @Nullable Object... args) {
    if (Log.isLoggable(tag, Log.DEBUG)) {
      Log.d(tag, buildMessage(message, args), th);
    }
  }

  /**
   * Log an info message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   */
  @FormatMethod
  public static void i(String tag, @NonNull @FormatString String message) {

    if (Log.isLoggable(tag, Log.INFO)) {
      Log.i(tag, message);
    }
  }

  /**
   * Log an info message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   * @param args the formatting args for the previous string.
   */
  @FormatMethod
  public static void i(
      String tag, @NonNull @FormatString String message, @Nullable Object... args) {
    if (Log.isLoggable(tag, Log.INFO)) {
      Log.i(tag, buildMessage(message, args));
    }
  }

  /**
   * Log an info message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message a supplier of the message to log. This will only be executed if the log level
   *     for the given tag is enabled.
   */
  public static void i(String tag, @NonNull Supplier<String> message) {
    if (Log.isLoggable(tag, Log.INFO)) {
      Log.i(tag, message.get());
    }
  }

  /**
   * Log an info message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param th a throwable to log
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   */
  @FormatMethod
  public static void i(String tag, @Nullable Throwable th, @NonNull @FormatString String message) {
    if (Log.isLoggable(tag, Log.INFO)) {
      Log.i(tag, message, th);
    }
  }

  /**
   * Log an info message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param th a throwable to log
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   * @param args the formatting args for the previous string.
   */
  @FormatMethod
  public static void i(
      String tag,
      @Nullable Throwable th,
      @NonNull @FormatString String message,
      @Nullable Object... args) {
    if (Log.isLoggable(tag, Log.INFO)) {
      Log.i(tag, buildMessage(message, args), th);
    }
  }

  /**
   * Log a warning message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   */
  @FormatMethod
  public static void w(String tag, @NonNull @FormatString String message) {
    if (Log.isLoggable(tag, Log.WARN)) {
      Log.w(tag, message);
    }
  }

  /**
   * Log a warning message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message a supplier of the message to log. This will only be executed if the log level
   *     for the given tag is enabled.
   */
  public static void w(String tag, @NonNull Supplier<String> message) {
    if (Log.isLoggable(tag, Log.WARN)) {
      Log.w(tag, message.get());
    }
  }

  /**
   * Log a warning message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param th a throwable to log.
   * @param message a supplier of the message to log. This will only be executed if the log level
   *     for the given tag is enabled.
   */
  public static void w(String tag, @Nullable Throwable th, @NonNull Supplier<String> message) {
    if (Log.isLoggable(tag, Log.WARN)) {
      Log.w(tag, message.get(), th);
    }
  }

  /**
   * Log a warning message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   * @param args the formatting args for the previous string.
   */
  @FormatMethod
  public static void w(
      String tag, @NonNull @FormatString String message, @Nullable Object... args) {
    if (Log.isLoggable(tag, Log.WARN)) {
      Log.w(tag, buildMessage(message, args));
    }
  }

  /**
   * Log a warning message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param th a throwable to log.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   */
  @FormatMethod
  public static void w(String tag, @Nullable Throwable th, @NonNull @FormatString String message) {
    if (Log.isLoggable(tag, Log.WARN)) {
      Log.w(tag, message, th);
    }
  }

  /**
   * Log a warning message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param th a throwable to log.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   * @param args the formatting args for the previous string.
   */
  @FormatMethod
  public static void w(
      String tag,
      @Nullable Throwable th,
      @NonNull @FormatString String message,
      @Nullable Object... args) {
    if (Log.isLoggable(tag, Log.WARN)) {
      Log.w(tag, buildMessage(message, args), th);
    }
  }

  /**
   * Log an error message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   */
  @FormatMethod
  public static void e(String tag, @NonNull @FormatString String message) {
    if (Log.isLoggable(tag, Log.ERROR)) {
      Log.e(tag, message);
    }
  }

  /**
   * Log an error message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   * @param args the formatting args for the previous string.
   */
  @FormatMethod
  public static void e(
      String tag, @NonNull @FormatString String message, @Nullable Object... args) {
    if (Log.isLoggable(tag, Log.ERROR)) {
      Log.e(tag, buildMessage(message, args));
    }
  }

  /**
   * Log an error message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param th a throwable to log.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   */
  @FormatMethod
  public static void e(String tag, @Nullable Throwable th, @NonNull @FormatString String message) {
    if (Log.isLoggable(tag, Log.ERROR)) {
      Log.e(tag, message, th);
    }
  }

  /**
   * Log an error message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param message a supplier of the message to log. This will only be executed if the log level
   *     for the given tag is enabled.
   */
  public static void e(String tag, @NonNull Supplier<String> message) {
    if (Log.isLoggable(tag, Log.ERROR)) {
      Log.e(tag, message.get());
    }
  }

  /**
   * Log an error message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param th a throwable to log.
   * @param message a supplier of the message to log. This will only be executed if the log level
   *     for the given tag is enabled.
   */
  public static void e(String tag, @Nullable Throwable th, @NonNull Supplier<String> message) {
    if (Log.isLoggable(tag, Log.ERROR)) {
      Log.e(tag, message.get(), th);
    }
  }

  /**
   * Log an error message.
   *
   * @param tag the tag shouldn't be more than 23 characters as {@link Log#isLoggable(String, int)}
   *     has this restriction.
   * @param th a throwable to log.
   * @param message the string message to log. This can also be a string format that's recognized by
   *     {@link String#format(String, Object...)}. e.g. "%s did something to %s, and %d happened as
   *     a result".
   * @param args the formatting args for the previous string.
   */
  @FormatMethod
  public static void e(
      String tag,
      @Nullable Throwable th,
      @NonNull @FormatString String message,
      @Nullable Object... args) {
    if (Log.isLoggable(tag, Log.ERROR)) {
      Log.e(tag, buildMessage(message, args), th);
    }
  }
}
