package ru.sawim.listener;

import protocol.Group;

/**
 * Created by admin on 09.06.2014.
 */
public interface OnUpdateRoster {
    void updateRoster();

    void updateProgressBar();

    void putIntoQueue(Group g);
}
