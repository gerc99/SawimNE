package protocol.xmpp;

import android.graphics.Bitmap;
import ru.sawim.R;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.icons.ImageList;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;

import java.util.Vector;


final class XForm {
    private Forms form;
    private boolean waitingForm;
    private boolean isXData;
    private Vector fields = new Vector();
    private Vector types = new Vector();
    private Vector values = new Vector();

    XForm() {
        waitingForm = true;
    }

    void init(int caption, FormListener listener) {
        form = new Forms(caption, listener, true);
    }

    void init(String caption, FormListener listener) {
        form = new Forms(caption, listener, true);
    }

    void init(Forms f) {
        form = f;
    }

    public boolean isWaiting() {
        return waitingForm;
    }

    public int getSize() {
        return fields.size();
    }

    public void back() {
        form.back();
    }

    public void clearForm() {
        form.clearForm();
    }

    public Forms getForm() {
        return form;
    }

    public void setWaiting() {
        waitingForm = true;
        form.setWaitingString(JLocale.getString(R.string.wait));
        form.invalidate(false);
    }

    public void setErrorMessage(String error) {
        form.setWarningString(error);
    }

    public String getField(String name) {
        for (int i = 0; i < fields.size(); ++i) {
            String field = (String) fields.elementAt(i);
            if (name.equals(field)) {
                return form.getTextFieldValue(i);
            }
        }
        return null;
    }

    public void loadXFromXml(XmlNode xml, XmlNode baseXml) {
        waitingForm = false;
        clearForm();
        XmlNode xForm = xml.getFirstNode("x", "jabber:x:data");
        isXData = (null != xForm);

        if (!isXData) {
            return;
        }
        addInfo(xForm.getFirstNodeValue("title"),
                xForm.getFirstNodeValue("instructions"));
        for (int i = 0; i < xForm.childrenCount(); ++i) {
            XmlNode item = xForm.childAt(i);
            if (!item.is("field")) {
                continue;
            }
            if (item.contains("media")) {
                String bs64img = baseXml.getFirstNodeValueRecursive("data");
                if (null != bs64img) {
                    byte[] imgBytes = Util.base64decode(bs64img);
                    bs64img = null;
                    Bitmap bitmap = ImageList.getInstance().createImage(imgBytes, 0, imgBytes.length).getBitmap();
                    form.addBitmap(ImageList.scalingCaptchaIconForDPI(bitmap));
                }
            }
            addField(item, item.getAttribute("type"));
        }
        form.invalidate(true);
    }

    public void loadFromXml(XmlNode xml, XmlNode baseXml) {
        waitingForm = false;
        clearForm();
        XmlNode xForm = xml.getFirstNode("x", "jabber:x:data");
        isXData = (null != xForm);

        if (isXData) {
            loadXFromXml(xml, baseXml);
            return;
        }
        addInfo(xml.getFirstNodeValue("title"),
                xml.getFirstNodeValue("instructions"));

        for (int i = 0; i < xml.childrenCount(); ++i) {
            XmlNode item = xml.childAt(i);
            if (item.is(S_EMAIL)) {
                addField(S_EMAIL, S_TEXT_SINGLE, "e-mail", "");

            } else if (item.is(S_USERNAME)) {
                addField(S_USERNAME, S_TEXT_SINGLE, "nick", "");

            } else if (item.is(S_PASSWORD)) {
                addField(S_PASSWORD, S_TEXT_PRIVATE, "password", "");

            } else if (item.is(S_KEY)) {
                addField(S_KEY, S_HIDDEN, "", "");
            }
        }
        form.invalidate(true);
    }

    public String getXmlForm() {
        for (int i = 0; i < fields.size(); ++i) {
            String itemType = (String) types.elementAt(i);
            if (S_LIST_SINGLE.equals(itemType)) {
                String[] list = Util.explode((String) values.elementAt(i), '|');
                values.setElementAt(list[form.getSelectorValue(i)], i);
            } else if (itemType.startsWith("jid-")) {
                values.setElementAt(form.getTextFieldValue(i), i);
            } else if (itemType.startsWith("text-")) {
                values.setElementAt(form.getTextFieldValue(i), i);
            } else if (itemType.equals(S_MEDIA)) {
                values.setElementAt(form.getTextFieldValue(i), i);
            } else if (S_BOOLEAN.equals(itemType)) {
                values.setElementAt(form.getCheckBoxValue(i) ? "1" : "0", i);
            } else if ("".equals(itemType)) {
                values.setElementAt(form.getTextFieldValue(i), i);
            }
        }
        StringBuilder sb = new StringBuilder();
        if (!isXData) {
            for (int i = 0; i < fields.size(); ++i) {
                sb.append("<").append((String) fields.elementAt(i)).append(">");
                sb.append(Util.xmlEscape((String) values.elementAt(i)));
                sb.append("</").append((String) fields.elementAt(i)).append(">");
            }
            return sb.toString();
        }
        sb.append("<x xmlns='jabber:x:data' type='submit'>");
        for (int i = 0; i < fields.size(); ++i) {
            String itemType = (String) types.elementAt(i);
            String value = (String) values.elementAt(i);

            sb.append("<field type='");
            sb.append(Util.xmlEscape(itemType));
            sb.append("' var='");
            sb.append(Util.xmlEscape((String) fields.elementAt(i)));
            sb.append("'><value>");

            if (S_JID_MULTI.equals(itemType) || S_TEXT_MULTI.equals(itemType)) {
                String[] vals = Util.explode(value.trim(), '\n');
                for (int j = 0; j < vals.length; ++j) {
                    sb.append(Util.xmlEscape(vals[j]));
                    if (j != vals.length - 1) {
                        sb.append("</value><value>");
                    }
                }

            } else {
                sb.append(Util.xmlEscape(value));
            }
            sb.append("</value></field>");
        }
        sb.append("</x>");

        return sb.toString();
    }

