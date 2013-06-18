



package com.jcraft.jzlib;

final class InfBlocks {
    private static final int MANY = 1440;
    
    
    private static final int[] inflate_mask = {
        0x00000000, 0x00000001, 0x00000003, 0x00000007, 0x0000000f,
        0x0000001f, 0x0000003f, 0x0000007f, 0x000000ff, 0x000001ff,
        0x000003ff, 0x000007ff, 0x00000fff, 0x00001fff, 0x00003fff,
        0x00007fff, 0x0000ffff
    };
    
    
    private static final int[] border = { 
        16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15
    };
    
    static final private int Z_OK=0;
    static final private int Z_STREAM_END=1;
    static final private int Z_BUF_ERROR=-5;
    
    static final private int TYPE=0;  
    static final private int LENS=1;  
    static final private int STORED=2;
    static final private int TABLE=3; 
    static final private int BTREE=4; 
    static final private int DTREE=5; 
    static final private int CODES=6; 
    static final private int DRY=7;   
    static final private int DONE=8;  
    static final private int BAD=9;   
    
    private int mode;            
    
    private int left;            
    
    private int table;           
    private int index;           
    private int[] blens;         
    private int[] bb = new int[1]; 
    private int[] tb = new int[1]; 
    
    private InfCodes codes;      
    
    private boolean last;            
    
    
    int bitk;            
    int bitb;            
    int[] hufts;         
    byte[] window;       
    int end;             
    int read;            
    int write;           
    private boolean checkfn;      
    private long adlerHash;          
    
    private InfTree inftree = new InfTree();
    private ZBuffers z;
    
    InfBlocks(ZBuffers z, boolean checkfn, int w) {
        hufts = new int[MANY * 3];
        window = new byte[w];
        end = w;
        this.checkfn = checkfn;
        mode = TYPE;
        this.z = z;
        codes = new InfCodes(z);
        reset();
    }
    
    long getAdlerHash() {
        return adlerHash;
    }
    final void reset() {
        if (CODES == mode) {
            codes.free();
        }
        mode = TYPE;
        bitk = 0;
        bitb = 0;
        read = write = 0;
        
        if (checkfn) {
            adlerHash = Adler32.adler32(0L, null, 0, 0);
        }
    }
    
