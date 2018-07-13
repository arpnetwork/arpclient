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

public class UserInfo {
    public static final int STATE_NO_CONNECTION = 0;
    public static final int STATE_CONNECTED = 1;
    public static final int STATE_DISCONNECTED = 2;
    public static final int STATE_CONNECT_FAIL = -1;
    public static final int STATE_DISCONNECT_ILLEGAL = -2;

    public String id;
    public String session;
    public int state;
    public Device device;

    @Override
    public String toString() {
        return "UserInfo [id=" + id + ", session=" + session + ", state=" + state + ", device="
                + device.toString() + "]";
    }
}

