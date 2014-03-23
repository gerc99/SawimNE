package ru.sawim.comm;


import ru.sawim.SawimApplication;

public final class GUID {
    public static final GUID CAP_AIM_SERVERRELAY = new GUID(new byte[]{(byte) 0x09, (byte) 0x46, (byte) 0x13, (byte) 0x49, (byte) 0x4C, (byte) 0x7F, (byte) 0x11, (byte) 0xD1, (byte) 0x82, (byte) 0x22, (byte) 0x44, (byte) 0x45, (byte) 0x53, (byte) 0x54, (byte) 0x00, (byte) 0x00});
    public static final GUID CAP_AIM_ISICQ = new GUID(new byte[]{(byte) 0x09, (byte) 0x46, (byte) 0x13, (byte) 0x44, (byte) 0x4C, (byte) 0x7F, (byte) 0x11, (byte) 0xD1, (byte) 0x82, (byte) 0x22, (byte) 0x44, (byte) 0x45, (byte) 0x53, (byte) 0x54, (byte) 0x00, (byte) 0x00});
    public static final GUID CAP_UTF8 = new GUID(new byte[]{(byte) 0x09, (byte) 0x46, (byte) 0x13, (byte) 0x4E, (byte) 0x4C, (byte) 0x7F, (byte) 0x11, (byte) 0xD1, (byte) 0x82, (byte) 0x22, (byte) 0x44, (byte) 0x45, (byte) 0x53, (byte) 0x54, (byte) 0x00, (byte) 0x00});
    public static final GUID CAP_FILE_TRANSFER = new GUID(new byte[]{(byte) 0x09, (byte) 0x46, (byte) 0x13, (byte) 0x43, (byte) 0x4C, (byte) 0x7F, (byte) 0x11, (byte) 0xD1, (byte) 0x82, (byte) 0x22, (byte) 0x44, (byte) 0x45, (byte) 0x53, (byte) 0x54, (byte) 0x00, (byte) 0x00});
    public static final GUID CAP_DC = new GUID(new byte[]{(byte) 0x09, (byte) 0x46, (byte) 0x00, (byte) 0x00, (byte) 0x4C, (byte) 0x7F, (byte) 0x11, (byte) 0xD1, (byte) 0x82, (byte) 0x22, (byte) 0x44, (byte) 0x45, (byte) 0x53, (byte) 0x54, (byte) 0x00, (byte) 0x00});

    public static final GUID CAP_MTN = new GUID(new byte[]{(byte) 0x56, (byte) 0x3f, (byte) 0xc8, (byte) 0x09, (byte) 0x0b, (byte) 0x6f, (byte) 0x41, (byte) 0xbd, (byte) 0x9f, (byte) 0x79, (byte) 0x42, (byte) 0x26, (byte) 0x09, (byte) 0xdf, (byte) 0xa2, (byte) 0xf3});
    public static final GUID CAP_XTRAZ = new GUID(new byte[]{(byte) 0x1A, (byte) 0x09, (byte) 0x3C, (byte) 0x6C, (byte) 0xD7, (byte) 0xFD, (byte) 0x4E, (byte) 0xC5, (byte) 0x9D, (byte) 0x51, (byte) 0xA6, (byte) 0x47, (byte) 0x4E, (byte) 0x34, (byte) 0xF5, (byte) 0xA0});

    public static final GUID CAP_QIP_STATUS = new GUID(new byte[]{(byte) 0xB7, (byte) 0x07, (byte) 0x43, (byte) 0x78, (byte) 0xF5, (byte) 0x0C, (byte) 0x77, (byte) 0x77, (byte) 0x97, (byte) 0x77, (byte) 0x57, (byte) 0x78, (byte) 0x50, (byte) 0x2D, (byte) 0x05, (byte) 0x00});
    public static final GUID CAP_QIP_HAPPY = new GUID(new byte[]{(byte) 0xB7, (byte) 0x07, (byte) 0x43, (byte) 0x78, (byte) 0xF5, (byte) 0x0C, (byte) 0x77, (byte) 0x77, (byte) 0x97, (byte) 0x77, (byte) 0x57, (byte) 0x78, (byte) 0x50, (byte) 0x2D, (byte) 0x07, (byte) 0x77});

    public static final GUID IAMTESTER = new GUID(new byte[]{'I', ' ', 'a', 'm', ' ', 't', 'e', 's', 't', 'e', 'r', ' ', ' ', ' ', ' ', ' ', ' '});

    public static final GUID CAP_Sawim = getSawimGuid();
    private final byte[] guid;

    private static GUID getSawimGuid() {
        byte[] guid = new byte[]{'J', 'i', 'm', 'm', ' ', (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        byte[] ver = StringConvertor.stringToByteArray(SawimApplication.VERSION);
        System.arraycopy(ver, 0, guid, 5, Math.min(ver.length, 10));
        return new GUID(guid);
    }

    public GUID(byte[] arrGuid) {
        guid = arrGuid;
    }

    public byte[] toByteArray() {
        return guid;
    }

    public boolean equals(byte[] buf, int pos, int len) {
        if (buf == null) {
            return false;
        }
        len = Math.min(len, guid.length);
        for (int i = 0; i < len; ++i) {
            if (guid[i] != buf[pos + i]) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(byte[] guids) {
        if (guids == null) {
            return false;
        }
        for (int i = 0; i < guids.length; i += guid.length) {
            if (equals(guids, i, guid.length)) {
                return true;
            }
        }
        return false;
    }
}