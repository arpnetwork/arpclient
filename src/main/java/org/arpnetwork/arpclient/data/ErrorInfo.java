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

public class ErrorInfo {
    public static final int ERROR_NETWORK = -1;

    // socket connection errors
    public static final int ERROR_DISCONNECTED_BY_DEVICE = -2;

    // server errors
    public static final int ERROR_UNKNOWN = 100;
    public static final int ERROR_PARAM = 101;
    public static final int ERROR_NO_DEVICES= 102;
    public static final int ERROR_MEDIA = 103;

    //device protocol errors
    public static final int ERROR_PROTOCOL_TOUCH_SETTING = -10001;
    public static final int ERROR_PROTOCOL_VIDEO_INFO = -10002;
    public static final int ERROR_CONNECTION_RESULT = -10003;
    public static final int ERROR_CONNECTION_REFUSED_VERSION = -10004;

    public static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case ERROR_NETWORK:
                return "network error";

            case ERROR_DISCONNECTED_BY_DEVICE:
                return "disconnected by remote device";

            case ERROR_UNKNOWN:
            case ERROR_PARAM:
            case ERROR_NO_DEVICES:
                return "server rejected";

            case ERROR_MEDIA:
                return "media data error";

            case ERROR_PROTOCOL_TOUCH_SETTING:
            case ERROR_PROTOCOL_VIDEO_INFO:
            case ERROR_CONNECTION_RESULT:
                return "remote device error";

            case ERROR_CONNECTION_REFUSED_VERSION:
                return "incompatible version";

            default:
                return "unknown error";
        }
    }
}
