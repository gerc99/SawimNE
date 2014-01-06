package sawim.forms;

import protocol.Protocol;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import sawim.search.UserInfo;

public class EditInfo implements FormListener {

    private static final int _NickNameItem = 1000;
    private static final int _FirstNameItem = 1001;
    private static final int _LastNameItem = 1002;
    private static final int _EmailItem = 1003;
    private static final int _BdayItem = 1004;
    private static final int _CellPhoneItem = 1005;
    private static final int _AddrItem = 1006;
    private static final int _CityItem = 1007;
    private static final int _StateItem = 1008;
    private static final int _SexItem = 1009;
    private static final int _HomePageItem = 1010;
    private static final int _WorkCompanyItem = 1011;
    private static final int _WorkDepartmentItem = 1012;
    private static final int _WorkPositionItem = 1013;
    private static final int _WorkPhoneItem = 1014;
    private static final int _AboutItem = 1015;
    private Forms form;
    private Protocol protocol;
    private UserInfo userInfo;

    public EditInfo(Protocol p, UserInfo info) {
        protocol = p;
        this.userInfo = info;
    }

    public EditInfo init() {
        final boolean isXmpp = (protocol instanceof protocol.xmpp.Xmpp);
        form = new Forms("editform", this, false);

        form.addTextField(_NickNameItem, "nick", userInfo.nick);
        form.addTextField(_FirstNameItem, "firstname", userInfo.firstName);
        form.addTextField(_LastNameItem, "lastname", userInfo.lastName);

        if (!isXmpp) {
            form.addSelector(_SexItem, "gender", "-" + "|" + "female" + "|" + "male", userInfo.gender);
        }

        form.addTextField(_BdayItem, "birth_day", userInfo.birthDay);

        if (isXmpp) {
            form.addTextField(_EmailItem, "email", userInfo.email);
            form.addTextField(_CellPhoneItem, "cell_phone", userInfo.cellPhone);
        }

        form.addTextField(_HomePageItem, "home_page", userInfo.homePage);

        form.addHeader("home_info");

        if (isXmpp) {
            form.addTextField(_AddrItem, "addr", userInfo.homeAddress);
        }

        form.addTextField(_CityItem, "city", userInfo.homeCity);
        form.addTextField(_StateItem, "state", userInfo.homeState);

        form.addHeader("work_info");
        form.addTextField(_WorkCompanyItem, "title", userInfo.workCompany);
        form.addTextField(_WorkDepartmentItem, "depart", userInfo.workDepartment);
        form.addTextField(_WorkPositionItem, "position", userInfo.workPosition);

        if (isXmpp) {
            form.addTextField(_WorkPhoneItem, "phone", userInfo.workPhone);
            form.addTextField(_AboutItem, "notes", userInfo.about);
        }

        return this;
    }

    public void show() {
        form.show();
    }

    private void destroy() {
        protocol = null;
        form = null;
        userInfo = null;
    }

    @Override
    public void formAction(Forms form, boolean apply) {
        if (!apply) {
            form.back();
            destroy();

        } else {
            boolean isXmpp = false;
            isXmpp = (protocol instanceof protocol.xmpp.Xmpp);
            userInfo.nick = form.getTextFieldValue(_NickNameItem);
            userInfo.birthDay = form.getTextFieldValue(_BdayItem);
            if (isXmpp) {
                userInfo.email = form.getTextFieldValue(_EmailItem);
                userInfo.cellPhone = form.getTextFieldValue(_CellPhoneItem);
            }

            userInfo.firstName = form.getTextFieldValue(_FirstNameItem);
            userInfo.lastName = form.getTextFieldValue(_LastNameItem);
            if (!isXmpp) {
                userInfo.gender = (byte) form.getSelectorValue(_SexItem);
            }
            userInfo.homePage = form.getTextFieldValue(_HomePageItem);

            if (isXmpp) {
                userInfo.homeAddress = form.getTextFieldValue(_AddrItem);
            }

            userInfo.homeCity = form.getTextFieldValue(_CityItem);
            userInfo.homeState = form.getTextFieldValue(_StateItem);

            userInfo.workCompany = form.getTextFieldValue(_WorkCompanyItem);
            userInfo.workDepartment = form.getTextFieldValue(_WorkDepartmentItem);
            userInfo.workPosition = form.getTextFieldValue(_WorkPositionItem);

            if (isXmpp) {
                userInfo.workPhone = form.getTextFieldValue(_WorkPhoneItem);
                userInfo.about = form.getTextFieldValue(_AboutItem);
            }

            userInfo.updateProfileView();
            protocol.saveUserInfo(userInfo);
            form.back();
            destroy();
        }
    }
}