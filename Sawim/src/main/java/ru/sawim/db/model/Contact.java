package ru.sawim.db.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by gerc on 30.05.2016.
 */
public class Contact extends RealmObject {

    @PrimaryKey
    private String contactId;
    private String contactName;
    private int groupId;
    private String groupName;
    private int status;
    private String statusText;
    private String avatarHash;
    private String firstServerMessageId;
    private boolean isConference;
    private String conferenceMyName;
    private boolean conferenceIsAutoJoin;
    private byte data;
    private short unreadMessageCount;

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getAvatarHash() {
        return avatarHash;
    }

    public void setAvatarHash(String avatarHash) {
        this.avatarHash = avatarHash;
    }

    public String getFirstServerMessageId() {
        return firstServerMessageId;
    }

    public void setFirstServerMessageId(String firstServerMessageId) {
        this.firstServerMessageId = firstServerMessageId;
    }

    public boolean isConference() {
        return isConference;
    }

    public void setConference(boolean conference) {
        isConference = conference;
    }

    public String getConferenceMyName() {
        return conferenceMyName;
    }

    public void setConferenceMyName(String conferenceMyName) {
        this.conferenceMyName = conferenceMyName;
    }

    public boolean isConferenceIsAutoJoin() {
        return conferenceIsAutoJoin;
    }

    public void setConferenceIsAutoJoin(boolean conferenceIsAutoJoin) {
        this.conferenceIsAutoJoin = conferenceIsAutoJoin;
    }

    public byte getData() {
        return data;
    }

    public void setData(byte data) {
        this.data = data;
    }

    public short getUnreadMessageCount() {
        return unreadMessageCount;
    }

    public void setUnreadMessageCount(short unreadMessageCount) {
        this.unreadMessageCount = unreadMessageCount;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
}
