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

import org.arpnetwork.arpclient.util.PreferenceManager;

/**
 * Quality of the video
 * The default quality is low quality
 */
public class Quality {
    private static final String KEY = "QUALITY";

    public static final int LOW = 1;
    public static final int HIGH = 2;

    public static void save(int quality) {
        PreferenceManager.getInstance().putInt(KEY, quality);
    }

    public static int get() {
        int quality = PreferenceManager.getInstance().getInt(KEY);
        if (quality == LOW || quality == HIGH) {
            return quality;
        }
        return LOW;
    }
}
