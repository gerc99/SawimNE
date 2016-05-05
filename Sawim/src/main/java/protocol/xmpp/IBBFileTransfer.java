

package protocol.xmpp;


import ru.sawim.comm.Util;
import ru.sawim.modules.FileTransfer;


public class IBBFileTransfer {
    private String to;
    private String sid;

    private int blockIndex;
    private static final int blockSize = 4096;

    private String fileName;
    private String fileDesc;
    private FileTransfer ft;

    public IBBFileTransfer(String name, String desc, FileTransfer ft) {
        this.fileName = name;
        this.fileDesc = desc;
        this.ft = ft;
        XmppContact c = (XmppContact) ft.getReceiver();
        this.to = c.getUserId();
        if (!(c instanceof XmppServiceContact)) {
            String resource = c.getCurrentSubContact().resource;
            this.to += '/' + resource;
        }
        this.sid = Util.xmlEscape("Sawim" + Util.uniqueValue());
    }

    public void setProgress(int percent) {
        ft.setProgress(percent);
    }

    public boolean isCanceled() {
        return ft.isCanceled();
    }

    public void destroy() {
        ft.destroy();
        ft = null;
    }

    public String initTransfer() {
        return "<iq id='Sawimibb_open' to='"
                + Util.xmlEscape(to)
                + "' type='set'><open xmlns='http://jabber.org/protocol/ibb' block-size='"
                + blockSize + "' sid='" + sid + "' stanza='iq'/></iq>";
    }

    private byte[] readNextBlock() {
        int size = Math.min(blockSize, ft.getFileSize() - blockIndex * blockSize);
        if (size < 0) {
            return null;
        }
        try {
            byte[] data = new byte[size];
            ft.getFileIS().read(data);
            return data;
        } catch (Exception ex) {
            return null;
        }
    }

    public String nextBlock() {
        byte[] data = readNextBlock();
        if (null == data) {
            return null;
        }
        String xml = "<iq id='Sawimibb_" + blockIndex + "' to='" + Util.xmlEscape(to)
                + "' type='set'><data xmlns='http://jabber.org/protocol/ibb' seq='"
                + blockIndex + "' sid='" + sid + "'>"
                + Util.xmlEscape(Util.base64encode(data)) + "</data></iq>";
        blockIndex++;
        return xml;
    }

    public int getPercent() {
        return 10 + (blockIndex * blockSize * 90 / ft.getFileSize());
    }

    public String close() {
        return "<iq id='Sawimibb_close' to='" + Util.xmlEscape(to)
                + "' type='set'><close xmlns='http://jabber.org/protocol/ibb' sid='"
                + sid + "'/></iq>";
    }

    public String getRequest() {
        return "<iq type='set' id='Sawimibb_si' to='"
                + Util.xmlEscape(to) + "'><si xmlns='http://jabber.org/protocol/si' id='"
                + sid + "' "
                + "mime-type='application/octet-stream' "
                + "profile='http://jabber.org/protocol/si/profile/file-transfer'>"
                + "<file xmlns='http://jabber.org/protocol/si/profile/file-transfer' "
                + "name='" + Util.xmlEscape(fileName) + "' "
                + "size='" + ft.getFileSize() + "' >"
                + "<desc>" + Util.xmlEscape(fileDesc) + "</desc>"
                + "</file>"
                + "<feature xmlns='http://jabber.org/protocol/feature-neg'>"
                + "<x xmlns='jabber:x:data' type='form'>"
                + "<field var='stream-method' type='list-single'>"
                + "<option><value>http://jabber.org/protocol/ibb</value></option>"
                + "</field></x></feature></si></iq>";
    }
}



