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

package org.arpnetwork.arpclient.protocol;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.arpnetwork.arpclient.data.ErrorCode;
import org.arpnetwork.arpclient.data.UserInfo;
import org.arpnetwork.arpclient.volley.GsonRequest;
import org.arpnetwork.arpclient.volley.VolleySingleton;
import org.json.JSONException;
import org.json.JSONObject;

public class ServerProtocol {
    private static final String HOST = "http://dev.arpnetwork.org:4040";

    public interface OnReceiveUserInfo {
        /**
         * Called when receive userInfo from server
         *
         * @param info userInfo
         */
        void onReceiveUserInfo(UserInfo info);
    }

    public interface OnServerProtocolError {
        /**
         * Called when error occurred
         *
         * @param code
         * @param msg
         */
        void onServerProtocolError(int code, String msg);
    }

    /**
     * Post http request to get userInfo from server
     *
     * @param context
     * @param condition         conditions to filter remote device needed
     * @param onReceiveUserInfo callback with userInfo
     * @param onError           callback with error
     */
    public static void getUserInfo(Context context, String condition,
            final OnReceiveUserInfo onReceiveUserInfo, final OnServerProtocolError onError) {
        String url = HOST + "/users";
        GsonRequest request = new GsonRequest<UserInfo>(Request.Method.POST, url, condition,
                UserInfo.class, new Response.Listener<UserInfo>() {
            @Override
            public void onResponse(UserInfo response) {
                onReceiveUserInfo.onReceiveUserInfo(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    try {
                        JSONObject errorResponse = new JSONObject(new String(error.networkResponse.data));
                        onError.onServerProtocolError(errorResponse.getInt("code"),
                                errorResponse.getString("message"));
                        return;
                    } catch (Exception e) {
                    }
                }
                onError.onServerProtocolError(ErrorCode.ERROR_NETWORK,
                        "network error");
            }
        });
        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }

    /**
     * Report connection state
     *
     * @param context
     * @param id      userInfo id
     * @param state   see {@link UserInfo}
     */
    public static void setConnectionState(Context context, String id, int state) {
        String url = HOST + "/users/" + id;
        JSONObject params = new JSONObject();
        try {
            params.put("state", state);
        } catch (JSONException e) {
        }
        GsonRequest request = new GsonRequest<Void>(Request.Method.PATCH, url, params.toString(),
                Void.class, new Response.Listener<Void>() {
            @Override
            public void onResponse(Void response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }
}
