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

import android.content.Context;
import android.view.MotionEvent;

import org.arpnetwork.arpclient.data.TouchSetting;

import java.util.Arrays;
import java.util.Locale;

public class TouchHandler {
    /**
     * A null/invalid pointer ID.
     */
    public static final int INVALID_POINTER = -1;

    // Last known position/pointer tracking
    private int mActivePointerId = INVALID_POINTER;
    private float[] mInitialMotionX;
    private float[] mInitialMotionY;
    private float[] mLastMotionX;
    private float[] mLastMotionY;
    private int[] mLastPointerId;

    private boolean mLandscape;

    private TouchSetting mTouchSetting;
    private Context mContext;

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
     * @param context
     */
    public void setTouchSetting(TouchSetting touchSetting, Context context) {
        mContext = context;
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
        if (mTouchSetting == null) {
            return false;
        }

        final int action = ev.getActionMasked();
        final int actionIndex = ev.getActionIndex();

        if (action == MotionEvent.ACTION_DOWN) {
            // Reset things for a new event stream, just in case we didn't get
            // the whole previous stream.
            cancel();
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                final int pointerId = ev.getPointerId(0);

                clearStringBuilder();

                mBuilder.append(String.format(Locale.US, "d %d %d %d %d %d %d \n", pointerId,
                        mTouchSetting.getTransformedPoint(x, y, mContext, mLandscape).x,
                        mTouchSetting.getTransformedPoint(x, y, mContext, mLandscape).y,
                        getPressure(ev, actionIndex), getTouchMajor(ev, 0), getTouchMinor(ev, 0)));
                mBuilder.append("c\n");

                saveInitialMotion(x, y, pointerId);
                mActivePointerId = pointerId;

                mListener.onTouchInfo(mBuilder.toString());
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                final int pointerId = ev.getPointerId(actionIndex);
                final float x = ev.getX(actionIndex);
                final float y = ev.getY(actionIndex);

                clearStringBuilder();

                mBuilder.append(String.format(Locale.US, "d %d %d %d %d %d %d \n", pointerId,
                        mTouchSetting.getTransformedPoint(x, y, mContext, mLandscape).x,
                        mTouchSetting.getTransformedPoint(x, y, mContext, mLandscape).y,
                        getPressure(ev, actionIndex), getTouchMajor(ev, actionIndex),
                        getTouchMinor(ev, actionIndex)));

                mBuilder.append("c\n");

                saveInitialMotion(x, y, pointerId);
                mLastPointerId[pointerId] = pointerId;

                mListener.onTouchInfo(mBuilder.toString());
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                clearStringBuilder();

                final int pointerCount = ev.getPointerCount();
                for (int i = 0; i < pointerCount; i++) {
                    final int id = ev.getPointerId(i);
                    final float x = ev.getX(i);
                    final float y = ev.getY(i);

                    mBuilder.append(String.format(Locale.US, "m %d %d %d %d %d %d \n", id,
                            mTouchSetting.getTransformedPoint(x, y, mContext, mLandscape).x,
                            mTouchSetting.getTransformedPoint(x, y, mContext, mLandscape).y,
                            getPressure(ev, actionIndex), getTouchMajor(ev, i),
                            getTouchMinor(ev, i)));
                    mBuilder.append("c\n");
                }

                mListener.onTouchInfo(mBuilder.toString());
                saveLastMotion(ev);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                clearStringBuilder();

                final int pointerId = ev.getPointerId(actionIndex);
                for (int i = 0; i < mLastPointerId.length; i++) {
                    if (i != mActivePointerId) {
                        mBuilder.append(String.format(Locale.US, "u %d \n", i));
                        mBuilder.append("c\n");
                    }
                }

                mListener.onTouchInfo(mBuilder.toString());
                clearMotionHistory(pointerId);
                break;
            }

            case MotionEvent.ACTION_UP: {
                clearStringBuilder();
                appendUpString();
                mListener.onTouchInfo(mBuilder.toString());

                cancel();
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                cancel();
                break;
            }
        }
        return true;
    }

    private void clearStringBuilder() {
        mBuilder.setLength(0);
    }

    private void appendUpString() {
        if (mActivePointerId != INVALID_POINTER) {
            mBuilder.append(String.format(Locale.US, "u %d \n", mActivePointerId));
            mBuilder.append("c\n");
        }
    }

    private void cancel() {
        clearStringBuilder();

        mActivePointerId = INVALID_POINTER;
        clearMotionHistory();
    }

    private void clearMotionHistory() {
        if (mInitialMotionX == null) {
            return;
        }
        Arrays.fill(mInitialMotionX, 0);
        Arrays.fill(mInitialMotionY, 0);
        Arrays.fill(mLastMotionX, 0);
        Arrays.fill(mLastMotionY, 0);

        Arrays.fill(mLastPointerId, 0);
    }

    private void clearMotionHistory(int pointerId) {
        if (mInitialMotionX == null) {
            return;
        }
        mInitialMotionX[pointerId] = 0;
        mInitialMotionY[pointerId] = 0;
        mLastMotionX[pointerId] = 0;
        mLastMotionY[pointerId] = 0;

        mLastPointerId[pointerId] = 0;
    }

    private void ensureMotionHistorySizeForId(int pointerId) {
        if (mInitialMotionX == null || mInitialMotionX.length <= pointerId) {
            float[] imx = new float[pointerId + 1];
            float[] imy = new float[pointerId + 1];
            float[] lmx = new float[pointerId + 1];
            float[] lmy = new float[pointerId + 1];
            int[] iit = new int[pointerId + 1];

            if (mInitialMotionX != null) {
                System.arraycopy(mInitialMotionX, 0, imx, 0, mInitialMotionX.length);
                System.arraycopy(mInitialMotionY, 0, imy, 0, mInitialMotionY.length);
                System.arraycopy(mLastMotionX, 0, lmx, 0, mLastMotionX.length);
                System.arraycopy(mLastMotionY, 0, lmy, 0, mLastMotionY.length);

                System.arraycopy(mLastPointerId, 0, iit, 0, mLastPointerId.length);
            }

            mInitialMotionX = imx;
            mInitialMotionY = imy;
            mLastMotionX = lmx;
            mLastMotionY = lmy;

            mLastPointerId = iit;
        }
    }

    private void saveInitialMotion(float x, float y, int pointerId) {
        ensureMotionHistorySizeForId(pointerId);
        mInitialMotionX[pointerId] = mLastMotionX[pointerId] = x;
        mInitialMotionY[pointerId] = mLastMotionY[pointerId] = y;
    }

    private void saveLastMotion(MotionEvent ev) {
        final int pointerCount = ev.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            final int pointerId = ev.getPointerId(i);
            final float x = ev.getX(i);
            final float y = ev.getY(i);
            mLastMotionX[pointerId] = x;
            mLastMotionY[pointerId] = y;

            mLastPointerId[pointerId] = pointerId;
        }
    }

    private int getPressure(MotionEvent ev, int pointerIndex) {
        int pressure = mTouchSetting.getPressure();
        float evPressure = ev.getPressure(pointerIndex);
        int result = 0;

        if (pressure != 0) {
            if (evPressure == 1) {
                result = 50;
            } else {
                result = (int) (evPressure * pressure);
            }
        }

        return result;
    }

    private int getTouchMajor(MotionEvent ev, int pointerIndex) {
        int touchMajor = mTouchSetting.getMajor();
        int evTouchMajor = (int) ev.getTouchMajor(pointerIndex);
        int result = 0;

        if (touchMajor != 0) {
            if (evTouchMajor == 0) {
                result = 5;
            } else {
                result = Math.min(evTouchMajor, touchMajor);
            }
        }

        return result;
    }

    private int getTouchMinor(MotionEvent ev, int id) {
        int touchMinor = mTouchSetting.getMinor();
        int evTouchMinor = (int) ev.getTouchMinor(id);
        int result = 0;

        if (touchMinor != 0) {
            if (evTouchMinor == 0) {
                result = 5;
            } else {
                result = Math.min(evTouchMinor, touchMinor);
            }
        }

        return result;
    }
}
