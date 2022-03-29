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
package com.android.car.libraries.apphost.lang;

import static com.android.car.libraries.apphost.logging.L.buildMessage;

import androidx.annotation.Nullable;
import androidx.core.util.Supplier;

/**
 * Utility methods for handling {@code null}able fields and methods.
 *
 * <p>These methods should be statically imported and <b>not</b> qualified by class name.
 */
public class NullUtils {
    /**
     * Returns a {@link Denullerator} with an initial value and type corresponding to the passed
     * parameter.
     */
    public static <T extends Object> Denullerator<T> ifNonNull(@Nullable T reference) {
        return new Denullerator<>(reference);
    }

    /**
     * A reference that can store {@code null} values but from which {@code null} values can never
     * be retrieved.
     *
     * <p>Note that the generic parameter must extend Object explicitly to ensure that the generic
     * type itself does not match something @Nullable. See
     * http://go/nullness_troubleshooting#issues-with-type-parameter-annotations
     *
     * @param <T> target class
     */
    public static class Denullerator<T extends Object> {
        @Nullable private T mReference;

        /**
         * New Denullerators should only be created using {@link NullUtils#ifNonNull(Object)} above.
         */
        private Denullerator(@Nullable T reference) {
            mReference = reference;
        }

        /** Returns a denullerator of a reference value. */
        public Denullerator<T> otherwiseIfNonNull(@Nullable T reference) {
            if (mReference == null) {
                mReference = reference;
            }
            return this;
        }

        /** Returns a denullerators of a reference value supplier. */
        public Denullerator<T> otherwiseIfNonNull(Supplier<@PolyNull T> referenceSupplier) {
            if (mReference == null) {
                mReference = referenceSupplier.get();
            }
            return this;
        }

        /** Return the value if it's not non-null. */
        public T otherwise(T reference) {
            return otherwiseIfNonNull(reference).otherwiseThrow();
        }

        /** Return the value if it's not non-null. */
        public T otherwise(Supplier<T> referenceSupplier) {
            return otherwiseIfNonNull(referenceSupplier).otherwiseThrow();
        }

        /** Returns an exception that values are not non-null */
        public T otherwiseThrow() {
            return otherwiseThrow("None of the supplied values were non-null!");
        }

        /** Returns the reference if it's not non-null. */
        public T otherwiseThrow(String msg, Object... msgArgs) {
            if (mReference == null) {
                throw new NullPointerException(buildMessage(msg, msgArgs));
            }
            return mReference;
        }
    }

    private NullUtils() {}
}
