package ru.sawim.roster;

import protocol.Contact;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.Util;

import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolBranch extends TreeBranch {

    private Protocol protocol;
    private int id;

    public ProtocolBranch(Protocol p, int id) {
        protocol = p;
        this.id = id;
        setExpandFlag(false);
    }

    public boolean isProtocol(Protocol p) {
        return protocol == p;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public boolean isEmpty() {
        if (Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE)) {
            ConcurrentHashMap<String, Contact> allItems = protocol.getContactItems();
            Enumeration<Contact> e = allItems.elements();
            while (e.hasMoreElements()) {
                Contact contact = e.nextElement();
                if (contact.isVisibleInContactList()) {
                    return false;
                }
            }

            for (int i = allItems.size() - 1; 0 <= i; --i) {

            }
            return true;
        }
        return (0 == protocol.getContactItems().size())
                && (0 == protocol.getGroupItems().size());
    }

    @Override
    public byte getType() {
        return PROTOCOL;
    }

    @Override
    public int getGroupId() {
        return id;
    }

    public String getText() {
        return protocol.getUserId();
    }

    public int getNodeWeight() {
        return 0;
    }
}