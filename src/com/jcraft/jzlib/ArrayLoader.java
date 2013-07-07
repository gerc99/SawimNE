

package com.jcraft.jzlib;

import ru.sawim.General;

import java.io.DataInputStream;
import java.io.InputStream;


public class ArrayLoader {
    
    public static int[] readIntArray(String name) {
        int[] arrayInt = null;
        InputStream in = null;
        try {
            in = General.getResourceAsStream(name);
            DataInputStream is=new DataInputStream(in);
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
            in = General.getResourceAsStream(name);
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
            in = General.getResourceAsStream(name);
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