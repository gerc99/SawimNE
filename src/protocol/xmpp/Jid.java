


package protocol.xmpp;

import sawim.comm.StringConvertor;
import sawim.comm.Util;


public class Jid {
    private static final String[] transports = {"mrim.", "icq.", "picq.", "pyicq.", "sicq.", "j2j."};
    private static final String[] icqTransports = {"icq.", "picq.", "pyicq."};

    public static String realJidToSawimJid(String realJid) {
        if (isIrcConference(realJid)) {
            int index = realJid.indexOf('!');
            if (-1 != index) {
                return realJid.substring(index + 1)
                        + '/' + realJid.substring(0, index);
            }
            index = realJid.indexOf('%');
            if ((-1 != index) && (-1 != realJid.indexOf('/', realJid.indexOf('@')))) {
                return realJid.substring(index + 1);
            }
        }
        String resource = getResource(realJid, null);
        String jid = getBareJid(realJid);
        return (null == resource) ? jid : (jid + '/' + resource);
    }

    public static String SawimJidToRealJid(String SawimJid) {
        if (isIrcConference(SawimJid) && (-1 != SawimJid.indexOf('/', SawimJid.indexOf('@')))) {
            String bareJid = getBareJid(SawimJid);
            if (-1 != bareJid.indexOf('%')) {
                bareJid = bareJid.substring(bareJid.indexOf('%') + 1);
            }
            return getResource(SawimJid, "") + '!' + bareJid;
        }
        return SawimJid;
    }

    public static String getDomain(String jid) {
        jid = getBareJid(jid);
        return jid.substring(jid.indexOf('@') + 1);
    }

    public static String getResource(String fullJid, String defResource) {
        int resourceStart = fullJid.indexOf('/') + 1;
        if (0 < resourceStart) {
            return fullJid.substring(resourceStart);
        }
        return defResource;
    }

    private static boolean isConferenceDomain(String jid, int start) {
        return jid.startsWith("conference.", start)
                || jid.startsWith("conf.", start)
                || jid.startsWith("muc.", start)
                || jid.startsWith("irc.", start);
    }

    public static boolean isConference(String jid) {
        int index = jid.indexOf('@');
        if (-1 < index) {
            if (isConferenceDomain(jid, index + 1)) {
                return true;
            }
            int index1 = jid.lastIndexOf('%', index);
            if (-1 < index1) {
                return isConferenceDomain(jid, index1 + 1);
            }
        }
        return false;
    }

    public static String makeReadableJid(String jid) {
        jid = Util.replace(jid, "@conference.jabber.ru", "@c.j.ru");
        return Util.replace(jid, "@conference.", "@c.");
    }

    public static boolean isGate(String jid) {
        return (-1 == jid.indexOf('@')) && (0 < jid.length());
    }

    public static boolean isPyIcqGate(String jid) {
        if (!isGate(jid)) {
            return false;
        }
        for (int i = 0; i < icqTransports.length; ++i) {
            if (jid.startsWith(icqTransports[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMrim(String jid) {
        return (-1 != jid.indexOf("@mrim."));
    }

    public static boolean isIrcConference(String jid) {
        return (-1 != jid.indexOf("@irc."));
    }

    public static boolean isKnownGate(String jid) {
        if (Jid.isGate(jid)) {
            for (int i = 0; i < transports.length; ++i) {
                if (jid.startsWith(transports[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getBareJid(String fullJid) {
        int resourceStart = fullJid.indexOf('/');
        if (-1 != resourceStart) {
            return StringConvertor.toLowerCase(fullJid.substring(0, resourceStart));
        }
        return StringConvertor.toLowerCase(fullJid);
    }

    public static String getNick(String jid) {
        return jid.substring(0, jid.indexOf('@'));
    }

    public static String getNormalJid(String jid) {
        String bare = getBareJid(jid);
        if (-1 == jid.indexOf('/')) {
            return bare;
        }
        return bare + '/' + getResource(jid, null);
    }
}


