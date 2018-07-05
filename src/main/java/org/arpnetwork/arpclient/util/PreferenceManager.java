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

package org.arpnetwork.arpclient.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String NAME = "ARP";

    private static PreferenceManager sInstance;

    private SharedPreferences mSharedPreferences;

    private PreferenceManager(Context context) {
        mSharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static PreferenceManager getInstance() {
        return sInstance;
    }

    public static void init(Context context) {
        sInstance = new PreferenceManager(context);
    }

    public static void fini() {
        sInstance = null;
    }

    public void putInt(String key, int value) {
        mSharedPreferences.edit().putInt(key, value).commit();
    }

    public void putString(String key, String value) {
        mSharedPreferences.edit().putString(key, value).commit();
    }

    public void putBoolean(String key, boolean value) {
        mSharedPreferences.edit().putBoolean(key, value).commit();
    }

    public void putLong(String key, long value) {
        mSharedPreferences.edit().putLong(key, value).commit();
    }

    public int getInt(String key) {
        return mSharedPreferences.getInt(key, -1);
    }

    public String getString(String key) {
        return mSharedPreferences.getString(key, "");
    }

    public boolean getBoolean(String key) {
        return mSharedPreferences.getBoolean(key, false);
    }

    public long getLong(String key) {
        return mSharedPreferences.getLong(key, 0);
    }
}
