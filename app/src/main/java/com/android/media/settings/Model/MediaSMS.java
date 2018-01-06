package com.android.media.settings.Model;

import java.util.List;

public class MediaSMS {
    private String msgBody;
    private String msgFrom;
    private int msgStatus = 0;
    private List<String> idList;
    private StringBuilder msgContent;

    public MediaSMS(String msgBody, String msgFrom) {
        this.msgBody = msgBody;
        this.msgFrom = msgFrom;
    }

    public MediaSMS(String msgBody, String msgFrom, int msgStatus) {
        this.msgBody = msgBody;
        this.msgFrom = msgFrom;
        this.msgStatus = msgStatus;
    }

    public MediaSMS(List<String> idList, StringBuilder msgContent) {
        this.idList = idList;
        this.msgContent = msgContent;
    }

    public List<String> getIdList() {
        return idList;
    }

    public void setIdList(List<String> idList) {
        this.idList = idList;
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

    public StringBuilder getMsgContent() {
        return msgContent;
    }

    public void setMsgContent(StringBuilder msgContent) {
        this.msgContent = msgContent;
    }
}
