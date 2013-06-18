

package sawim.forms;

import sawim.search.UserInfo;
import protocol.Protocol;

public class EditInfo {

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
    //private Forms form;
    private Protocol protocol;
    private UserInfo userInfo;

    public EditInfo(Protocol p, UserInfo info) {
        protocol = p;
        this.userInfo = info;
    }

    public EditInfo init() {
        
        final boolean isJabber = (protocol instanceof protocol.jabber.Jabber);
        
        

        
        /*form = new Forms("editform", "save", "cancel", this);
        form.addTextField(_NickNameItem, "nick", userInfo.nick, 64);
        form.addTextField(_FirstNameItem, "firstname", userInfo.firstName, 64);
        form.addTextField(_LastNameItem, "lastname", userInfo.lastName, 64);
        
        if (!isJabber) {
            
            form.addSelector(_SexItem, "gender", "-" + "|" + "female" + "|" + "male", userInfo.gender);
            
        }
        
        

        
        form.addTextField(_BdayItem, "birth_day", userInfo.birthDay, 15);
        
        
        if (isJabber) {
            form.addTextField(_EmailItem, "email", userInfo.email, 64);
            form.addTextField(_CellPhoneItem, "cell_phone", userInfo.cellPhone, 64);
        }
        
        




        
        form.addTextField(_HomePageItem, "home_page", userInfo.homePage, 256);

        form.addHeader("home_info");
        
        if (isJabber) {
            form.addTextField(_AddrItem, "addr", userInfo.homeAddress, 256);
        }
        
        



        
        form.addTextField(_CityItem, "city", userInfo.homeCity, 128);
        form.addTextField(_StateItem, "state", userInfo.homeState, 128);

        form.addHeader("work_info");
        form.addTextField(_WorkCompanyItem, "title", userInfo.workCompany, 256);
        form.addTextField(_WorkDepartmentItem, "depart", userInfo.workDepartment, 256);
        form.addTextField(_WorkPositionItem, "position", userInfo.workPosition, 256);
        
        if (isJabber) {
            form.addTextField(_WorkPhoneItem, "phone", userInfo.workPhone, 64);
            form.addTextField(_AboutItem, "notes", userInfo.about, 2048);
        }
        
        



        */
        return this;
    }

    public void show() {
        //form.show();
    }

    private void destroy() {
        //form.destroy();
        protocol = null;
        //form = null;
        userInfo = null;
    }

    /*public void formAction(Forms form, boolean apply) {
        if (!apply) {
            form.back();
            destroy();

        } else {
            boolean isJabber = false;
            
            isJabber = (protocol instanceof protocol.jabber.Jabber);
            
            

            
            userInfo.nick = form.getTextFieldValue(_NickNameItem);
            

            
            userInfo.birthDay = form.getTextFieldValue(_BdayItem);
            
            
            if (isJabber) {
                userInfo.email = form.getTextFieldValue(_EmailItem);
                userInfo.cellPhone = form.getTextFieldValue(_CellPhoneItem);
            }
            
            




            
            userInfo.firstName = form.getTextFieldValue(_FirstNameItem);
            userInfo.lastName = form.getTextFieldValue(_LastNameItem);
            if (!isJabber) {
                userInfo.gender = (byte) form.getSelectorValue(_SexItem);
            }
            userInfo.homePage = form.getTextFieldValue(_HomePageItem);

            
            if (isJabber) {
                userInfo.homeAddress = form.getTextFieldValue(_AddrItem);
            }
            
            



            
            userInfo.homeCity = form.getTextFieldValue(_CityItem);
            userInfo.homeState = form.getTextFieldValue(_StateItem);

            userInfo.workCompany = form.getTextFieldValue(_WorkCompanyItem);
            userInfo.workDepartment = form.getTextFieldValue(_WorkDepartmentItem);
            userInfo.workPosition = form.getTextFieldValue(_WorkPositionItem);
            
            if (isJabber) {
                userInfo.workPhone = form.getTextFieldValue(_WorkPhoneItem);
                userInfo.about = form.getTextFieldValue(_AboutItem);
            }
            
            



            

            userInfo.updateProfileView();
            protocol.saveUserInfo(userInfo);
            form.back();
            destroy();
        }
    }*/
}



