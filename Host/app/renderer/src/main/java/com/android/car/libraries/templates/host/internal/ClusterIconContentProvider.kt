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
package com.android.car.libraries.templates.host.internal

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.contentValuesOf
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction.HOST_FAILURE_CLUSTER_ICON
import com.android.car.libraries.templates.host.internal.ClusterIconContentProvider.Companion.addToCache
import com.android.car.libraries.templates.host.internal.ClusterIconContentProvider.Companion.queryIconData
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.android.car.libraries.templates.host.R

/**
 * A [ContentProvider] for providing navigation state icons to the Cluster.
 *
 * It uses an in-memory cache of [Bitmap]s that can be retrieved with a [Uri]. Use [addToCache] to
 * add a bitmap to the cache. Use [queryIconData] to check for the existence of a bitmap in cache
 * (note that this will refresh the validity of the entry).
 */
class ClusterIconContentProvider : ContentProvider() {
  private val scope = CoroutineScope(Dispatchers.IO)
  private lateinit var iconProviderDelegate: IconProviderDelegate

  override fun onCreate(): Boolean {
    val context = checkNotNull(context)

    val timeoutMillis =
      context.resources.getInteger(R.integer.cluster_icon_cache_duration_millis).toLong()

    val authority = authority(context)
    iconProviderDelegate = IconProviderDelegate(authority, timeoutMillis, scope)

    return true
  }

  override fun shutdown() {
    scope.cancel("ContentProvider shutting down")
    iconProviderDelegate.shutdown()
    super.shutdown()
  }

  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
    return iconProviderDelegate.openFile(uri)
  }

  /**
   * Get the uri and aspect ratio of an icon if it exists in cache. Prefer to use the convenience
   * method [queryIconData], instead of calling this directly.
   *
   * @param selection the iconId to look up
   * @return a [Cursor] with or 1 row if a match was found, or 0 rows otherwise
   */
  override fun query(
    uri: Uri,
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    sortOrder: String?
  ): Cursor {
    val cursor =
      MatrixCursor(
        arrayOf(QUERY_RESULT_CONTENT_URI, QUERY_RESULT_ASPECT_RATIO),
        /* initialCapacity */ 1
      )
    iconProviderDelegate.query(selection)?.let { (contentUri, aspectRatio) ->
      cursor.addRow(arrayOf(contentUri, aspectRatio))
    }

    return cursor
  }

  /**
   * Converts the provided [ByteArray] to a Bitmap and caches it, returning the URI path for this
   * icon. There are no stability guarantees for the keys / expected values, so prefer to use the
   * convenience method [addToCache], instead of calling this directly.
   */
  override fun insert(uri: Uri, values: ContentValues?): Uri? {
    if (values == null) return null
    val iconId = values.getAsString(INSERT_PARAM_ICON_ID) ?: return null
    val bytes = values.getAsByteArray(INSERT_PARAM_BITMAP_BYTES) ?: return null
    return iconProviderDelegate.cacheIcon(bytes, iconId)
  }

  override fun getType(uri: Uri): String? = null

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<String>?
  ): Int = 0

  companion object {

    private const val INSERT_PARAM_ICON_ID = "iconId"
    private const val INSERT_PARAM_BITMAP_BYTES = "data"

    private const val QUERY_RESULT_CONTENT_URI = "contentUri"
    private const val QUERY_RESULT_ASPECT_RATIO = "aspectRatio"

    /**
     * @return a uri and aspect ratio for the icon if it already exists in cache. [null] otherwise
     */
    fun queryIconData(iconId: String, context: Context): Pair<String, Double>? {
      context.contentResolver.query(contentUri(context), null, iconId, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val contentUriIndex = cursor.getColumnIndex(QUERY_RESULT_CONTENT_URI)
          val aspectRatioIndex = cursor.getColumnIndex(QUERY_RESULT_ASPECT_RATIO)
          if (contentUriIndex >= 0 && aspectRatioIndex >= 0) {
            val contentUri = cursor.getString(contentUriIndex)
            val aspectRatio = cursor.getDouble(aspectRatioIndex)
            return contentUri to aspectRatio
          } else {
            L.w(LogTags.CLUSTER) {
              "Icon for id $iconId exists, but failed to extract URI/aspectRatio"
            }
          }
        }
      }
      return null
    }

    /** Saves the bitmap to an in-memory cache, and returns a Uri that can be used to access it. */
    fun addToCache(iconId: String, bitmapBytes: ByteArray, context: Context): Uri? {
      return context.contentResolver.insert(
        contentUri(context),
        contentValuesOf(INSERT_PARAM_ICON_ID to iconId, INSERT_PARAM_BITMAP_BYTES to bitmapBytes)
      )
    }

    private fun contentUri(context: Context) = "content://${authority(context)}".toUri()

    /** Returns the provider's authority, as defined in the Manifest. */
    private fun authority(context: Context) = "${context.packageName}.ClusterIconContentProvider"
  }
}

