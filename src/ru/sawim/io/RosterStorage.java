package ru.sawim.io;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.Roster;
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
                + DatabaseHelper.AVATAR_HASH + " text, "
                + DatabaseHelper.IS_CONFERENCE + " int, "
                + DatabaseHelper.CONFERENCE_MY_NAME + " text, "
                + DatabaseHelper.CONFERENCE_IS_AUTOJOIN + " int, "
                + DatabaseHelper.ROW_DATA + " int, "
                + DatabaseHelper.UNREAD_MESSAGES_COUNT + " int);";
        SawimApplication.getDatabaseHelper().getWritableDatabase().execSQL(CREATE_ROSTER_TABLE);
    }

    public void load(Protocol protocol) {
        Roster roster = new Roster();
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getWritableDatabase().query(storeName, null, WHERE_ACCOUNT_ID,
                    new String[]{protocol.getUserId()}, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String account = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ACCOUNT_ID));
                    String groupName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.GROUP_NAME));
                    int groupId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.GROUP_ID));
                    boolean groupIsExpand = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.GROUP_IS_EXPAND)) == 1;
                    String userId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONTACT_ID));
                    String userName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONTACT_NAME));
                    String avatarHash = cursor.getString(cursor.getColumnIndex(DatabaseHelper.AVATAR_HASH));
                    boolean isConference = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.IS_CONFERENCE)) == 1;
                    String conferenceMyNick = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONFERENCE_MY_NAME));
                    boolean conferenceIsAutoJoin = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.CONFERENCE_IS_AUTOJOIN)) == 1;
                    byte booleanValues = (byte) cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ROW_DATA));

                    Group group = protocol.createGroup(groupName);
                    if (!roster.getGroupItems().containsKey(groupId)) {
                        group.setGroupId(groupId);
                        group.setExpandFlag(groupIsExpand);
                        roster.getGroupItems().put(groupId, group);
                    }

                    Contact contact = protocol.createContact(userId, userName, isConference);
                    contact.setGroupId(groupId);
                    contact.setBooleanValues(booleanValues);
                    if (isConference) {
                        XmppServiceContact serviceContact = (XmppServiceContact) contact;
                        serviceContact.setMyName(conferenceMyNick);
                        serviceContact.setAutoJoin(conferenceIsAutoJoin);
                    }
                    contact.avatarHash = avatarHash;
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
        protocol.setRoster(roster, false);
    }

    public void save(Protocol protocol) {
        Cursor c = null;
        try {
            c = SawimApplication.getDatabaseHelper().getWritableDatabase().query(storeName, null, WHERE_ACCOUNT_ID,
                    new String[]{protocol.getUserId()}, null, null, null);
            for (Contact contact : protocol.getContactItems().values()) {
                Group group = protocol.getGroup(contact);
                if (group == null) {
                    group = protocol.getNotInListGroup();
                }
                ContentValues values = getRosterValues(protocol, group, contact);
                SawimApplication.getDatabaseHelper().getWritableDatabase().insertWithOnConflict(storeName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private ContentValues getRosterValues(Protocol protocol, Group group, Contact contact) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.ACCOUNT_ID, protocol.getUserId());
        values.put(DatabaseHelper.GROUP_ID, group.getGroupId());
        values.put(DatabaseHelper.GROUP_NAME, group.getName());
        values.put(DatabaseHelper.GROUP_IS_EXPAND, group.isExpanded() ? 1 : 0);

        values.put(DatabaseHelper.CONTACT_ID, contact.getUserId());
        values.put(DatabaseHelper.CONTACT_NAME, contact.getName());
        values.put(DatabaseHelper.IS_CONFERENCE, contact.isConference() ? 1 : 0);
        if (contact.isConference()) {
            XmppServiceContact serviceContact = (XmppServiceContact) contact;
            values.put(DatabaseHelper.CONFERENCE_MY_NAME, serviceContact.getMyName());
            values.put(DatabaseHelper.CONFERENCE_IS_AUTOJOIN, serviceContact.isAutoJoin() ? 1 : 0);
        }
        values.put(DatabaseHelper.ROW_DATA, contact.getBooleanValues());
        return values;
    }

    public void updateAvatarHash(String uniqueUserId, String hash) {
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

    public String getAvatarHash(String uniqueUserId) {
        String hash = null;
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getWritableDatabase().query(storeName, null,
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

    public void updateUnreadMessagesCount(String protocolId, String uniqueUserId, int count) {
        Cursor c = null;
        try {
            c = SawimApplication.getDatabaseHelper().getWritableDatabase().query(storeName, null, WHERE_ACC_CONTACT_ID,
                    new String[]{protocolId, uniqueUserId}, null, null, null);
            Contact contact = RosterHelper.getInstance().getProtocol(protocolId).getItemByUID(uniqueUserId);
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.ACCOUNT_ID, protocolId);
            values.put(DatabaseHelper.CONTACT_ID, uniqueUserId);
            values.put(DatabaseHelper.IS_CONFERENCE, contact.isConference() ? 1 : 0);
            values.put(DatabaseHelper.UNREAD_MESSAGES_COUNT, count);
            SawimApplication.getDatabaseHelper().getWritableDatabase().update(storeName, values, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId});
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public void loadUnreadMessages() {
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
                        contact.updateChatState(protocol, chat);
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
