package com.sec.android.app.dns.radiovis.stomp;

import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.radiovis.RadioVISFrame;

/**
 * RadioVISStompParser using SINGLETON Design pattern. : will make only one
 * time.
 */

public class RadioVISStompParser {
    public static String getRadioVisFrameCommand(String stompFrame) {
        int startPoint = 0, endPoint = 0;
        String command = null;
        endPoint = stompFrame.indexOf('\n');
        if (endPoint == 0) { // In case of MESSAGE Frame
            startPoint = 1;
            endPoint = stompFrame.indexOf('\n', startPoint);
        }
        if (!(startPoint < 0 || startPoint > endPoint || endPoint > stompFrame.length()))
            command = stompFrame.substring(startPoint, endPoint);
        return command;
    }

    /*
     * public static String getRadioVisFrameHeader(String stompFrame) { String
     * headerName = null, headerValue = null; int PivotPoint = 0,
     * SecondParsedPoint = 0;
     * 
     * SecondParsedPoint = stompFrame.indexOf('\n');
     * 
     * while (stompFrame.lastIndexOf('\n') != SecondParsedPoint) {
     * 
     * PivotPoint = stompFrame.indexOf(":", SecondParsedPoint); headerName =
     * stompFrame .substring(SecondParsedPoint + 1, PivotPoint);
     * 
     * if (headerName.contains(RadioVISStomp.SHOW)) break;
     * 
     * SecondParsedPoint = StompFrame.indexOf('\n', PivotPoint); headerValue =
     * StompFrame.substring(PivotPoint + 1, SecondParsedPoint);
     * 
     * } return null; }
     */

    public static String getRadioVisFrameBody(String stompFrame) {
        String body = null;
        int startPoint = 0;
        int endPoint = 0;

        if (stompFrame.contains(RadioVISStompProtocol.TEXT))
            startPoint = stompFrame.indexOf(RadioVISStompProtocol.TEXT)
                    + RadioVISStompProtocol.TEXT.length();
        else if (stompFrame.contains(RadioVISStompProtocol.SHOW))
            startPoint = stompFrame.indexOf(RadioVISStompProtocol.SHOW)
                    + RadioVISStompProtocol.SHOW.length();
        endPoint = stompFrame.length();

        body = stompFrame.substring(startPoint, endPoint);
        body = body.trim();
        return body;
    }

    public static String getRadioVisFrameTriggerTime(String stompFrame) {
        int parsedPoint = 0, lineEndPosition = 0, pivotPosition = 0;
        String triggerTime = null;

        parsedPoint = stompFrame.indexOf(RadioVISStompProtocol.TRIGGER_TIME);
        if (parsedPoint == -1) {
            LogDns.i("VIS Stomp Parser", "There is no Trigger time");
            return null;
        }

        pivotPosition = stompFrame.indexOf(":", parsedPoint);
        lineEndPosition = stompFrame.indexOf('\n', pivotPosition);

        triggerTime = stompFrame.substring(pivotPosition + 1, lineEndPosition);

        return triggerTime;
    }

    public static String getRadioVisFrameLink(String stompFrame) {
        int parsedPoint = 0, lineEndPosition = 0, pivotPosition = 0;
        String link = null;

        parsedPoint = stompFrame.indexOf(RadioVISStompProtocol.LINK);
        if (parsedPoint == -1) {
            LogDns.i("VIS Stomp Parser", "There is no Link");
            return null;
        }

        pivotPosition = stompFrame.indexOf(":", parsedPoint);
        lineEndPosition = stompFrame.indexOf('\n', pivotPosition);

        link = stompFrame.substring(pivotPosition + 1, lineEndPosition);

        return link;
    }

    public static String getRadioVisFrameDestination(String stompFrame) {
        int parsedPoint = 0, lineEndPosition = 0, pivotPosition = 0;
        String destination = null;

        parsedPoint = stompFrame.indexOf(RadioVISStompProtocol.DESTINATION);
        if (parsedPoint == -1) {
            LogDns.i("VIS Stomp Parser", "There is no Destination");
            return null;
        }

        pivotPosition = stompFrame.indexOf(":", parsedPoint);
        lineEndPosition = stompFrame.indexOf('\n', pivotPosition);

        destination = stompFrame.substring(pivotPosition + 1, lineEndPosition);

        return destination;
    }

    public static String getRadioVisFrameHeaderValue(String stompFrame, String headerKey) {

        int parsedPoint = 0, lineEndPosition = 0, pivotPosition = 0;
        String headerValue = null;

        if (!stompFrame.contains(headerKey))
            return null;

        parsedPoint = stompFrame.indexOf(headerKey);
        pivotPosition = stompFrame.indexOf(":", parsedPoint);
        lineEndPosition = stompFrame.indexOf('\n', pivotPosition);

        headerValue = stompFrame.substring(pivotPosition + 1, lineEndPosition);

        return headerValue;
    }

    public static boolean verifyMessageFrame(String stompFrame) {

        // Validate STOMP Message BODY
        // TEXT < 128 byte
        // SHOW < 512 byte
        if (stompFrame.contains(RadioVISStompProtocol.TEXT)
                && getRadioVisFrameBody(stompFrame).length() > RadioVISStompProtocol.BODY_TEXTSIZE)
            return false;

        if (stompFrame.contains(RadioVISStompProtocol.SHOW)
                && getRadioVisFrameBody(stompFrame).length() > RadioVISStompProtocol.BODY_SHOWSIZE)
            return false;

        return true;
    }

    public static String getRadioVisFrameBodyType(String stompFrame) {
        String bodyType = null;

        if (stompFrame.contains(RadioVISStompProtocol.TEXT))
            bodyType = RadioVISStompProtocol.TEXT;
        else if (stompFrame.contains(RadioVISStompProtocol.SHOW))
            bodyType = RadioVISStompProtocol.SHOW;

        return bodyType;
    }

    public static RadioVISFrame parse(String receivedData) {
        String command = null;
        String content = null;
        String contentType = null;
        int commandSetValue = 0;

        RadioVISFrame frame = RadioVISFrame.obtain();
        command = getRadioVisFrameCommand(receivedData);

        if (null == command)
            return null;

        if (command.equals(RadioVISStompProtocol.CONNECTED))
            commandSetValue = RadioVISStompProtocol._CONNECTED;
        else if (command.equals(RadioVISStompProtocol.MESSAGE)) {
            commandSetValue = RadioVISStompProtocol._MESSAGE;
            // Set body content and type
            content = getRadioVisFrameBody(receivedData);
            contentType = getRadioVisFrameBodyType(receivedData);
            frame.setType(contentType);
            if (contentType != null) {
                if (contentType.equals(RadioVISStompProtocol.TEXT))
                    frame.setText(content);
                else if (contentType.equals(RadioVISStompProtocol.SHOW)) {
                    frame.setImageUrl(content);
                    frame.setLink(getRadioVisFrameLink(receivedData));
                }
            }
        } else if (command.equals(RadioVISStompProtocol.ERROR))
            commandSetValue = RadioVISStompProtocol._ERROR;
        frame.setCommand(commandSetValue);
        return frame;
    }
}
