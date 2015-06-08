package ru.sawim.io;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import protocol.Contact;
import protocol.Group;
import protocol.Profile;
import protocol.Protocol;
import protocol.Roster;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppContact;
import protocol.xmpp.XmppServiceContact;
import ru.sawim.SawimApplication;
import ru.sawim.chat.Chat;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.RosterHelper;

/**
 * Created by gerc on 18.09.2014.
 */
public class RosterStorage {

    private static final String WHERE_ACC_CONTACT_ID = DatabaseHelper.ACCOUNT_ID + " = ? AND " + DatabaseHelper.CONTACT_ID + " = ?";
    private static final String WHERE_ACCOUNT_ID = DatabaseHelper.ACCOUNT_ID + " = ?";
    private static final String WHERE_CONTACT_ID = DatabaseHelper.CONTACT_ID + " = ?";

    private String storeName;
    public RosterStorage(String recordStoreName) {
        storeName = "roster";
        String CREATE_ROSTER_TABLE = "create table if not exists "
                + storeName + " ("
                + DatabaseHelper.ROW_AUTO_ID + " integer primary key autoincrement, "
                + DatabaseHelper.ACCOUNT_ID + " text not null, "
                + DatabaseHelper.GROUP_NAME + " text not null, "
                + DatabaseHelper.GROUP_ID + " int, "
                + DatabaseHelper.GROUP_IS_EXPAND + " int, "
                + DatabaseHelper.CONTACT_ID + " text not null, "
                + DatabaseHelper.CONTACT_NAME + " text not null, "
                + DatabaseHelper.STATUS + " int, "
                + DatabaseHelper.STATUS_TEXT + " text, "
                + DatabaseHelper.AVATAR_HASH + " text, "
                + DatabaseHelper.IS_CONFERENCE + " int, "
                + DatabaseHelper.CONFERENCE_MY_NAME + " text, "
                + DatabaseHelper.CONFERENCE_IS_AUTOJOIN + " int, "
                + DatabaseHelper.ROW_DATA + " int, "
                + DatabaseHelper.UNREAD_MESSAGES_COUNT + " int, "
                + DatabaseHelper.SUB_CONTACT_RESOURCE + " text, "
                + DatabaseHelper.SUB_CONTACT_STATUS + " int, "
                + DatabaseHelper.SUB_CONTACT_PRIORITY + " int, "
                + DatabaseHelper.SUB_CONTACT_PRIORITY_A + " int);";
        SawimApplication.getDatabaseHelper().getWritableDatabase().execSQL(CREATE_ROSTER_TABLE);
    }

