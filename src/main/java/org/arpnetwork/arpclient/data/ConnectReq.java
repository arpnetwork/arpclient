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

public class ConnectReq extends ProtocolPacket<ConnectData> {
    private static final int ID = 1;

    public ConnectReq(String sessionId, String packageName) {
        super(ID);

        data = new ConnectData();
        data.session = sessionId;
        data.packageName = packageName;
        data.quality = Quality.get();
    }
}
