
package sawim;

import android.view.Menu;
import sawim.ui.text.TextList;
import java.util.Vector;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import sawim.ui.text.TextListModel;
import sawim.util.JLocale;
import protocol.*;
import ru.sawim.models.form.ControlStateListener;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;


public class AccountsForm implements FormListener, ControlStateListener {
    private Forms form;
    private int editAccountNum;
    private TextList accountList = TextList.getInstance();

    private static final int protocolTypeField = 1011;
    private static final int uinField = 1012;
    private static final int passField = 1013;
    private static final int nickField = 1014;
    public void addAccount(int num, Profile acc) {
        Options.setAccount(num, acc);
        setCurrentProtocol();
        updateAccountList();
    }

    private void setCurrentProtocol() {
        ContactList cl = ContactList.getInstance();
        Vector listOfProfiles = new Vector();
        
        for (int i = 0; i < Options.getAccountCount(); ++i) {
            Profile p = Options.getAccount(i);
            if (p.isActive) {
                listOfProfiles.addElement(p);
            }
        }
        if (listOfProfiles.isEmpty()) {
            Profile p = Options.getAccount(0);
            p.isActive = true;
            listOfProfiles.addElement(p);
        }
        cl.addProtocols(listOfProfiles);
        cl.getManager().update();
    }

    private static final int MENU_ACCOUNT_EDIT        = 0;
    private static final int MENU_ACCOUNT_DELETE      = 1;
    private static final int MENU_ACCOUNT_UP          = 2;
    private static final int MENU_ACCOUNT_DOWN        = 3;
    private static final int MENU_ACCOUNT_SET_CURRENT = 4;
    private static final int MENU_ACCOUNT_SET_ACTIVE  = 5;
    private static final int MENU_ACCOUNT_CREATE      = 6;

    private String getProtocolName(byte type) {
        for (int i = 0; i < Profile.protocolNames.length; ++i) {
            if (Profile.protocolTypes[i] == type) {
                return Profile.protocolNames[i];
            }
        }
        return null;
    }
    private void updateAccountList() {
        TextListModel accountListModel = new TextListModel();
        int current = Options.getCurrentAccount();
        int accountCount = Options.getAccountCount();
        for (int i = 0; i < accountCount; ++i) {
            Profile account = Options.getAccount(i);
            boolean isCurrent = (current == i);
            
            isCurrent = account.isActive;
            
            String text = account.userId + (isCurrent ? "*" : "");
            
            text = getProtocolName(account.protocolType) + text;
            
            accountListModel.addItem(text, isCurrent);
        }
        final int maxAccount = Options.getMaxAccountCount();
        if (accountCount < maxAccount) {
            accountListModel.addItem(JLocale.getString("add_new"), false);
        }
        accountList.setCaption(JLocale.getString("options_account"));
        accountList.setModel(accountListModel);
    }

    /*protected void buildMenu(Menu accountMenu) {
        int accountCount = Options.getAccountCount();
        if ((0 < accountCount)) {
            accountMenu.add(Menu.FIRST, MENU_ACCOUNT_SET_ACTIVE, 2, "set_active");
        }
        accountMenu.add(Menu.FIRST, MENU_ACCOUNT_EDIT, 2, "edit");
        if ((0 < accountCount)) {
            accountMenu.add(Menu.FIRST, MENU_ACCOUNT_EDIT, 2, "delete");
        }
        if (1 < accountCount) {
            accountMenu.add(Menu.FIRST, MENU_ACCOUNT_UP, 2, "lift up");
            accountMenu.add(Menu.FIRST, MENU_ACCOUNT_DOWN, 2, "put down");
        }
        accountMenu.add(Menu.FIRST, MENU_ACCOUNT_CREATE, 2, "register_new");
    }*/

    public void showAccountEditor(Profile p) {
        initAccountEditor(Options.getAccountIndex(p)).show();
    }
    private Forms initAccountEditor(int accNum) {
        editAccountNum = accNum;
        Profile account = Options.getAccount(editAccountNum);
        form = Forms.getInstance();
        form.init("options_account", this);

        if (1 < Profile.protocolTypes.length) {
            int protocolIndex = 0;
            for (int i = 0; i < Profile.protocolTypes.length; ++i) {
                if (account.protocolType == Profile.protocolTypes[i]) {
                    protocolIndex = i;
                    break;
                }
            }
            form.addSelector(protocolTypeField, "protocol", Profile.protocolNames, protocolIndex);
        }
        
        form.addLatinTextField(uinField, "UserID", account.userId);
        form.addPasswordField(passField, "password", account.password);
        form.addTextField(nickField, "nick", account.nick);
        form.setControlStateListener(this);
        updateAccountForm();
        return form;
    }

