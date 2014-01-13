/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2001 Lapo Luchini.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHORS
OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.jcraft.jzlib;

import protocol.net.TcpSocket;
import sawim.SawimException;

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
        } while (buffer.avail_in > 0 || buffer.avail_out == 0);
    }

    public int getFlushMode() {
        return (flush);
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
