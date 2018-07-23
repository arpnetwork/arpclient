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

package org.arpnetwork.arpclient.touch;

import android.view.MotionEvent;

import org.arpnetwork.arpclient.data.TouchSetting;

import java.util.Locale;

public class TouchHandler {
    private boolean mLandscape;

    private TouchSetting mTouchSetting;

    private OnTouchInfoListener mListener;

    private StringBuilder mBuilder = new StringBuilder();

    public interface OnTouchInfoListener {
        void onTouchInfo(String touchInfo);
    }

    public TouchHandler(OnTouchInfoListener listener) {
        mListener = listener;
    }

    /**
     * Set remote device touch setting for transform.
     *
     * @param touchSetting
     */
    public void setTouchSetting(TouchSetting touchSetting) {
        mTouchSetting = touchSetting;
    }

    /**
     * Set screen orientation for touch event transform.
     *
     * @param landscape
     */
    public void setLandscape(boolean landscape) {
        mLandscape = landscape;
    }

    /**
     * Transform touch event into commands for remote device.
     *
     * @param ev
     * @return
     */
    public boolean onTouchEvent(MotionEvent ev) {
        if (mTouchSetting == null || !mTouchSetting.isEnabled()) {
            return false;
        }

        final int action = ev.getActionMasked();

        final int actionIndex = ev.getActionIndex();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                final int pointerId = ev.getPointerId(0);

                clearStringBuilder();
                appendTouchInfoString(pointerId, actionIndex, "d", x, y, ev);

                mListener.onTouchInfo(mBuilder.toString());
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                final int pointerId = ev.getPointerId(actionIndex);
                final float x = ev.getX(actionIndex);
                final float y = ev.getY(actionIndex);

                clearStringBuilder();
                appendTouchInfoString(pointerId, actionIndex, "d", x, y, ev);

                mListener.onTouchInfo(mBuilder.toString());
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerCount = ev.getPointerCount();

                clearStringBuilder();
                for (int i = 0; i < pointerCount; i++) {
                    final float x = ev.getX(i);
                    final float y = ev.getY(i);

                    appendTouchInfoString(ev.getPointerId(i), actionIndex, "m", x, y, ev);
                }

                mListener.onTouchInfo(mBuilder.toString());
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                clearStringBuilder();
                appendUpString(ev.getPointerId(actionIndex));

                mListener.onTouchInfo(mBuilder.toString());
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                clearStringBuilder();
                appendUpString(ev.getPointerId(0));

                mListener.onTouchInfo(mBuilder.toString());
                break;
            }
        }
        return true;
    }

    private void clearStringBuilder() {
        mBuilder.setLength(0);
    }

    private void appendTouchInfoString(int id, int index, String type, float x, float y, MotionEvent ev) {
        mBuilder.append(String.format(Locale.US, "%s %d %d %d %d %d %d \n", type, id,
                mTouchSetting.getTransformedPoint(x, y, mLandscape).x,
                mTouchSetting.getTransformedPoint(x, y, mLandscape).y,
                mTouchSetting.getTransformedPressure(ev.getPressure(index)),
                mTouchSetting.getTransformedTouchMajor(ev.getTouchMajor(0)),
                mTouchSetting.getTransformedTouchMinor(ev.getTouchMinor(0))));
        mBuilder.append("c\n");
    }

    private void appendUpString(int id) {
        mBuilder.append(String.format(Locale.US, "u %d \n", id));
        mBuilder.append("c\n");
    }
}