    public void controlStateChanged(int controlId) {
        if (protocolTypeField == controlId) {
            updateAccountForm();
        }
    }
    private void updateAccountForm() {
        int id = 0;
        
        id = form.getSelectorValue(protocolTypeField);
        
        form.setTextFieldLabel(uinField, Profile.protocolIds[id]);
    }
    public boolean setCurrentAccount(int accNum) {
        if (Options.getAccountCount() <= accNum) {
            return false;
        }
        if (accNum != Options.getCurrentAccount()) {
            Options.setCurrentAccount(accNum);
            setCurrentProtocol();
            updateAccountList();
        }
        return true;
    }
    /*public void select(int cmd) {
        int num = accountList.getCurrItem();
        switch (cmd) {
            case MENU_ACCOUNT_CREATE:
                accountList.restore();
                new protocol.jabber.JabberRegistration().show();
                break;
                

            case MENU_ACCOUNT_UP:
                if ((0 != num) && (num < Options.getAccountCount())) {
                    Profile up = Options.getAccount(num);
                    Profile down = Options.getAccount(num - 1);
                    Options.setAccount(num - 1, up);
                    Options.setAccount(num, down);
                    //accountList.setCurrentItemIndex(num - 1);
                    setCurrentProtocol();
                    updateAccountList();
                }
                accountList.restore();
                break;

            case MENU_ACCOUNT_DOWN:
                if (num < Options.getAccountCount() - 1) {
                    Profile up = Options.getAccount(num);
                    Profile down = Options.getAccount(num + 1);
                    Options.setAccount(num, down);
                    Options.setAccount(num + 1, up);
                    //accountList.setCurrentItemIndex(num + 1);
                    setCurrentProtocol();
                    updateAccountList();
                }
                accountList.restore();
                break;
        }
        
        Profile account = Options.getAccount(num);
        Protocol p = ContactList.getInstance().getProtocol(account);
        if ((null != p) && p.isConnected()) {
            return;
        }
        

        switch (cmd) {
            case MENU_ACCOUNT_DELETE:
				ContactList.getInstance().getProtocol(Options.getAccount(num)).setStatus(StatusInfo.STATUS_OFFLINE, "");
                Options.delAccount(num);
                setCurrentProtocol();
                Options.safeSave();
                updateAccountList();
                accountList.restore();
                break;

                
            case MENU_ACCOUNT_SET_ACTIVE:
                if (num < Options.getAccountCount()) {
                    account.isActive = !account.isActive;
                    Options.saveAccount(account);
                    setCurrentProtocol();
                    updateAccountList();
                    accountList.restore();
                    break;
                }
                initAccountEditor(num).show();
                break;

            case MENU_ACCOUNT_SET_CURRENT:
                if (setCurrentAccount(num)) {
                    accountList.restore();
                    break;
                }

            case MENU_ACCOUNT_EDIT:
                initAccountEditor(num).show();
                break;
        }
    }*/

    public void show() {
        updateAccountList();
        accountList.show();
    }
    public void formAction(Forms form, boolean apply) {
        if (apply) {
            
            Profile account = new Profile();
            if (1 < Profile.protocolTypes.length) {
                account.protocolType = Profile.protocolTypes[form.getSelectorValue(protocolTypeField)];
            }
            account.userId = form.getTextFieldValue(uinField).trim();
            if (StringConvertor.isEmpty(account.userId)) {
                return;
            }
            account.password = form.getTextFieldValue(passField);
            account.nick = form.getTextFieldValue(nickField);
            
            if (Options.getAccountCount() <= editAccountNum) {
                account.isActive = true;
            } else {
                account.isActive = Options.getAccount(editAccountNum).isActive;
            }
            
            addAccount(editAccountNum, account);
        }
        form.back();
    }
    
    public AccountsForm() {
    }

}