    int result = Z_OK;
    void proc() throws ZError {
        int t;              
        int b = bitb;            
        int k = bitk;            
        int p = z.next_in_index; 
        int n = z.avail_in;      
        int q = write;                        
        int m = (q < read ? read - q - 1 : end - q); 
        byte[] z_next_in = z.next_in;
        
        
        while (true) {
            switch (mode) {
                case TYPE:
                    while (k < 3) {
                        if (n != 0) {
                            result = Z_OK;
                        } else {
                            bitb = b;
                            bitk = k;
                            z.avail_in = n;
                            z.next_in_index = p;
                            write = q;
                            inflate_flush();
                            return;
                        }
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }
                    t = (int)(b & 7);
                    last = (t & 1) == 1;
                    
                    switch (t >>> 1) {
                        case 0:                         
                        {
                            b >>>= (3);
                            k -= (3);
                        }
                        t = k & 7;                    
                        
                        {b>>>=(t);k-=(t);}
                        mode = LENS;                  
                        break;
                        case 1: {                       
                            int bl = InfTree.fixed_bl; 
                            int bd = InfTree.fixed_bd; 
                            int[] tl = InfTree.fixed_tl; 
                            int[] td = InfTree.fixed_td; 
                            codes.init(bl, bd, tl, 0, td, 0);
                            {b>>>=(3);k-=(3);}
                            mode = CODES;
                        }
                        break;

                        case 2:                         
                            {b>>>=(3);k-=(3);}
                            mode = TABLE;
                            break;

                        case 3:                         
                            throw new ZError(ZError.Z_DATA_ERROR);
                    }
                    break;

                case LENS:
                    while (k < 32) {
                        if (n != 0) {
                            result = Z_OK;
                        } else {
                            bitb=b; bitk=k;
                            z.avail_in=n;
                            z.next_in_index=p;
                            write=q;
                            inflate_flush();
                            return;
                        }
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }
                    
                    if ((((~b) >>> 16) & 0xffff) != (b & 0xffff)) {
                        ZStream.setMsg("invalid stored block lengths");
                        throw new ZError(ZError.Z_DATA_ERROR);
                    }
                    left = (b & 0xffff);
                    b = k = 0;                       
                    mode = left!=0 ? STORED : (last ? DRY : TYPE);
                    break;

                case STORED:
                    if (n == 0) {
                        bitb = b;
                        bitk = k;
                        z.avail_in = n;
                        z.next_in_index = p;
                        write = q;
                        inflate_flush();
                        return;
                    }
                    
                    if (m == 0) {
                        if (q == end && read != 0) {
                            q = 0;
                            m = (q < read ? read - q - 1 : end - q);
                        }
                        if (m == 0) {
                            write = q;
                            inflate_flush();
                            q = write;
                            m =(q < read ? read - q - 1 : end - q);
                            if (q == end && read != 0) {
                                q = 0;
                                m = (q < read ? read - q - 1 : end - q);
                            }
                            if (m == 0) {
                                bitb=b; bitk=k;
                                z.avail_in=n;
                                z.next_in_index=p;
                                write=q;
                                inflate_flush();
                                return;
                            }
                        }
                    }
                    result = Z_OK;
                    
                    t = left;
                    if(t>n) t = n;
                    if(t>m) t = m;
                    System.arraycopy(z.next_in, p, window, q, t);
                    p += t;  n -= t;
                    q += t;  m -= t;
                    if ((left -= t) != 0)
                        break;
                    mode = last ? DRY : TYPE;
                    break;
                    
                case TABLE:
                    while (k < 14) {
                        if (n != 0) {
                            result = Z_OK;
                        } else {
                            bitb = b;
                            bitk = k;
                            z.avail_in = n;
                            z.next_in_index = p;
                            write = q;
                            inflate_flush();
                            return;
                        }
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }
                    
                    table = t = (b & 0x3fff);
                    if ((t & 0x1f) > 29 || ((t >> 5) & 0x1f) > 29) {
                        ZStream.setMsg("too many length or distance symbols");
                        throw new ZError(ZError.Z_DATA_ERROR);
                    }
                    t = 258 + (t & 0x1f) + ((t >> 5) & 0x1f);
                    if ((null == blens) || (blens.length < t)) {
                        blens=new int[t];
                    } else{
                        for(int i=0; i<t; i++){blens[i]=0;}
                    }
                    
                    {b>>>=(14);k-=(14);}
                    
                    index = 0;
                    mode = BTREE;

                case BTREE:
                    while (index < 4 + (table >>> 10)) {
                        while (k < 3) {
                            if (n != 0) {
                                result = Z_OK;
                            } else {
                                bitb = b;
                                bitk = k;
                                z.avail_in = n;
                                z.next_in_index = p;
                                write = q;
                                inflate_flush();
                                return;
                            }
                            n--;
                            b |= (z_next_in[p++] & 0xff) << k;
                            k += 8;
                        }
                        
                        blens[border[index++]] = b & 7;
                        
                        {b >>>= (3); k -= (3);}
                    }
                    
                    while  (index < 19) {
                        blens[border[index++]] = 0;
                    }
                    
                    bb[0] = 7;
                    inftree.inflate_trees_bits(blens, bb, tb, hufts);

                    index = 0;
                    mode = DTREE;
                    
                case DTREE:
                    while (true) {
                        t = table;
                        if(!(index < 258 + (t & 0x1f) + ((t >> 5) & 0x1f))){
                            break;
                        }
                        
                        int[] h;
                        int i, j, c;
                        
                        t = bb[0];
                        
                        while (k < t) {
                            if (n != 0) {
                                result = Z_OK;
                            } else {
                                bitb = b; bitk = k;
                                z.avail_in = n;
                                z.next_in_index = p;
                                write = q;
                                inflate_flush();
                                return;
                            }
                            n--;
                            b |= (z_next_in[p++] & 0xff) << k;
                            k += 8;
                        }
                        
                        if (tb[0] == -1) {
                            
                        }
                        
                        t = hufts[(tb[0] + (b&inflate_mask[t])) * 3 + 1];
                        c = hufts[(tb[0] + (b&inflate_mask[t])) * 3 + 2];
                        
                        if (c < 16) {
                            b >>>= t;
                            k -= t;
                            blens[index++] = c;
                        } else { 
                            i = c == 18 ? 7 : c - 14;
                            j = c == 18 ? 11 : 3;
                            
                            while (k < (t + i)) {
                                if (n != 0) {
                                    result = Z_OK;
                                } else {
                                    bitb = b;
                                    bitk = k;
                                    z.avail_in = n;
                                    z.next_in_index = p;
                                    write = q;
                                    inflate_flush();
                                    return;
                                }
                                n--;
                                b |= (z_next_in[p++] & 0xff) << k;
                                k += 8;
                            }
                            
                            b >>>= t;
                            k -= t;
                            
                            j += (b & inflate_mask[i]);
                            
                            b >>>= i;
                            k -= i;
                            
                            i = index;
                            t = table;
                            if (i + j > 258 + (t & 0x1f) + ((t >> 5) & 0x1f) ||
                                    (c == 16 && i < 1)) {
                                ZStream.setMsg("invalid bit length repeat");
                                throw new ZError(ZError.Z_DATA_ERROR);
                            }
                            
                            c = (c == 16) ? blens[i - 1] : 0;
                            do {
                                blens[i++] = c;
                            } while (--j != 0);
                            index = i;
                        }
                    }
                    
                    tb[0]=-1;
                    {
                        int[] bl = new int[1];
                        int[] bd = new int[1];
                        int[] tl = new int[1];
                        int[] td = new int[1];
                        bl[0] = 9;         
                        bd[0] = 6;         
                        
                        t = table;
                        inftree.inflate_trees_dynamic(257 + (t & 0x1f),
                                1 + ((t >> 5) & 0x1f),
                                blens, bl, bd, tl, td, hufts);
                        t = Z_OK;
                        codes.init(bl[0], bd[0], hufts, tl[0], hufts, td[0]);
                    }
                    mode = CODES;
                    
                case CODES:
                    bitb = b;
                    bitk = k;
                    z.avail_in = n;
                    z.next_in_index = p;
                    write = q;
                    
                    codes.proc(this);
                    if (Z_STREAM_END != result) {
                        inflate_flush();
                        return;
                    }
                    result = Z_OK;
                    codes.free();
                    
                    p = z.next_in_index;
                    n = z.avail_in;
                    b = bitb;
                    k = bitk;
                    q = write;
                    m = (q < read ? read - q - 1 : end - q);
                    
                    mode = last ? DRY : TYPE;
                    break;
                    
                case DRY:
                    write = q;
                    inflate_flush();
                    q = write;
                    m = (q < read ? read - q - 1 : end - q);
                    if (read != write) {
                        bitb = b;
                        bitk = k;
                        z.avail_in = n;
                        z.next_in_index = p;
                        write = q;
                        inflate_flush();
                        return;
                    }
                    mode = DONE;
                    result = Z_STREAM_END;
                    bitb = b;
                    bitk = k;
                    z.avail_in = n;
                    z.next_in_index = p;
                    write = q;
                    inflate_flush();
                    
                case DONE:
                    result = Z_STREAM_END;
                    return;
                    
                case BAD:
                    throw new ZError(ZError.Z_DATA_ERROR);
                    
                default:
                    throw new ZError(ZError.Z_STREAM_ERROR);
            }
        }
    }
    
