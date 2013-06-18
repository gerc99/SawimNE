


package com.jcraft.jzlib;


import sawim.SawimException;
import protocol.net.TcpSocket;

public final class ZInputStream {
    private TcpSocket in;
    private ZBuffers buffers = new ZBuffers();
    private Inflate inflate;
    private static final int bufsize = 512;
    private byte[] buf = new byte[bufsize];

    private int inflateInit(int w, boolean nowrap) {
        inflate = new Inflate();
        return inflate.inflateInit(nowrap ? -w : w, buffers);
    }

    public ZInputStream(TcpSocket in) {
        this.in = in;
        inflateInit(Inflate.MAX_WBITS, false);
        buffers.next_in = buf;
        buffers.next_in_index = 0;
        buffers.avail_in = 0;
    }

    private boolean nomoreinput = false;

    public int read(byte[] b) throws SawimException {
        int off = 0;
        int len = b.length;
        if (0 == len) {
            return 0;
        }
        ZBuffers buffer = buffers;
        int err;
        buffer.next_out = b;
        buffer.next_out_index = off;
        buffer.avail_out = len;
        do {
            if ((0 == buffer.avail_in) && !nomoreinput) {
                
                buffer.next_in_index = 0;

                int avail = in.available();
                while (0 == avail) {
                    try { Thread.sleep(70); } catch (Exception e) {};
                    avail = in.available();
                }
                buffer.avail_in = in.read(buf, 0, Math.min(avail, buf.length));


                if (-1 == buffer.avail_in) {
                    buffer.avail_in = 0;
                    nomoreinput = true;
                }
            }
            try {
                inflate.inflate(buffer);
            } catch (ZError ex) {
                throw new SawimException(120, 8);
            }
            err = inflate.getErrCode();

            if (JZlib.Z_BUF_ERROR == err) {
                if (nomoreinput) {
                    return -1;
                }
                throw new SawimException(120, 8);
            }
            if ((nomoreinput || JZlib.Z_STREAM_END == err) && (buffer.avail_out == len)) {
                return -1;
            }
        } while (buffer.avail_out == len && JZlib.Z_OK == err);

        return len - buffer.avail_out;
    }

    
    public int available() throws SawimException {
        return (0 < in.available()) ? bufsize : 0;
    }

    public void close() {
        in = null;
    }
}


