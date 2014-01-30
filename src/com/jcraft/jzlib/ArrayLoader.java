/*
 * ArrayLoader.java
 *
 * Created on 24.09.2006, 1:47
 *
 * Copyright (c) 2005-2007, Eugene Stahov (evgs), http://bombus-im.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * You can also redistribute and/or modify this program under the
 * terms of the Psi License, specified in the accompanied COPYING
 * file, as published by the Psi Project; either dated January 1st,
 * 2005, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.jcraft.jzlib;

import ru.sawim.SawimApplication;

import java.io.DataInputStream;
import java.io.InputStream;

/**
 * @author evgs
 */
public class ArrayLoader {

    public static int[] readIntArray(String name) {
        int[] arrayInt = null;
        InputStream in = null;
        try {
            in = SawimApplication.getResourceAsStream(name);
            DataInputStream is = new DataInputStream(in);
            int len = is.readInt();
            arrayInt = new int[len];

            for (int i = 0; i < len; ++i) {
                arrayInt[i] = is.readInt();
            }
            is.close();
        } catch (Exception ex) {
            arrayInt = null;
        }
        try {
            in.close();
        } catch (Exception ex) {
        }

        return arrayInt;
    }

    public static short[] readShortArray(String name) {
        short[] arrayShort = null;
        InputStream in = null;
        try {
            in = SawimApplication.getResourceAsStream(name);
            DataInputStream is = new DataInputStream(in);
            int len = is.readInt();
            arrayShort = new short[len];

            for (int i = 0; i < len; ++i) {
                arrayShort[i] = is.readShort();
            }
            is.close();
        } catch (Exception ex) {
            arrayShort = null;
        }
        try {
            in.close();
        } catch (Exception ex) {
        }

        return arrayShort;
    }

    public static byte[] readByteArray(String name) {
        byte[] arrayByte = null;
        InputStream in = null;
        try {
            in = SawimApplication.getResourceAsStream(name);
            DataInputStream is = new DataInputStream(in);
            int len = is.readInt();
            arrayByte = new byte[len];
            is.read(arrayByte, 0, len);
            is.close();
        } catch (Exception ex) {
            arrayByte = null;
        }
        try {
            in.close();
        } catch (Exception ex) {
        }

        return arrayByte;
    }
}