    final void free() {
        reset();
        window = null;
        hufts = null;
        
    }
    
    
    private void set_dictionary(byte[] d, int start, int n) {
        System.arraycopy(d, start, window, 0, n);
        read = write = n;
    }
    
    
    void inflate_flush() {
        
        int p = z.next_out_index;
        int q = read;
        
        
        int n = ((q <= write ? write : end) - q);
        if (n > z.avail_out) {
            n = z.avail_out;
        }
        if ((0 != n) && (Z_BUF_ERROR == result)) {
            result = Z_OK;
        }
        
        
        z.avail_out -= n;
        
        
        if (checkfn) {
            adlerHash = Adler32.adler32(adlerHash, window, q, n);
        }
        
        
        System.arraycopy(window, q, z.next_out, p, n);
        p += n;
        q += n;
        
        
        if (q == end) {
            
            q = 0;
            if (write == end)
                write = 0;
            
            
            n = write - q;
            if (n > z.avail_out) {
                n = z.avail_out;
            }
            if ((0 != n) && (Z_BUF_ERROR == result)) {
                result = Z_OK;
            }
            
            
            z.avail_out -= n;
            
            
            if (checkfn) {
                adlerHash = Adler32.adler32(adlerHash, window, q, n);
            }
            
            
            System.arraycopy(window, q, z.next_out, p, n);
            p += n;
            q += n;
        }
        
        
        z.next_out_index = p;
        read = q;
    }
}


