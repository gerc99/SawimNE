/*
 *  MicroEmulator
 *  Copyright (C) 2001-2003 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */

package org.microemu.cldc.socket;

import android.net.SSLCertificateSocketFactory;
import sawim.modules.DebugLog;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.security.cert.X509Certificate;

public class SocketConnection implements javax.microedition.io.SocketConnection {

    protected Socket socket;

    public SocketConnection() {
    }

    public SocketConnection(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
    }

    public SocketConnection(Socket socket) {
        this.socket = socket;
    }

    public String getAddress() throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException();
        }

        return socket.getInetAddress().toString();
    }

    public String getLocalAddress() throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException();
        }

        return socket.getLocalAddress().toString();
    }

    public int getLocalPort() throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException();
        }

        return socket.getLocalPort();
    }

    public int getPort() throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException();
        }

        return socket.getPort();
    }

    public int getSocketOption(byte option) throws IllegalArgumentException,
            IOException {
        if (socket != null && socket.isClosed()) {
            throw new IOException();
        }
        switch (option) {
            case DELAY:
                if (socket.getTcpNoDelay()) {
                    return 1;
                } else {
                    return 0;
                }
            case LINGER:
                int value = socket.getSoLinger();
                if (value == -1) {
                    return 0;
                } else {
                    return value;
                }
            case KEEPALIVE:
                if (socket.getKeepAlive()) {
                    return 1;
                } else {
                    return 0;
                }
            case RCVBUF:
                return socket.getReceiveBufferSize();
            case SNDBUF:
                return socket.getSendBufferSize();
            default:
                throw new IllegalArgumentException();
        }
    }

    public void setSocketOption(byte option, int value)
            throws IllegalArgumentException, IOException {
        if (socket.isClosed()) {
            throw new IOException();
        }
        switch (option) {
            case DELAY:
                int delay;
                if (value == 0) {
                    delay = 0;
                } else {
                    delay = 1;
                }
                socket.setTcpNoDelay(delay != 0);
                break;
            case LINGER:
                if (value < 0) {
                    throw new IllegalArgumentException();
                }
                socket.setSoLinger(value != 0, value);
                break;
            case KEEPALIVE:
                int keepalive;
                if (value == 0) {
                    keepalive = 0;
                } else {
                    keepalive = 1;
                }
                socket.setKeepAlive(keepalive != 0);
                break;
            case RCVBUF:
                if (value <= 0) {
                    throw new IllegalArgumentException();
                }
                socket.setReceiveBufferSize(value);
                break;
            case SNDBUF:
                if (value <= 0) {
                    throw new IllegalArgumentException();
                }
                socket.setSendBufferSize(value);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void close() throws IOException {
        // TODO fix differences between Java ME and Java SE

        socket.close();
    }

    public InputStream openInputStream() throws IOException {
        return socket.getInputStream();
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    public OutputStream openOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    public void startTls(String host) {
        try {
//            TrustManager trustAllCerts = new X509TrustManager() {
//                    public X509Certificate[] getAcceptedIssuers() {
//                        return null;
//                    }
//                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
//                    }
//                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
//                    }
//            };
//            SSLContext sc = SSLContext.getInstance("TLS");
//            sc.init(null, new TrustManager[]{trustAllCerts}, new SecureRandom());
            SSLSocketFactory sslFactory = SSLCertificateSocketFactory.getInsecure(0, null);
            socket = sslFactory.createSocket(socket, host, socket.getPort(), true);
            ((SSLSocket) socket).setUseClientMode(true);
            dump();

        } catch (Exception e) {
            DebugLog.panic("startTls error", e);
        }
    }

    private void dump() throws SSLPeerUnverifiedException {
        SSLSession session = ((SSLSocket) socket).getSession();
        java.security.cert.Certificate[] cchain = session.getPeerCertificates();
        System.out.println("The Certificates used by peer");
        for (int i = 0; i < cchain.length; i++) {
            System.out.println(((X509Certificate) cchain[i]).getSubjectDN());
        }
        System.out.println("Peer host is " + session.getPeerHost());
        System.out.println("Cipher is " + session.getCipherSuite());
        System.out.println("Protocol is " + session.getProtocol());
        System.out.println("ID is " + session.getId().length);
        System.out.println("Session created in " + session.getCreationTime());
        System.out.println("Session accessed in " + session.getLastAccessedTime());
        System.out.println("Session valid in " + session.isValid());
    }
}
