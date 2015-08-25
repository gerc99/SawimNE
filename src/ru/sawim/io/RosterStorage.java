package ru.sawim.io;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import protocol.Contact;
import protocol.Group;
import protocol.Profile;
import protocol.Protocol;
import protocol.Roster;
import protocol.StatusInfo;
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

    public static final String storeName = "roster";
    public static final String subContactsTable = "subcontacts";
    public RosterStorage(String recordStoreName) {
        final String CREATE_ROSTER_TABLE = "create table if not exists "
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
                + DatabaseHelper.FIRST_SERVER_MESSAGE_ID + " text, "
                + DatabaseHelper.IS_CONFERENCE + " int, "
                + DatabaseHelper.CONFERENCE_MY_NAME + " text, "
                + DatabaseHelper.CONFERENCE_IS_AUTOJOIN + " int, "
                + DatabaseHelper.ROW_DATA + " int, "
                + DatabaseHelper.UNREAD_MESSAGES_COUNT + " int);";

        final String CREATE_SUB_CONTACTS_TABLE = "create table if not exists "
                + subContactsTable + " ("
                + DatabaseHelper.CONTACT_ID + " text not null, "
                + DatabaseHelper.SUB_CONTACT_RESOURCE + " text, "
                + DatabaseHelper.SUB_CONTACT_STATUS + " int, "
                + DatabaseHelper.STATUS_TEXT + " text, "
                + DatabaseHelper.SUB_CONTACT_PRIORITY + " int, "
                + DatabaseHelper.SUB_CONTACT_PRIORITY_A + " int);";

        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SawimApplication.getDatabaseHelper().getWritableDatabase().execSQL(CREATE_ROSTER_TABLE);
                SawimApplication.getDatabaseHelper().getWritableDatabase().execSQL(CREATE_SUB_CONTACTS_TABLE);
            }
        });
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
                    String firstServerMsgId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FIRST_SERVER_MESSAGE_ID));

                    boolean isConference = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.IS_CONFERENCE)) == 1;
                    String conferenceMyNick = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONFERENCE_MY_NAME));
                    boolean conferenceIsAutoJoin = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.CONFERENCE_IS_AUTOJOIN)) == 1;

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
                    contact.firstServerMsgId = firstServerMsgId;
                    contact.avatarHash = avatarHash;
                    if (protocol.isStreamManagementSupported()) {
                        contact.setStatus((byte) status, statusText);
                    }
                    contact.setGroupId(groupId);
                    contact.setBooleanValues(booleanValues);
                    if (isConference) {
                        XmppServiceContact serviceContact = (XmppServiceContact) contact;
                        serviceContact.setMyName(conferenceMyNick);
                        serviceContact.setAutoJoin(conferenceIsAutoJoin);
                        serviceContact.setConference(true);

                        loadSubContacts(protocol, serviceContact);
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

    public void loadSubContacts(Protocol protocol, XmppServiceContact serviceContact) {
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getReadableDatabase().query(subContactsTable, null, WHERE_CONTACT_ID,
                    new String[]{serviceContact.getUserId()}, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String subcontactRes = cursor.getString(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_RESOURCE));
                    int subcontactStatus = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_STATUS));
                    String statusText = cursor.getString(cursor.getColumnIndex(DatabaseHelper.STATUS_TEXT));
                    int subcontactPriority = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_PRIORITY));
                    int subcontactPriorityA = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_PRIORITY_A));
                    if (subcontactRes != null) {
                        XmppServiceContact.SubContact subContact = serviceContact.getSubContact((Xmpp) protocol, subcontactRes);
                        subContact.status = (byte) subcontactStatus;
                        subContact.statusText = statusText;
                        subContact.priority = (byte) subcontactPriority;
                        subContact.priorityA = (byte) subcontactPriorityA;
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

    public void save(final Protocol protocol, final Contact contact, Group group) {
        if (group == null) {
            group = protocol.getNotInListGroup();
        }
        final Group finalGroup = group;
        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                Cursor cursor = null;
                try {
                    SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                    cursor = sqLiteDatabase.query(storeName,
                            new String[]{DatabaseHelper.ACCOUNT_ID, DatabaseHelper.CONTACT_ID},
                            WHERE_ACC_CONTACT_ID, new String[]{protocol.getUserId(), contact.getUserId()}, null, null, null);
                    if (cursor != null && cursor.getCount() > 0) {
                        sqLiteDatabase.update(storeName, getRosterValues(protocol, finalGroup, contact),
                                WHERE_ACC_CONTACT_ID, new String[]{protocol.getUserId(), contact.getUserId()});
                    } else {
                        sqLiteDatabase.insert(storeName, null, getRosterValues(protocol, finalGroup, contact));
                    }
                } catch (Exception e) {
                    DebugLog.panic(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
    }

    public void save(final XmppContact contact, final XmppServiceContact.SubContact subContact) {
        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                Cursor cursor = null;
                try {
                    SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                    cursor = sqLiteDatabase.query(subContactsTable,
                            null,
                            WHERE_CONTACT_ID + " AND " + DatabaseHelper.SUB_CONTACT_RESOURCE + " = ?",
                            new String[]{contact.getUserId(), subContact.resource}, null, null, null);

                    if (cursor != null && cursor.getCount() > 0) {
                        sqLiteDatabase.update(subContactsTable, getSubContactsValues(contact, subContact),
                                WHERE_CONTACT_ID + " AND " + DatabaseHelper.SUB_CONTACT_RESOURCE + " = ?",
                                new String[]{contact.getUserId(), subContact.resource});
                    } else {
                        sqLiteDatabase.insert(subContactsTable, null, getSubContactsValues(contact, subContact));
                    }
                } catch (Exception e) {
                    DebugLog.panic(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
    }

    public void delete(final Contact contact, final XmppServiceContact.SubContact subContact) {
        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                try {
                    SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                    if (contact.isConference() && subContact != null) {
                        sqLiteDatabase.delete(subContactsTable, WHERE_CONTACT_ID + " and "
                                        + DatabaseHelper.SUB_CONTACT_RESOURCE + "= ?",
                                new String[]{contact.getUserId(), subContact.resource});
                    }
                } catch (Exception e) {
                    DebugLog.panic(e);
                }
            }
        });
    }

    public void deleteContact(final Protocol protocol, final Contact contact) {
        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                sqLiteDatabase.delete(storeName, WHERE_ACC_CONTACT_ID, new String[]{protocol.getUserId(), contact.getUserId()});
            }
        });
    }

    public void deleteGroup(final Protocol protocol, final Group group) {
        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                for (Contact contact : group.getContacts()) {
                    sqLiteDatabase.delete(storeName, DatabaseHelper.ACCOUNT_ID + "= ?" + " and " + DatabaseHelper.GROUP_ID + "= ?",
                            new String[]{protocol.getUserId(), String.valueOf(group.getGroupId())});
                }
            }
        });
    }

    public void updateGroup(final Protocol protocol, final Group group) {
        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                for (Contact contact : group.getContacts()) {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.GROUP_ID, group.getGroupId());
                    values.put(DatabaseHelper.GROUP_NAME, group.getName());
                    sqLiteDatabase.update(storeName, values, WHERE_ACC_CONTACT_ID, new String[]{protocol.getUserId(), contact.getUserId()});
                }
            }
        });
    }

    public void addGroup(final Protocol protocol, final Group group) {
        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                for (Contact contact : group.getContacts()) {
                    sqLiteDatabase.insert(storeName, null, getRosterValues(protocol, group, contact));
                }
            }
        });
    }

    public void setOfflineStatuses(final Protocol protocol) {
        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                for (Contact contact : protocol.getContactItems().values()) {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.STATUS, StatusInfo.STATUS_OFFLINE);
                    if (contact.isConference()) {
                        XmppServiceContact serviceContact = (XmppServiceContact) contact;
                        for (XmppContact.SubContact subContact : serviceContact.subcontacts.values()) {
                            ContentValues values2 = new ContentValues();
                            values2.put(DatabaseHelper.SUB_CONTACT_STATUS, StatusInfo.STATUS_OFFLINE);
                            sqLiteDatabase.update(subContactsTable, values2,
                                    DatabaseHelper.SUB_CONTACT_RESOURCE + "= ?",
                                    new String[]{subContact.resource});
                        }
                    }
                    sqLiteDatabase.update(storeName, values, WHERE_ACC_CONTACT_ID, new String[]{protocol.getUserId(), contact.getUserId()});
                }
            }
        });
    }

    private ContentValues getRosterValues(Protocol protocol, Group group, Contact contact) {
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
        if (contact.isConference()) {
            XmppServiceContact serviceContact = (XmppServiceContact) contact;
            values.put(DatabaseHelper.CONFERENCE_MY_NAME, serviceContact.getMyName());
            values.put(DatabaseHelper.CONFERENCE_IS_AUTOJOIN, serviceContact.isAutoJoin() ? 1 : 0);
        }
        values.put(DatabaseHelper.ROW_DATA, contact.getBooleanValues());
        return values;
    }

    public ContentValues getSubContactsValues(XmppContact contact, XmppServiceContact.SubContact subContact) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.CONTACT_ID, contact.getUserId());
        values.put(DatabaseHelper.SUB_CONTACT_RESOURCE, subContact.resource);
        values.put(DatabaseHelper.SUB_CONTACT_STATUS, subContact.status);
        values.put(DatabaseHelper.STATUS_TEXT, subContact.statusText);
        values.put(DatabaseHelper.SUB_CONTACT_PRIORITY, subContact.priority);
        values.put(DatabaseHelper.SUB_CONTACT_PRIORITY_A, subContact.priorityA);
        return values;
    }

    public static void updateFirstServerMsgId(final Contact contact) {
        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                try {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.FIRST_SERVER_MESSAGE_ID, contact.firstServerMsgId);
                    SawimApplication.getDatabaseHelper().getWritableDatabase().update(storeName, values, WHERE_CONTACT_ID, new String[]{contact.getUserId()});
                } catch (Exception e) {
                    DebugLog.panic(e);
                }
            }
        });
    }

    public static void updateAvatarHash(final String uniqueUserId, final String hash) {
        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                try {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.AVATAR_HASH, hash);
                    SawimApplication.getDatabaseHelper().getWritableDatabase().update(storeName, values, WHERE_CONTACT_ID, new String[]{uniqueUserId});
                } catch (Exception e) {
                    DebugLog.panic(e);
                }
            }
        });
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
            if (cursor != null) {
                cursor.close();
            }
        }
        return hash;
    }

    public void updateUnreadMessagesCount(final String protocolId, final String uniqueUserId, final int count) {
        SqlAsyncTask.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                try {
                    SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.UNREAD_MESSAGES_COUNT, count);
                    sqLiteDatabase.update(storeName, values, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId});
                } catch (Exception e) {
                    DebugLog.panic(e);
                }
            }
        });
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
