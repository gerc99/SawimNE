package ru.sawim.listener;

import protocol.Contact;
import ru.sawim.chat.MessData;

/**
 * Created by admin on 09.06.2014.
 */
public interface OnUpdateChat {

    void addMessage(Contact contact, MessData mess);

    void updateMessages(Contact contact);

    void updateChat();

    void updateMucList();

    void pastText(String s);
}
