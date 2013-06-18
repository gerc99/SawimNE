



package com.jcraft.jzlib;


import sawim.SawimException;
import protocol.net.TcpSocket;

public final class ZOutputStream {

    private final int bufsize = 512;
    private int flush = JZlib.Z_NO_FLUSH;
    private byte[] buf = new byte[bufsize];
    private TcpSocket out;
    private ZBuffers buffers = new ZBuffers();
    private Deflate deflate;

    public ZOutputStream(TcpSocket out, int level) {
        super();
        this.out = out;
        deflateInit(level, Deflate.MAX_WBITS, false);
    }
    private int deflateInit(int level, int bits, boolean nowrap) {
        deflate = new Deflate(buffers);
        return deflate.deflateInit(level, nowrap ? -bits : bits);
    }
    private int deflate(int flush) {
        return deflate.deflate(flush);
    }

    public void write(byte b[]) throws SawimException {
        ZBuffers buffer = buffers;
        int off = 0;
        int len = b.length;
        if (0 == len) {
            return;
        }
        int err;
        buffer.next_in = b;
        buffer.next_in_index = off;
        buffer.avail_in = len;
        do {
            buffer.next_out = buf;
            buffer.next_out_index = 0;
            buffer.avail_out = bufsize;
            err = deflate(flush);
            if (JZlib.Z_OK != err) {
                throw new SawimException(120, 11);
            }
            out.write(buf, 0, bufsize - buffer.avail_out);
        } while (buffer.avail_in>0 || buffer.avail_out==0);
    }

    public int getFlushMode() {
        return(flush);
    }

    public void setFlushMode(int flush) {
        this.flush = flush;
    }

    public void finish() throws SawimException {
        ZBuffers buffer = buffers;
        int err;
        do {
            buffer.next_out = buf;
            buffer.next_out_index = 0;
            buffer.avail_out = bufsize;
            err = deflate(JZlib.Z_FINISH);
            if (err != JZlib.Z_STREAM_END && err != JZlib.Z_OK) {
                throw new SawimException(120, 9);
            }
            if (0 < bufsize - buffer.avail_out) {
                out.write(buf, 0, bufsize - buffer.avail_out);
            }
        } while ((0 < buffer.avail_in) || (0 == buffer.avail_out));
        flush();
    }
    public void close() {
        try {
            finish();
        } catch (Exception ignored) {
        }
        deflate.deflateEnd();
        deflate = null;
        out = null;
    }

    public void flush() throws SawimException {
        out.flush();
    }

}


