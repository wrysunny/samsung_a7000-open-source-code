package com.sec.android.app.dns.radiovis.http;

import org.json.JSONException;
import org.json.JSONObject;
import com.sec.android.app.dns.LogDns;

public class RadioVISHttpParser {

    private static final String TAG = "RadioVISHttpParser";

    // all function was overloaded.
    public static String getRadioVisFrameBody(final JSONObject visFrame) {
        String visBody = null;
        String bodyContent = null;
        try {
            visBody = visFrame.getString(RadioVISHttpProtocol.HTTP_BODY);
            if (visBody.contains(RadioVISHttpProtocol.SHOW)
                    || visBody.contains(RadioVISHttpProtocol.TEXT)) {
                bodyContent = visBody.substring(5, visBody.length());
            }
            LogDns.v(TAG, "body" + bodyContent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return bodyContent;
    }

    public static String getRadioVisFrameTriggerTime(JSONObject visFrame) {
        JSONObject headerJson;
        String triggerTime = null;
        try {
            headerJson = visFrame.getJSONObject("headers");
            triggerTime = headerJson.get("RadioVIS-Trigger-Time").toString();
            LogDns.v(TAG, "RadioVIS-Trigger-Time" + triggerTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return triggerTime;
    }

    public static String getRadioVisFrameLink(final JSONObject visFrame) {
        JSONObject headerJson;
        String frameLink = null;
        try {
            headerJson = visFrame.getJSONObject("headers");
            frameLink = headerJson.get("RadioVIS-Link").toString();
            LogDns.v(TAG, "RadioVIS-JSON-FRAME-LINK" + frameLink);
        } catch (JSONException e) {
            LogDns.v(TAG, "There is no LINK URL");
        }
        return frameLink;
    }

    public static String getRadioVisFrameDestination(final JSONObject visFrame) {
        JSONObject headerJson;
        String frameDestination = "";
        try {
            headerJson = visFrame.getJSONObject("headers");
            frameDestination = headerJson.get("RadioVIS-Destination").toString();
            LogDns.v(TAG, "RadioVIS-JSON-FRAME-DESTINATION" + frameDestination);
        } catch (JSONException e) {
            LogDns.v(TAG, "There is no destination");
        }
        return frameDestination;
    }

    public static String getRadioVisFrameMessageId(JSONObject visFrame) {
        JSONObject headerJson;
        String messageId = "";
        try {
            headerJson = visFrame.getJSONObject("headers");
            messageId = headerJson.get("RadioVIS-Message-ID").toString();
            LogDns.v(TAG, "RadioVIS-JSON-FRAME-DESTINATION" + messageId);
        } catch (JSONException e) {
            LogDns.v(TAG, "There is no destination");
        }
        return messageId;
    }

    public static String getRadioVisFrameBodyType(final JSONObject visFrame) {
        String bodyContent = null;
        String bodyType = null;
        try {
            bodyContent = visFrame.getString(RadioVISHttpProtocol.HTTP_BODY);
            // Log.v(TAG, Body_Content.substring(0,4));
            if (bodyContent.contains(RadioVISHttpProtocol.TEXT))
                bodyType = RadioVISHttpProtocol.TEXT;
            else if (bodyContent.contains(RadioVISHttpProtocol.SHOW))
                bodyType = RadioVISHttpProtocol.SHOW;
            else
                bodyType = RadioVISHttpProtocol.NONE;

        } catch (JSONException e) {
            bodyType = RadioVISHttpProtocol.NONE;
        }
        LogDns.v(TAG, "body type" + bodyType);
        return bodyType;
    }
}
