



package com.jcraft.jzlib;


public class ZBuffers {
    public byte[] next_in;     
    public int next_in_index;
    public int avail_in;       
    
    public byte[] next_out;    
    public int next_out_index;
    public int avail_out;      
    
    public byte nextByte() {
        avail_in--;
        return next_in[next_in_index++];
    }
}

