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

import android.graphics.Point;
import android.util.Size;

public class TouchSetting {
    private static final int DEFAULT_PRESSURE = 50;
    private static final int DEFAULT_MAJOR = 5;
    private static final int DEFAULT_MINOR = 5;

    private int contacts;
    private int x;
    private int y;
    private int pressure;
    private int major;
    private int minor;

    private Size mScreenSize;

    public void setScreenSize(Size size) {
        mScreenSize = size;
    }

    /**
     * Get transformed point for touch command,
     * affected by screen touch event, orientation and screen size.
     *
     * @param originX     touch event X
     * @param originY     touch event Y
     * @param isLandscape screen orientation, true for landscape
     * @return
     */
    public Point getTransformedPoint(float originX, float originY, boolean isLandscape) {
        int transformedX = (int) (originX / getPercentX());
        int transformedY = (int) (originY / getPercentY());
        if (isLandscape) {
            transformedX = (int) ((getScreenHeight() - originY) / getPercentY());
            transformedY = (int) (originX / getPercentX());
        }
        return new Point(transformedX, transformedY);
    }

    /**
     * Get transformed value of pressure for touch command,
     * affected by screen touch event.
     *
     * @param eventPressure
     * @return
     */
    public int getTransformedPressure(float eventPressure) {
        int result = 0;

        if (pressure != 0) {
            if (eventPressure == 1.0f) {
                result = DEFAULT_PRESSURE;
            } else {
                result = (int) (eventPressure * pressure);
            }
        }

        return result;
    }

    /**
     * Get transformed value of touch major for touch command,
     * affected by screen touch event.
     *
     * @param eventTouchMajor
     * @return
     */
    public int getTransformedTouchMajor(float eventTouchMajor) {
        int result = 0;

        if (major != 0) {
            if (eventTouchMajor == 0.0f) {
                result = DEFAULT_MAJOR;
            } else {
                result = Math.min((int) eventTouchMajor, major);
            }
        }

        return result;
    }

    /**
     * Get transformed value of touch minor for touch command,
     * affected by screen touch event.
     *
     * @param eventTouchMinor
     * @return
     */
    public int getTransformedTouchMinor(float eventTouchMinor) {
        int result = 0;

        if (minor != 0) {
            if (eventTouchMinor == 0) {
                result = DEFAULT_MINOR;
            } else {
                result = Math.min((int) eventTouchMinor, minor);
            }
        }

        return result;
    }

    private double getPercentX() {
        return (double) getScreenWidth() / x;
    }

    private double getPercentY() {
        return (double) getScreenHeight() / y;
    }

    private int getScreenWidth() {
        return mScreenSize.getWidth();
    }

    private int getScreenHeight() {
        return mScreenSize.getHeight();
    }

    @Override
    public String toString() {
        return "TouchSetting [contacts=" + contacts + ", x=" + x + ", y=" + y + ", pressure="
                + pressure + ", major=" + major + ", minor=" + minor + ", screen width:"
                + getScreenWidth() + ", screen height:" + getScreenHeight() + "]";
    }
}
