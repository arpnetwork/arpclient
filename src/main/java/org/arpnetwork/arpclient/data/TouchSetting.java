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

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class TouchSetting {
    int contacts;
    int x;
    int y;
    int pressure;
    int major;
    int minor;

    double PercentX;
    double PercentY;

    public int getPressure() {
        return pressure;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    /**
     * Get transformed point for touch command,
     * affected by screen touch event and orientation.
     *
     * @param originX     touch event X
     * @param originY     touch event Y
     * @param context
     * @param isLandscape screen orientation, true for landscape
     * @return
     */
    public Point getTransformedPoint(float originX, float originY, Context context, boolean isLandscape) {
        int transformedX = 0;
        int transformedY = 0;
        if (isLandscape) {
            transformedX = (int) ((getScreenHeight(context) - originY) / getPercentY(context));
            transformedY = (int) (originX / getPercentX(context));
        } else {
            transformedX = (int) (originX / getPercentX(context));
            transformedY = (int) (originY / getPercentY(context));
        }
        return new Point(transformedX, transformedY);
    }

    private double getPercentX(Context context) {
        if (x == 0) {
            x = 32767;
        }
        int width = getScreenWidth(context);
        return (double) width / x;
    }

    private double getPercentY(Context context) {
        if (y == 0) {
            y = 32767;
        }
        int height = getScreenHeight(context);
        return (double) height / y;
    }

    private static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    private static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    @Override
    public String toString() {
        return "TouchSetting [contacts=" + contacts + ", x=" + x + ", y=" + y + ", pressure="
                + pressure + ", major=" + major + ", minor=" + minor
                + ", PercentX=" + PercentX + ", PercentY=" + PercentY + "]";
    }
}
