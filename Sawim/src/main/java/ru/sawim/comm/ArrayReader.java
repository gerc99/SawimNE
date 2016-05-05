
package ru.sawim.comm;


public final class ArrayReader {
    private byte[] buf;
    private int off;

    public ArrayReader(byte[] data, int index) {
        this.buf = data;
        this.off = index;
    }

    public void setOffset(int offset) {
        off = offset;
    }

    public int getOffset() {
        return off;
    }

    public boolean isNotEnd() {
        return off < buf.length;
    }

    public byte[] getBuffer() {
        return buf;
    }

    public int getByte() {
        return ((int) buf[off++]) & 0x000000FF;
    }

    public int getWordLE() {
        int val = (((int) buf[off++])) & 0x000000FF;
        return val | (((int) buf[off++]) << 8) & 0x0000FF00;
    }

    public int getWordBE() {
        int val = (((int) buf[off++]) << 8) & 0x0000FF00;
        return val | (((int) buf[off++])) & 0x000000FF;
    }

    public long getDWordLE() {
        long val;

        val = (((long) buf[off++])) & 0x000000FF;
        val |= (((long) buf[off++]) << 8) & 0x0000FF00;
        val |= (((long) buf[off++]) << 16) & 0x00FF0000;
        val |= (((long) buf[off++]) << 24) & 0xFF000000;
        return val;
    }

    public long getDWordBE() {
        long val;
        val = (((long) buf[off++]) << 24) & 0xFF000000;
        val |= (((long) buf[off++]) << 16) & 0x00FF0000;
        val |= (((long) buf[off++]) << 8) & 0x0000FF00;
        val |= (((long) buf[off++])) & 0x000000FF;
        return val;
    }

    public int getTlvType() {
        return Util.getWordBE(buf, off);
    }

    public void skipTlv() {
        off += 4 + Util.getWordBE(buf, off + 2);
    }

    public byte[] getTlv() {
        if (off + 4 > buf.length) {
            return null;
        }
        int length = Util.getWordBE(buf, off + 2);
        if (off + 4 + length > buf.length) {
            return null;
        }
        byte[] value = new byte[length];
        System.arraycopy(buf, off + 4, value, 0, length);
        off += 4 + length;
        return value;
    }

    public byte[] getArray(int length) {
        byte[] data = new byte[length];
        if (0 < length) {
            System.arraycopy(buf, off, data, 0, length);
        }
        off += length;
        return data;
    }

    public void skip(int skip) {
        off += skip;
    }

    public byte[] getTlvData(int type, int offset, int length) {
        off = offset;
        int end = offset + length;
        while (off < end) {
            if (getTlvType() == type) {
                return getTlv();
            }
            skipTlv();
        }
        return null;
    }
}