/**
 * This class extracts most of the logic out of [ClusterIconContentProvider] so it can be more
 * easily tested.
 */
class IconProviderDelegate(
  private val authority: String,
  cacheTimeoutMillis: Long,
  private val coroutineScope: CoroutineScope
) {
  private var cache: Cache<String, Bitmap> =
    CacheBuilder.newBuilder().expireAfterAccess(cacheTimeoutMillis, TimeUnit.MILLISECONDS).build()

  private val uriMatcher =
    UriMatcher(UriMatcher.NO_MATCH).apply { addURI(authority, "img/*", URI_IMAGE_CODE) }

  fun cacheIcon(bytes: ByteArray, iconId: String): Uri {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val key = keyForIconId(iconId)

    cache.put(key, bitmap)

    return uriForKey(key)
  }

  /** @return a [Pair] with ContentUri and AspectRatio if a match was found, [null] otherwise */
  fun query(iconId: String?): Pair<Uri, Double>? {
    if (iconId == null) return null
    val key = keyForIconId(iconId)
    val bitmap = cache.getIfPresent(key) ?: return null

    val contentUri = uriForKey(key)
    val aspectRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
    return contentUri to aspectRatio
  }

  /** Returns a [ParcelFileDescriptor] that will be written to asynchronously */
  fun openFile(uri: Uri): ParcelFileDescriptor {
    return when (uriMatcher.match(uri)) {
      URI_IMAGE_CODE -> {
        val key =
          requireNotNull(uri.lastPathSegment) {
            "Cluster icon requested but no key provided. URI=$uri"
          }

        val bitmap =
          cache.getIfPresent(key)
            ?: run {
              LogUtil.log(HOST_FAILURE_CLUSTER_ICON)
              throw IllegalStateException("Requested cluster icon that's not in cache. (key=$key)")
            }
        val width = uri.getQueryParameter("w")?.toIntOrNull() ?: bitmap.width
        val height = uri.getQueryParameter("h")?.toIntOrNull() ?: bitmap.height
        // TODO(b/197754774): Cache scaled bitmaps
        val scaledBitmap =
          if (width != bitmap.width || height != bitmap.height) {
            bitmap.scale(width, height)
          } else {
            bitmap
          }

        // Use a pipe to avoid eagerly saving bitmaps to disk (or at all)
        val (readPipe, writePipe) = ParcelFileDescriptor.createReliablePipe()

        // asynchronously write bitmap to output stream
        writeToPipeAsync(writePipe, scaledBitmap)

        // Give the readPipe for cluster to consume the bitmap
        readPipe
      }
      else ->
        throw IllegalArgumentException("Requested a path that doesn't correspond to an icon: $uri")
    }
  }

  private fun writeToPipeAsync(writePipe: ParcelFileDescriptor, bitmap: Bitmap) =
    coroutineScope.launch {
      runCatching {
        L.d(LogTags.CLUSTER) { "Writing bitmap to pipe" }
        ParcelFileDescriptor.AutoCloseOutputStream(writePipe).use { outputStream ->
          bitmap.compress(Bitmap.CompressFormat.PNG, /* quality= */ 100, outputStream)
        }
      }
        .onFailure {
          L.e(LogTags.CLUSTER, it) { "IOException writing cluster icon to pipe" }
          writePipe.closeWithError("IOException writing to pipe")
        }
    }

  fun shutdown() {
    cache.invalidateAll()
  }

  private fun keyForIconId(iconId: String) = "cluster_icon_$iconId"

  private fun uriForKey(key: String) = "content://${authority}/img/$key".toUri()

  companion object {
    private const val URI_IMAGE_CODE = 1
  }
}
