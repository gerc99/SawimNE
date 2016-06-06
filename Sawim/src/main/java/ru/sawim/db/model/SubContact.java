package ru.sawim.db.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by gerc on 30.05.2016.
 */
public class SubContact extends RealmObject {

    @PrimaryKey
    private String subContactId;
    private String contactId;
    private String resource;
    private byte status;
    private String statusText;
    private String avatarHash;
    private byte priority;
    private byte priorityA;
    private short client;
    private int data;

    public String getSubContactId() {
        return subContactId;
    }

    public void setSubContactId(String subContactId) {
        this.subContactId = subContactId;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
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

    public byte getPriority() {
        return priority;
    }

    public void setPriority(byte priority) {
        this.priority = priority;
    }

    public byte getPriorityA() {
        return priorityA;
    }

    public void setPriorityA(byte priorityA) {
        this.priorityA = priorityA;
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }

    public short getClient() {
        return client;
    }

    public void setClient(short client) {
        this.client = client;
    }
}
