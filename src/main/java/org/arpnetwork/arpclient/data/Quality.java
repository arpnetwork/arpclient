package org.arpnetwork.arpclient.data;

import org.arpnetwork.arpclient.util.PreferenceManager;

/**
 * Quality of the video
 * The default quality is low quality
 */
public class Quality {
    private static final String KEY_QUALITY = "KEY_QUALITY";

    public static final int LOW_QUALITY = 1;
    public static final int HIGH_QUALITY = 2;

    public static void saveQuality(int quality) {
        PreferenceManager.getInstance().putInt(KEY_QUALITY, quality);
    }

    public static int getQuality() {
        int quality = PreferenceManager.getInstance().getInt(KEY_QUALITY);
        if (quality == LOW_QUALITY || quality == HIGH_QUALITY) {
            return quality;
        }
        return LOW_QUALITY;
    }
}
