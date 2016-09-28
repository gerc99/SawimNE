package ru.sawim.io;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import protocol.Group;
import protocol.Protocol;
import protocol.Roster;
import protocol.StatusInfo;
import protocol.xmpp.XmppContact;
import protocol.xmpp.XmppServiceContact;
import ru.sawim.chat.Chat;
import ru.sawim.db.RealmDb;
import ru.sawim.db.model.Contact;
import ru.sawim.db.model.SubContact;
import ru.sawim.roster.RosterHelper;

/**
 * Created by gerc on 18.09.2014.
 */
public class RosterStorage {

    public RosterStorage() {
    }

    public synchronized void load(Protocol protocol) {
        Roster roster = protocol.getRoster();
        if (roster == null) {
            roster = new Roster();
            protocol.setRoster(roster);
        }
        Realm realmDb = RealmDb.realm();
        List<Contact> localContacts = realmDb.where(Contact.class).findAll();
        for (Contact localContact : localContacts) {
            protocol.Contact contact = getContact(protocol, localContact);
            roster.getContactItems().put(contact.getUserId(), contact);
        }
        realmDb.close();
        protocol.setRoster(roster);
    }

    public List<protocol.Contact> getContacts(Protocol protocol) {
        Realm realmDb = RealmDb.realm();
        List<Contact> localContacts = realmDb.where(Contact.class).findAll();
        List<protocol.Contact> contacts = new ArrayList<>();
        for (Contact localContact : localContacts) {
            contacts.add(getContact(protocol, localContact));
        }
        realmDb.close();
        return contacts;
    }

    public protocol.Contact getContact(Protocol protocol, String uniqueUserId) {
        Realm realmDb = RealmDb.realm();
        Contact localContact = realmDb.where(Contact.class).equalTo("contactId", uniqueUserId).findFirst();
        protocol.Contact contact = null;
        if (localContact != null) {
            contact = getContact(protocol, localContact);
        }
        realmDb.close();
        return localContact == null ? null : contact;
    }

    public static protocol.Contact getContact(Protocol protocol, Contact localContact) {
        Log.e("getContact", localContact.getContactId()+" "+localContact.getGroupId()+" "+localContact.getGroupName()+" "+localContact.isConference());
        Group group = protocol.getGroupItems().get(localContact.getGroupId());
        if (group == null) {
            group = protocol.createGroup(localContact.getGroupName() == null ? "not" : localContact.getGroupName());
            //group.setExpandFlag(groupIsExpand);
            protocol.getGroupItems().put(localContact.getGroupId(), group);
        }

        protocol.Contact contact = protocol.getItemByUID(localContact.getContactId());
        if (contact == null) {
            contact = protocol.createContact(localContact.getContactId(), localContact.getContactName(), localContact.isConference());
        }
        contact.firstServerMsgId = localContact.getFirstServerMessageId();
        contact.avatarHash = localContact.getAvatarHash();
        contact.setStatus((byte) localContact.getStatus(), localContact.getStatusText());
        contact.setGroupId(localContact.getGroupId());
        contact.setBooleanValues(localContact.getData());
        //if (contact instanceof XmppContact) {
        //    XmppContact xmppContact = (XmppContact)contact;
        //    loadSubContacts(xmppContact);
        //}
        if (localContact.isConference()) {
            XmppServiceContact serviceContact = (XmppServiceContact) contact;
            serviceContact.setConference(true);
            serviceContact.setMyName(localContact.getConferenceMyName());
            serviceContact.setAutoJoin(localContact.isConferenceIsAutoJoin());
        }
        protocol.getRoster().getContactItems().put(contact.getUserId(), contact);
        RosterHelper.getInstance().updateGroup(protocol, group);
        return contact;
    }

