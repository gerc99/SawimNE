package protocol.xmpp;


import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.crypto.HMACSHA1;
import ru.sawim.modules.crypto.SHA1;

public class SASL_ScramSha1 {
    private String jid;
    String pass;
    String cnonce;
    String clientFirstMessageBare;
    String lServerSignature;
    HMACSHA1 hmac;

    public String init(String jid, String password) {
        this.jid = jid;
        this.pass = password;
        cnonce = "sawim" + Util.nextRandInt();
        clientFirstMessageBare = "n=" + Jid.getNick(jid) + ",r=" + cnonce;
        System.out.println("n,," + clientFirstMessageBare);
        return Util.base64encode(getBytes("n,," + clientFirstMessageBare));
    }

    public String response(String challenge) {
        return Util.base64encode(getBytes(processServerMessage(challenge)));
    }

    public boolean success(String success) {
        return lServerSignature.equals(success);
    }

    public SASL_ScramSha1() {
        hmac = new HMACSHA1();
    }

    private void xorB(byte[] dest, byte[] source) {
        int l = dest.length;
        for (int i = 0; i < l; i++) {
            dest[i] ^= source[i];
        }
    }

    private String getAttribute(String[] attrs, char id) {
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].charAt(0) == id) return attrs[i].substring(2);
        }
        return null;
    }

    private String processServerMessage(String serverFirstMessage) {
        String[] attrs = Util.explode(serverFirstMessage, ',');

        int i = Integer.parseInt(getAttribute(attrs, 'i'));
        String salt = getAttribute(attrs, 's');
        String r = getAttribute(attrs, 'r');

        byte[] pwd = getBytes(pass);

        byte[] clientKey;
        byte[] saltedPassword;
        try {
            byte[] bs = Util.base64decode(salt);
            saltedPassword = hi(pwd, bs, i);
            HMACSHA1 mac = getHMAC(saltedPassword);
            byte[] ck = getBytes("Client Key");

            clientKey = mac.hmac(ck);
        } catch (Exception e) {
            return null;
        }

        byte[] storedKey = SHA1.calculate(clientKey);

        String clientFinalMessageWithoutProof = "c=biws,r=" + r;

        String authMessage = clientFirstMessageBare + ","
                + serverFirstMessage + ","
                + clientFinalMessageWithoutProof;

        byte[] clientSignature = getHMAC(storedKey).hmac(getBytes(authMessage));
        byte[] clientProof = new byte[clientKey.length];
        System.arraycopy(clientKey, 0, clientProof, 0, clientKey.length);
        xorB(clientProof, clientSignature);

        byte[] serverKey = getHMAC(saltedPassword).hmac(getBytes("Server Key"));
        byte[] serverSignature = getHMAC(serverKey).hmac(getBytes(authMessage));
        lServerSignature = "v=" + Util.base64encode(serverSignature);

        return clientFinalMessageWithoutProof + ",p=" + Util.base64encode(clientProof);
    }

    private byte[] hi(byte[] str, byte[] salt, int i) {
        HMACSHA1 mac = getHMAC(str);
        byte[] ooo1 = {0, 0, 0, 1};
        byte[] m = new byte[salt.length + 4];
        System.arraycopy(salt, 0, m, 0, salt.length);
        System.arraycopy(ooo1, 0, m, salt.length, 4);
        byte[] U = mac.hmac(m);
        byte[] dest = new byte[U.length];
        System.arraycopy(U, 0, dest, 0, U.length);
        i--;
        while (i > 0) {
            U = mac.hmac(U);
            xorB(dest, U);
            i--;
        }
        return dest;
    }

    private HMACSHA1 getHMAC(byte[] str) {
        hmac.init(str);
        return hmac;
    }

    private byte[] getBytes(String str) {
        return StringConvertor.stringToByteArrayUtf8(str);
    }
}