    private void addField(XmlNode field, String type) {
        final String S_VALUE = "value";
        final String S_OPTION = "option";
        final String S_LABEL = "label";
        String name = StringConvertor.notNull(field.getAttribute("var"));
        String label = StringConvertor.notNull(field.getAttribute(S_LABEL));
        String value = StringConvertor.notNull(field.getFirstNodeValue(S_VALUE));
        if (S_LIST_SINGLE.equals(type)) {
            int selectedIndex = 0;
            int totalCount = 0;
            StringBuilder items = new StringBuilder();
            StringBuilder labels = new StringBuilder();
            field.removeNode(S_VALUE);
            for (int i = 0; i < field.childrenCount(); ++i) {
                XmlNode opt = field.childAt(i);
                if (opt.is(S_OPTION)) {
                    String curValue = opt.getFirstNodeValue(S_VALUE);
                    labels.append('|').append(opt.getAttribute(S_LABEL));
                    items.append('|').append(curValue);
                    if (value.equals(curValue)) {
                        selectedIndex = totalCount;
                    }
                    totalCount++;
                }
            }
            items.deleteCharAt(0);
            labels.deleteCharAt(0);

            int num = fields.size();
            fields.addElement(name);
            types.addElement(type);
            values.addElement(items.toString());
            form.addSelector(num, label, labels.toString(), selectedIndex);

        } else if (S_JID_MULTI.equals(type) || S_TEXT_MULTI.equals(type)) {
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < field.childrenCount(); ++i) {
                XmlNode opt = field.childAt(i);
                if (opt.is(S_VALUE)) {
                    all.append(opt.value).append('\n');
                }
            }
            value = all.toString();
            addField(name, type, label, value);

        } else if (S_FIXED.equals(type)) {
            form.addString(value);

        } else {
            addField(name, type, label, value);
        }
    }

    private void addInfo(String title, String instructions) {
        if (!StringConvertor.isEmpty(title)) {
            form.setCaption(title);
        }
        if (!StringConvertor.isEmpty(instructions)) {
            form.addString(instructions);
        }
    }

    private void addField(String name, String type, String label, String value) {
        int num = fields.size();
        name = StringConvertor.notNull(name);
        type = StringConvertor.notNull(type);
        value = StringConvertor.notNull(value);
        fields.addElement(name);
        types.addElement(type);
        values.addElement(value);
        if (S_HIDDEN.equals(type)) {

        } else if (S_TEXT_SINGLE.equals(type)) {
            form.addTextField(num, label, value);

        } else if (S_MEDIA.equals(type)) {
            form.addTextField(num, label, value);

        } else if (S_TEXT_MULTI.equals(type)) {
            form.addTextField(num, label, value);

        } else if (S_TEXT_PRIVATE.equals(type)) {
            form.addPasswordField(num, label, value);

        } else if (S_BOOLEAN.equals(type)) {
            form.addCheckBox(num, label, XmppConnection.isTrue(value));

        } else if (S_JID_SINGLE.equals(type)) {
            form.addTextField(num, label, value);

        } else if (S_JID_MULTI.equals(type)) {
            form.addTextField(num, label, value);

        } else if ("".equals(type)) {
            form.addTextField(num, label, value);
        }
    }

    private final static String S_EMAIL = "email";
    public final static String S_USERNAME = "username";
    public final static String S_PASSWORD = "password";
    private final static String S_KEY = "key";
    private static final String S_TEXT_SINGLE = "text-single";
    private static final String S_LIST_SINGLE = "list-single";
    private static final String S_TEXT_PRIVATE = "text-private";
    private static final String S_JID_SINGLE = "jid-single";
    private static final String S_JID_MULTI = "jid-multi";
    private static final String S_HIDDEN = "hidden";
    private static final String S_BOOLEAN = "boolean";
    private static final String S_FIXED = "fixed";
    private static final String S_TEXT_MULTI = "text-multi";
    private static final String S_MEDIA = "media";
}


