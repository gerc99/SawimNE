/*
 * ControlStateListener.java
 *
 * Created on 14 Декабрь 2010 г., 11:56
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ru.sawim.ui.adapter.form;

import ru.sawim.ui.activity.BaseActivity;

/**
 * @author Vladimir Kryukov
 */
public interface ControlStateListener {
    void controlStateChanged(BaseActivity activity, String id);
}