    public synchronized void load(Protocol protocol) {
        if (protocol.getProfile().protocolType != Profile.PROTOCOL_JABBER) return;
        Roster roster = new Roster();
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getReadableDatabase().query(storeName, null, WHERE_ACCOUNT_ID,
                    new String[]{protocol.getUserId()}, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String account = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ACCOUNT_ID));
                    String groupName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.GROUP_NAME));
                    int groupId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.GROUP_ID));
                    boolean groupIsExpand = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.GROUP_IS_EXPAND)) == 1;
                    String userId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONTACT_ID));
                    String userName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONTACT_NAME));
                    int status = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.STATUS));
                    String statusText = cursor.getString(cursor.getColumnIndex(DatabaseHelper.STATUS_TEXT));
                    String avatarHash = cursor.getString(cursor.getColumnIndex(DatabaseHelper.AVATAR_HASH));

                    boolean isConference = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.IS_CONFERENCE)) == 1;
                    String conferenceMyNick = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONFERENCE_MY_NAME));
                    boolean conferenceIsAutoJoin = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.CONFERENCE_IS_AUTOJOIN)) == 1;
                    String subcontactRes = cursor.getString(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_RESOURCE));
                    int subcontactStatus = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_STATUS));
                    int subcontactPriority = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_PRIORITY));
                    int subcontactPriorityA = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_PRIORITY_A));

                    byte booleanValues = (byte) cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ROW_DATA));

                    Group group = roster.getGroupItems().get(groupId);
                    if (group == null) {
                        group = protocol.createGroup(groupName);
                        group.setExpandFlag(groupIsExpand);
                        roster.getGroupItems().put(groupId, group);
                    }

                    Contact contact = protocol.getItemByUID(userId);
                    if (contact == null) {
                        contact = protocol.createContact(userId, userName, isConference);
                    }
                    if (protocol.isStreamManagementSupported() && subcontactRes != null) {
                        contact.avatarHash = avatarHash;
                        contact.setStatus((byte) status, statusText);
                    }
                    contact.setGroupId(groupId);
                    contact.setBooleanValues(booleanValues);
                    if (isConference) {
                        XmppServiceContact serviceContact = (XmppServiceContact) contact;
                        serviceContact.setMyName(conferenceMyNick);
                        serviceContact.setAutoJoin(conferenceIsAutoJoin);
                        if (protocol.isStreamManagementSupported() && subcontactRes != null) {
                            XmppServiceContact.SubContact subContact = serviceContact.getSubContact((Xmpp) protocol, subcontactRes);
                            subContact.status = (byte) subcontactStatus;
                            subContact.priority = (byte) subcontactPriority;
                            subContact.priorityA = (byte) subcontactPriorityA;
                        }
                    }
                    roster.getContactItems().put(contact.getUserId(), contact);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        protocol.setRoster(roster);
    }

    public synchronized void save(Protocol protocol, Contact contact, Group group) {
        Cursor c = null;
        try {
            SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
            c = sqLiteDatabase.query(storeName, null, WHERE_ACCOUNT_ID,
                    new String[]{protocol.getUserId()}, null, null, null);
            if (group == null) {
                group = protocol.getNotInListGroup();
            }
            sqLiteDatabase.insertWithOnConflict(storeName, null,
                    getRosterValues(protocol, group, contact, null), SQLiteDatabase.CONFLICT_REPLACE);
            if (contact.isConference()) {
                XmppServiceContact serviceContact = (XmppServiceContact) contact;
                for (XmppContact.SubContact subContact : serviceContact.subcontacts.values()) {
                    sqLiteDatabase.insertWithOnConflict(storeName, null,
                            getRosterValues(protocol, group, contact, subContact), SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public synchronized void save(Protocol protocol, Contact contact, Group group, XmppServiceContact.SubContact subContact) {
        Cursor c = null;
        try {
            SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
            c = sqLiteDatabase.query(storeName, null, WHERE_ACCOUNT_ID,
                    new String[]{protocol.getUserId()}, null, null, null);
            if (group == null) {
                group = protocol.getNotInListGroup();
            }

            if (contact.isConference()) {
                sqLiteDatabase.insertWithOnConflict(storeName, null,
                            getRosterValues(protocol, group, contact, subContact), SQLiteDatabase.CONFLICT_REPLACE);
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public synchronized void delete(Protocol protocol, Contact contact, XmppServiceContact.SubContact subContact) {
        Cursor c = null;
        try {
            SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
            c = sqLiteDatabase.query(storeName, null, WHERE_ACCOUNT_ID,
                    new String[]{protocol.getUserId()}, null, null, null);

            if (contact.isConference()) {
                sqLiteDatabase.delete(storeName, DatabaseHelper.ACCOUNT_ID + "= ?" + " and "
                                + DatabaseHelper.CONTACT_ID + "= ?" + " and "
                                + DatabaseHelper.SUB_CONTACT_RESOURCE + "= ?",
                        new String[]{protocol.getUserId(), contact.getUserId(), subContact.resource});
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public synchronized void deleteContact(Protocol protocol, Contact contact) {
        SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
        sqLiteDatabase.delete(storeName, DatabaseHelper.ACCOUNT_ID + "= ?" + " and " + DatabaseHelper.CONTACT_ID + "= ?", new String[]{protocol.getUserId(), contact.getUserId()});
    }

    public synchronized void deleteGroup(Protocol protocol, Group group) {
        SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
        for (Contact contact : group.getContacts()) {
            sqLiteDatabase.delete(storeName, DatabaseHelper.ACCOUNT_ID + "= ?" + " and " + DatabaseHelper.GROUP_ID + "= ?", new String[]{protocol.getUserId(), String.valueOf(group.getGroupId())});
        }
    }

    public synchronized void updateGroup(Protocol protocol, Group group) {
        SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
        for (Contact contact : group.getContacts()) {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.GROUP_ID, group.getGroupId());
            values.put(DatabaseHelper.GROUP_NAME, group.getName());
            sqLiteDatabase.update(storeName, values, DatabaseHelper.ACCOUNT_ID + "= ?" + " and " + WHERE_CONTACT_ID, new String[]{protocol.getUserId(), contact.getUserId()});
        }
    }

    public synchronized void addGroup(Protocol protocol, Group group) {
        SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
        for (Contact contact : group.getContacts()) {
            sqLiteDatabase.insert(storeName, null, getRosterValues(protocol, group, contact, null));
        }
    }

    private ContentValues getRosterValues(Protocol protocol, Group group, Contact contact, XmppServiceContact.SubContact subContact) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.ACCOUNT_ID, protocol.getUserId());
        values.put(DatabaseHelper.GROUP_ID, group.getGroupId());
        values.put(DatabaseHelper.GROUP_NAME, group.getName());
        values.put(DatabaseHelper.GROUP_IS_EXPAND, group.isExpanded() ? 1 : 0);

        values.put(DatabaseHelper.CONTACT_ID, contact.getUserId());
        values.put(DatabaseHelper.CONTACT_NAME, contact.getName());
        values.put(DatabaseHelper.STATUS, contact.getStatusIndex());
        values.put(DatabaseHelper.STATUS_TEXT, contact.getStatusText());
        values.put(DatabaseHelper.IS_CONFERENCE, contact.isConference() ? 1 : 0);
        values.putNull(DatabaseHelper.SUB_CONTACT_RESOURCE);
        if (contact.isConference()) {
            XmppServiceContact serviceContact = (XmppServiceContact) contact;
            values.put(DatabaseHelper.CONFERENCE_MY_NAME, serviceContact.getMyName());
            values.put(DatabaseHelper.CONFERENCE_IS_AUTOJOIN, serviceContact.isAutoJoin() ? 1 : 0);

            if (subContact != null && subContact.resource != null) {
                values.put(DatabaseHelper.SUB_CONTACT_RESOURCE, subContact.resource);
                values.put(DatabaseHelper.SUB_CONTACT_STATUS, subContact.status);
                values.put(DatabaseHelper.SUB_CONTACT_PRIORITY, subContact.priority);
                values.put(DatabaseHelper.SUB_CONTACT_PRIORITY_A, subContact.priorityA);
            }
        }
        values.put(DatabaseHelper.ROW_DATA, contact.getBooleanValues());
        return values;
    }

    public synchronized void updateAvatarHash(String uniqueUserId, String hash) {
        Cursor c = null;
        try {
            c = SawimApplication.getDatabaseHelper().getWritableDatabase().query(storeName, null, WHERE_CONTACT_ID,
                            new String[]{uniqueUserId}, null, null, null);
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.AVATAR_HASH, hash);
            SawimApplication.getDatabaseHelper().getWritableDatabase().update(storeName, values, WHERE_CONTACT_ID, new String[]{uniqueUserId});
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public synchronized String getAvatarHash(String uniqueUserId) {
        String hash = null;
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getReadableDatabase().query(storeName, null,
                    WHERE_CONTACT_ID, new String[]{uniqueUserId}, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    hash = cursor.getString(cursor.getColumnIndex(DatabaseHelper.AVATAR_HASH));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null){
                cursor.close();
            }
        }
        return hash;
    }

    public synchronized void updateUnreadMessagesCount(final String protocolId, final String uniqueUserId, final int count) {
        Cursor c = null;
        try {
            SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
            c = sqLiteDatabase.query(storeName,
                    new String[]{DatabaseHelper.ACCOUNT_ID, DatabaseHelper.CONTACT_ID, DatabaseHelper.UNREAD_MESSAGES_COUNT},
                    WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId}, null, null, null);
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.UNREAD_MESSAGES_COUNT, count);
            sqLiteDatabase.update(storeName, values, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId});
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public synchronized void loadUnreadMessages() {
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getWritableDatabase().query(storeName, null, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String account = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ACCOUNT_ID));
                    String userId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONTACT_ID));
                    short unreadMessageCount = cursor.getShort(cursor.getColumnIndex(DatabaseHelper.UNREAD_MESSAGES_COUNT));
                    boolean isConference = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.IS_CONFERENCE)) == 1;
                    if (unreadMessageCount == 0) {
                        continue;
                    }
                    Protocol protocol = RosterHelper.getInstance().getProtocol(account);
                    if (protocol != null) {
                        Contact contact = protocol.getItemByUID(userId);
                        if (contact == null) {
                            contact = protocol.createContact(userId, userId, isConference);
                        }
                        Chat chat = protocol.getChat(contact);
                        chat.setOtherMessageCounter(unreadMessageCount);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
