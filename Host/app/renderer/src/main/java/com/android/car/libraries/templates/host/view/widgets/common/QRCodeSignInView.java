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
package com.android.car.libraries.templates.host.view.widgets.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.car.app.model.signin.QRCodeSignInMethod;
import com.android.car.libraries.apphost.common.CarAppError;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.templates.host.R;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A view that displays {@link QRCodeSignInMethod} UI. */
public class QRCodeSignInView extends FrameLayout {
  private ImageView mQRCodeView;

  public QRCodeSignInView(Context context) {
    this(context, null);
  }

  public QRCodeSignInView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public QRCodeSignInView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  @SuppressWarnings("ResourceType")
  public QRCodeSignInView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    mQRCodeView = findViewById(R.id.qr_code_view);
  }

  /** Sets the qr code. */
  public void setQRCodeSignInMethod(
      TemplateContext templateContext, QRCodeSignInMethod qrCodeSignInMethod) {
    setQRCode(templateContext, qrCodeSignInMethod.getUri().toString());
  }

  private void setQRCode(TemplateContext templateContext, String url) {
    QRCode qrCode;
    try {
      qrCode = Encoder.encode(url, ErrorCorrectionLevel.H);
    } catch (WriterException e) {
      templateContext
          .getErrorHandler()
          .showError(
              CarAppError.builder(templateContext.getCarAppPackageInfo().getComponentName())
                  .setCause(e)
                  .build());
      return;
    }

    BitmapDrawable drawable = new BitmapDrawable(getResources(), qrToBitmap(qrCode));
    drawable.setAntiAlias(false);
    drawable.setFilterBitmap(false);
    mQRCodeView.setImageDrawable(drawable);
  }

  private Bitmap qrToBitmap(QRCode qrCode) {
    ByteMatrix matrix = qrCode.getMatrix();
    int width = matrix.getWidth();
    int height = matrix.getHeight();
    int[] colors = new int[width * height];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        colors[y * width + x] = (matrix.get(x, y) != 0) ? Color.WHITE : Color.TRANSPARENT;
      }
    }

    return Bitmap.createBitmap(colors, 0, width, width, height, Bitmap.Config.ALPHA_8);
  }
}
