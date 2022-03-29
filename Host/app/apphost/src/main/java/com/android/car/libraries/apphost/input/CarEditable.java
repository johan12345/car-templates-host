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
package com.android.car.libraries.apphost.input;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/** Views that implement this interface are editable by the IME system. */
public interface CarEditable {
  /** Notifies that the input connection has been created. */
  InputConnection onCreateInputConnection(EditorInfo outAttrs);

  /** Sets a listener for events related to input on this car editable. */
  void setCarEditableListener(CarEditableListener listener);

  /** Sets whether input is enabled. */
  void setInputEnabled(boolean enabled);
}