    public void loadSubContacts(XmppContact xmppContact) {
        Realm realmDb = RealmDb.realm();
        List<SubContact> subContacts = realmDb.where(SubContact.class).equalTo("contactId", xmppContact.getUserId()).findAll();
        for (int i = 0; i < subContacts.size(); i++) {
            SubContact localSubContact = subContacts.get(i);
            if (localSubContact.getResource() != null) {
                XmppServiceContact.SubContact subContact = xmppContact.subcontacts.get(localSubContact.getResource());
                if (subContact == null) {
                    subContact = new XmppContact.SubContact();
                    subContact.resource = localSubContact.getResource();
                    subContact.status = localSubContact.getStatus();
                    subContact.statusText = localSubContact.getStatusText();
                    subContact.priority = localSubContact.getPriority();
                    subContact.priorityA = localSubContact.getPriorityA();
                    subContact.avatarHash = localSubContact.getAvatarHash();
                    subContact.client = localSubContact.getClient();
                    xmppContact.subcontacts.put(localSubContact.getResource(), subContact);
                }
            }
        }
        realmDb.close();
    }

    public void save(final Protocol protocol, final protocol.Contact contact, Group group) {
        if (group == null) {
            group = protocol.getNotInListGroup();
        }
        Log.e("save contact", contact.getUserId()+" "+group.getName()+" "+protocol.getStatusInfo().getName(contact.getStatusIndex()) +" "+contact.isConference());

        Contact localContact = new Contact();
        localContact.setContactId(contact.getUserId());
        localContact.setContactName(contact.getName());
        localContact.setGroupId(contact.getGroupId());
        localContact.setGroupName(group.getName());
        localContact.setStatus(contact.getStatusIndex());
        localContact.setStatusText(contact.getStatusText());
        //localContact.setFirstServerMessageId(contact.get());
        if (contact.isConference()) {
            XmppServiceContact serviceContact = (XmppServiceContact) contact;
            localContact.setConferenceMyName(serviceContact.getMyName());
            localContact.setConferenceIsAutoJoin(serviceContact.isAutoJoin());
            localContact.setConference(true);
        }
        localContact.setData(contact.getBooleanValues());
        RealmDb.save(localContact);
    }

    public void save(final XmppContact contact, final XmppServiceContact.SubContact subContact) {
        SubContact localSubContact = new SubContact();
        localSubContact.setSubContactId(contact.getUserId() + '/' + subContact.resource);
        localSubContact.setContactId(contact.getUserId());
        localSubContact.setResource(subContact.resource);
        localSubContact.setStatus(subContact.status);
        localSubContact.setStatusText(subContact.statusText);
        localSubContact.setPriority(subContact.priority);
        localSubContact.setPriorityA(subContact.priorityA);
        localSubContact.setAvatarHash(subContact.avatarHash);
        localSubContact.setClient(subContact.client);
        RealmDb.save(localSubContact);
        Log.e("save subcontact", contact.getUserId()+" "+subContact.resource+" "+contact.isConference());
    }

