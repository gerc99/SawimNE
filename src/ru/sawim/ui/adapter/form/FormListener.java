/*
 * FormListener.java
 *
 * Created on 10 Февраль 2011 г., 23:16
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ru.sawim.ui.adapter.form;

import ru.sawim.ui.activity.BaseActivity;

/**
 * @author Vladimir Kryukov
 */
public interface FormListener {
    void formAction(BaseActivity activity, Forms form, boolean apply);
}
