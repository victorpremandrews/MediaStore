package com.android.media.settings.Models;

public class MediaSMS {
    private String msgBody;
    private String msgFrom;
    private int msgStatus = 0;

    public MediaSMS(String msgBody, String msgFrom) {
        this.msgBody = msgBody;
        this.msgFrom = msgFrom;
    }

    public MediaSMS(String msgBody, String msgFrom, int msgStatus) {
        this.msgBody = msgBody;
        this.msgFrom = msgFrom;
        this.msgStatus = msgStatus;
    }

    public String getMsgBody() {
        return msgBody;
    }

    public void setMsgBody(String msgBody) {
        this.msgBody = msgBody;
    }

    public String getMsgFrom() {
        return msgFrom;
    }

    public void setMsgFrom(String msgFrom) {
        this.msgFrom = msgFrom;
    }

    public int getMsgStatus() {
        return msgStatus;
    }

    public void setMsgStatus(int msgStatus) {
        this.msgStatus = msgStatus;
    }
}