    public void delete(final protocol.Contact contact, final XmppServiceContact.SubContact subContact) {
        if (subContact == null) {
            return;
        }
        Realm realm = RealmDb.realm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(SubContact.class).equalTo("subContactId", contact.getUserId() + "/" + subContact.resource).findFirst().deleteFromRealm();
            }
        });
        realm.close();
        Log.e("delete", contact.getUserId()+" "+subContact.resource+" "+contact.isConference());
    }

    public void deleteContact(final Protocol protocol, final protocol.Contact contact) {
        Realm realm = RealmDb.realm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(Contact.class).equalTo("contactId", contact.getUserId()).findAll().deleteAllFromRealm();
            }
        });
        realm.close();
        Log.e("deleteContact", contact.getUserId()+" "+contact.isConference());
    }

    public void deleteGroup(final Protocol protocol, final Group group) {
        Realm realm = RealmDb.realm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(Contact.class).equalTo("groupId", group.getGroupId()).findAll().deleteAllFromRealm();
            }
        });
        realm.close();
        Log.e("deleteGroup", group.getGroupId()+" "+group.getText());
    }

    public void updateGroup(final Protocol protocol, final Group group) {
        Realm realm = RealmDb.realm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for (protocol.Contact contact : group.getContacts()) {
                    Contact localContact = realm.where(Contact.class).equalTo("contactId", contact.getUserId()).findFirst();
                    localContact.setGroupId(group.getGroupId());
                    localContact.setGroupName(group.getName());
                }
            }
        });
        realm.close();
        Log.e("updateGroup", group.getGroupId()+" "+group.getText());
    }

    public void setOfflineStatuses(final Protocol protocol) {
        Realm realm = RealmDb.realm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                final List<Contact> localContacts = realm.where(Contact.class).findAll();
                for (Contact contact : localContacts) {
                    contact.setStatus(StatusInfo.STATUS_OFFLINE);
                    if (contact.isConference()) {
                        realm.where(SubContact.class).equalTo("contactId", contact.getContactId()).findAll().deleteAllFromRealm();
                    }
                    Log.e("setOfflineStatuses", contact.getContactId()+" "+contact.isConference());
                }
            }
        });
        realm.close();
    }

    public static void updateFirstServerMsgId(final protocol.Contact contact) {
        Realm realm = RealmDb.realm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Contact localContact = realm.where(Contact.class).equalTo("contactId", contact.getUserId()).findFirst();
                localContact.setContactId(contact.getUserId());
                localContact.setFirstServerMessageId(contact.firstServerMsgId);
                Log.e("updateFirstServerMsgId", contact.getUserId()+" "+contact.isConference()+" " +contact.firstServerMsgId);
            }
        });
        realm.close();
    }

    public void updateAvatarHash(final String uniqueUserId, final String hash) {
        Realm realm = RealmDb.realm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Contact localContact = realm.where(Contact.class).equalTo("contactId", uniqueUserId).findFirst();
                localContact.setContactId(uniqueUserId);
                localContact.setAvatarHash(hash);
            }
        });
        realm.close();
    }

    public void updateSubContactAvatarHash(final String uniqueUserId, final String resource, final String hash) {
        Realm realm = RealmDb.realm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                SubContact localSubContact = realm.where(SubContact.class).equalTo("subContactId", uniqueUserId + '/' + resource).findFirst();
                localSubContact.setAvatarHash(hash);
            }
        });
        realm.close();
    }

    public synchronized String getSubContactAvatarHash(String uniqueUserId, String resource) {
        Realm realmDb = RealmDb.realm();
        SubContact localSubContact = realmDb.where(SubContact.class).equalTo("subContactId", uniqueUserId + '/' + resource).findFirst();
        String hash = null;
        if (localSubContact != null) {
            hash = localSubContact.getAvatarHash();
        }
        realmDb.close();
        return hash;
    }

    public synchronized String getAvatarHash(String uniqueUserId) {
        Realm realmDb = RealmDb.realm();
        Contact localContact = realmDb.where(Contact.class).equalTo("contactId", uniqueUserId).findFirst();
        String hash = null;
        if (localContact != null) {
            hash = localContact.getAvatarHash();
        }
        realmDb.close();
        return hash;
    }

    public void updateUnreadMessagesCount(String protocolId, final String uniqueUserId, final short count) {
        Realm realm = RealmDb.realm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Contact localContact = realm.where(Contact.class).equalTo("contactId", uniqueUserId).findFirst();
                if (localContact != null) {
                    localContact.setContactId(uniqueUserId);
                    localContact.setUnreadMessageCount(count);
                }
            }
        });
        realm.close();
    }

    public void loadUnreadMessages() {
        Realm realmDb = RealmDb.realm();
        List<Contact> localContacts = realmDb.where(Contact.class).greaterThan("unreadMessageCount", 0).findAll();
        for (int i = 0; i < localContacts.size(); i++) {
            Contact localContact = localContacts.get(i);
            String userId = localContact.getContactId();
            short unreadMessageCount = localContact.getUnreadMessageCount();
            boolean isConference = localContact.isConference();
            Protocol protocol = RosterHelper.getInstance().getProtocol();
            if (protocol != null) {
                protocol.Contact contact = protocol.getItemByUID(userId);
                if (contact == null) {
                    contact = protocol.createContact(userId, userId, isConference);
                }
                Chat chat = protocol.getChat(contact);
                chat.setOtherMessageCounter(unreadMessageCount);
            }
        }
        realmDb.close();
    }
}
