/*
 * Copyright 2018 ARP Network
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

package org.arpnetwork.arpclient.data;

import android.graphics.Rect;

public class VideoInfo {
    public int width; // video width
    public int height; // video height
    public int quality;

    public int resolutionWidth; // screen width of remote device
    public int resolutionHeight; // screen height of remote device
    public int statusBarHeight; // status bar height of remote device
    public int virtualBarHeight; // virtual bar height of remote device

    /**
     * Calculate surface view rect based on display area size
     *
     * @param displayWidth  display area width
     * @param displayHeight display area height
     * @return
     */
    public Rect getSurfaceViewRect(int displayWidth, int displayHeight) {
        double videoScale = width / (double) height;
        double deviceScreenScale = resolutionWidth / (double) resolutionHeight;

        double deviceScaleY = height / (double) resolutionHeight;
        double deviceScaleX = width / (double) resolutionWidth;

        double videoDisplayWidth = 0;
        double videoDisplayHeight = 0;

        double scaleStatusBarHeight = 0;
        double scaleVirtualBarHeight = 0;

        if (videoScale > deviceScreenScale) {
            // screen width scaled in video
            videoDisplayWidth = height * deviceScreenScale;
            videoDisplayHeight = height - (statusBarHeight + virtualBarHeight) * deviceScaleY;
            scaleStatusBarHeight = statusBarHeight * deviceScaleY;
            scaleVirtualBarHeight = virtualBarHeight * deviceScaleY;
        } else {
            // screen height scaled in video
            videoDisplayWidth = (double) width;
            videoDisplayHeight = (resolutionHeight - statusBarHeight - virtualBarHeight) * deviceScaleX;
            scaleStatusBarHeight = statusBarHeight * deviceScaleX;
            scaleVirtualBarHeight = virtualBarHeight * deviceScaleX;
        }

        // fix view size
        int viewWidth = (int) (displayWidth / videoDisplayWidth * width);
        int viewHeight = (int) (displayHeight / videoDisplayHeight * height);

        // fix view origin point based on show area
        double displayStatusBarHeight = (scaleStatusBarHeight * viewHeight / (double) height);
        double displayVirtualBarHeight = (scaleVirtualBarHeight * viewHeight / (double) height);
        int marginTop = (int) ((displayHeight + displayStatusBarHeight + displayVirtualBarHeight
                - viewHeight) / 2 - displayStatusBarHeight);
        int marginLeft = (displayWidth - viewWidth) / 2;

        return new Rect(marginLeft, marginTop, marginLeft + viewWidth, marginTop + viewHeight);
    }

    /**
     * Calculate display rect based on render view size
     *
     * @param viewWidth  render view width
     * @param viewHeight render view height
     * @return
     */
    public Rect getDisplayRect(int viewWidth, int viewHeight) {
        double videoScale = width / (double) height;
        double deviceScreenScale = resolutionWidth / (double) resolutionHeight;

        double deviceScaleY = height / (double) resolutionHeight;
        double deviceScaleX = width / (double) resolutionWidth;

        int videoWidth = viewWidth;
        int videoHeight = viewHeight;

        if (videoScale > deviceScreenScale) {
            videoWidth = (int) (videoWidth * deviceScaleY / deviceScaleX);
        } else {
            videoHeight = (int) (videoHeight * deviceScaleX / deviceScaleY);
        }

        int marginLeft = (viewWidth - videoWidth) / 2;
        int marginTop = (viewHeight - videoHeight) / 2;

        return new Rect(marginLeft, marginTop, marginLeft + videoWidth, marginTop + videoHeight);
    }
}
