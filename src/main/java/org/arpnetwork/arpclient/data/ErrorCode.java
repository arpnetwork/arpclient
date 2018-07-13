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

public class ErrorCode {
    public static final int ERROR_NETWORK = -1;

    // socket connection errors
    public static final int ERROR_DISCONNECTED_BY_DEVICE = -2;

    // server errors
    public static final int ERROR_UNKNOWN = 100;
    public static final int ERROR_PARAM = 101;
    public static final int ERROR_NO_DEVICES= 102;

    //device protocol errors
    public static final int ERROR_PROTOCOL_TOUCH_SETTING = -10001;
    public static final int ERROR_PROTOCOL_VIDEO_INFO = -10002;
    public static final int ERROR_CONNECTION_RESULT = -10003;
    public static final int ERROR_CONNECTION_REFUSED = -10004;
}
