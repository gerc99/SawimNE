



package com.jcraft.jzlib;

final class InfCodes {

    static final private int[] inflate_mask = {
        0x00000000, 0x00000001, 0x00000003, 0x00000007, 0x0000000f,
        0x0000001f, 0x0000003f, 0x0000007f, 0x000000ff, 0x000001ff,
        0x000003ff, 0x000007ff, 0x00000fff, 0x00001fff, 0x00003fff,
        0x00007fff, 0x0000ffff
    };

    static final private int Z_OK=0;
    static final private int Z_STREAM_END=1;

    
    
    
    static final private int START=0;  
    static final private int LEN=1;    
    static final private int LENEXT=2; 
    static final private int DIST=3;   
    static final private int DISTEXT=4;
    static final private int COPY=5;   
    static final private int LIT=6;    
    static final private int WASH=7;   
    static final private int END=8;    
    

    private int mode;      

    
    private int len;

    private int[] tree; 
    private int tree_index = 0;
    private int need;   

    private int lit;

    
    private int get;              
    private int dist;             

    private byte lbits;           
    private byte dbits;           
    private int[] ltree;          
    private int ltree_index;      
    private int[] dtree;          
    private int dtree_index;      

    private ZBuffers z;
    InfCodes(ZBuffers z) {
        this.z = z;
    }
    void init(int bl, int bd,
            int[] tl, int tl_index,
            int[] td, int td_index) {
        mode = START;
        lbits = (byte)bl;
        dbits = (byte)bd;
        ltree = tl;
        ltree_index = tl_index;
        dtree = td;
        dtree_index = td_index;
        tree = null;
    }

    void proc(InfBlocks s) throws ZError {
        int j = 0;              
        int[] t = null;            
        int tindex = 0;         
        int e = 0;              
        int b = s.bitb;                      
        int k = s.bitk;                      
        int p = z.next_in_index;             
        int n = z.avail_in;                  
        int q = s.write;                     
        int m = (q < s.read) ? (s.read - q - 1) : (s.end - q); 
        int f = 0;              
        final byte[] z_next_in = z.next_in;

        
        while (true) {
            switch (mode) {
                
                case START:         
                    if (m >= 258 && n >= 10) {

                        s.bitb = b;
                        s.bitk = k;
                        z.avail_in = n;
                        z.next_in_index = p;
                        s.write=q;
                        inflate_fast(lbits, dbits,
                                ltree, ltree_index,
                                dtree, dtree_index,
                                s);

                        p = z.next_in_index;
                        n = z.avail_in;
                        b = s.bitb;
                        k = s.bitk;
                        q = s.write;
                        m = q < s.read ? s.read - q - 1 : s.end - q;

                        if (Z_OK != s.result) {
                            if (Z_STREAM_END == s.result) {
                                mode = WASH;
                            } else {
                                throw new ZError(ZError.Z_DATA_ERROR);
                            }
                            break;
                        }
                    }
                    need = lbits;
                    tree = ltree;
                    tree_index = ltree_index;

                    mode = LEN;

                case LEN:           
                    j = need;

                    while (k < j) {
                        if (0 != n) {
                            s.result = Z_OK;

                        } else {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.next_in_index = p;
                            s.write = q;
                            s.inflate_flush();
                            return;
                        }
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }

                    tindex = (tree_index + (b & inflate_mask[j])) * 3;

                    b >>>= (tree[tindex + 1]);
                    k -= (tree[tindex + 1]);

                    e = tree[tindex];

                    if (e == 0) {               
                        lit = tree[tindex + 2];
                        mode = LIT;
                        break;
                    }
                    if ((e & 16) != 0) {          
                        get = e & 15;
                        len = tree[tindex + 2];
                        mode = LENEXT;
                        break;
                    }
                    if ((e & 64) == 0) {        
                        need = e;
                        tree_index = tindex / 3 + tree[tindex + 2];
                        break;
                    }
                    if ((e & 32) != 0) {               
                        mode = WASH;
                        break;
                    }
                    ZStream.setMsg("invalid literal/length code");
                    throw new ZError(ZError.Z_DATA_ERROR);

                case LENEXT:        
                    j = get;

                    while  (k < j) {
                        if (n != 0) {
                            s.result = Z_OK;
                        } else {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.next_in_index = p;
                            s.write = q;
                            s.inflate_flush();
                            return;
                        }
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }

                    len += (b & inflate_mask[j]);

                    b >>= j;
                    k -= j;

                    need = dbits;
                    tree = dtree;
                    tree_index = dtree_index;
                    mode = DIST;

                case DIST:          
                    j = need;

                    while (k < j) {
                        if (n != 0) {
                            s.result = Z_OK;
                        } else {

                            s.bitb=b;s.bitk=k;
                            z.avail_in=n;
                            z.next_in_index=p;
                            s.write = q;
                            s.inflate_flush();
                            return;
                        }
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }

                    tindex = (tree_index + (b & inflate_mask[j])) * 3;

                    b >>= tree[tindex + 1];
                    k -= tree[tindex + 1];

                    e = (tree[tindex]);
                    if ((e & 16) != 0) {               
                        get = e & 15;
                        dist = tree[tindex+2];
                        mode = DISTEXT;
                        break;
                    }
                    if ((e & 64) == 0) {        
                        need = e;
                        tree_index = tindex / 3 + tree[tindex + 2];
                        break;
                    }
                    ZStream.setMsg("invalid distance code");
                    throw new ZError(ZError.Z_DATA_ERROR);

                case DISTEXT:       
                    j = get;

                    while (k < j) {
                        if(n != 0) {
                            s.result = Z_OK;
                        } else {

                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.next_in_index = p;
                            s.write = q;
                            s.inflate_flush();
                            return;
                        }
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }

                    dist += (b & inflate_mask[j]);

                    b >>= j;
                    k -= j;

                    mode = COPY;

                case COPY:          
                    f = q - dist;
                    while (f < 0) {     
                        f += s.end;     
                    }
                    while (len != 0) {

                        if (m == 0) {
                            if (q == s.end && s.read != 0) {
                                q = 0;
                                m = q < s.read ? s.read - q - 1 : s.end - q;
                            }
                            if (m == 0) {
                                s.write = q;
                                s.inflate_flush();
                                q = s.write;
                                m = q < s.read ? s.read - q - 1 : s.end - q;

                                if (q == s.end && s.read != 0) {
                                    q = 0;
                                    m = q < s.read ? s.read - q - 1 : s.end - q;
                                }

                                if (m == 0) {
                                    s.bitb = b;
                                    s.bitk = k;
                                    z.avail_in = n;
                                    z.next_in_index = p;
                                    s.write = q;
                                    s.inflate_flush();
                                    return;
                                }
                            }
                        }

                        s.window[q++] = s.window[f++];
                        m--;

                        if (f == s.end) {
                            f = 0;
                        }
                        len--;
                    }
                    mode = START;
                    break;
                case LIT:           
                    if (m == 0) {
                        if (q == s.end && s.read != 0) {
                            q = 0;
                            m = q < s.read ? s.read - q - 1 : s.end - q;
                        }
                        if (m == 0) {
                            s.write = q;
                            s.inflate_flush();
                            q = s.write;
                            m = q < s.read ? s.read - q - 1 : s.end - q;

                            if (q == s.end && s.read != 0) {
                                q = 0;
                                m = q < s.read ? s.read - q - 1 : s.end - q;
                            }
                            if (m == 0) {
                                s.bitb = b;
                                s.bitk = k;
                                z.avail_in = n;
                                z.next_in_index = p;
                                s.write = q;
                                s.inflate_flush();
                                return;
                            }
                        }
                    }
                    s.result = Z_OK;

                    s.window[q++] = (byte)lit;
                    m--;

                    mode = START;
                    break;

                case WASH:           
                    if (k > 7) {        
                        k -= 8;
                        n++;
                        p--;             
                    }

                    s.write = q;
                    s.inflate_flush();
                    q = s.write;
                    m = q < s.read ? s.read - q - 1 : s.end - q;

                    if (s.read != s.write) {
                        s.bitb = b;
                        s.bitk = k;
                        z.avail_in = n;
                        z.next_in_index = p;
                        s.write = q;
                        s.inflate_flush();
                        return;
                    }
                    s.result = Z_STREAM_END;
                    mode = END;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.next_in_index = p;
                    s.write = q;
                    s.inflate_flush();

                case END:
                    s.result = Z_STREAM_END;
                    return;

                default:
                    throw new ZError(ZError.Z_STREAM_ERROR);
            }
        }
    }

    void free() {
    }

    
    
    
    

    private void inflate_fast(int bl, int bd,
            int[] tl, int tl_index,
            int[] td, int td_index,
            InfBlocks s) throws ZError {
        int t;                
        int[] tp;             
        int tp_index;         
        int e;                
        int c;                
        int d;                
        int r;                

        
        int b = s.bitb;          
        int k = s.bitk;          
        int p = z.next_in_index; 
        int n = z.avail_in;      
        int q = s.write;         
        int m = (q < s.read) ? (s.read - q - 1) : (s.end - q); 

        int tp_index_t_3;     

        
        int ml = inflate_mask[bl]; 
        int md = inflate_mask[bd]; 

        byte[] z_next_in = z.next_in;

        
        do {                          
            
            while (k < 20) {              
                n--;
                b |= (z_next_in[p++]&0xff) << k;
                k += 8;
            }

            t = b & ml;
            tp = tl;
            tp_index = tl_index;
            tp_index_t_3 = (tp_index + t) * 3;
            if ((e = tp[tp_index_t_3]) == 0){
                b >>= (tp[tp_index_t_3 + 1]);
                k -= (tp[tp_index_t_3 + 1]);

                s.window[q++] = (byte)tp[tp_index_t_3 + 2];
                m--;
                continue;
            }
            while (true) {

                b >>= (tp[tp_index_t_3 + 1]);
                k -= (tp[tp_index_t_3 + 1]);

                if ((e & 16) != 0) {
                    e &= 15;
                    c = tp[tp_index_t_3+2] + ((int)b & inflate_mask[e]);

                    b>>=e; k-=e;

                    
                    while (k < 15) {           
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }

                    t = b & md;
                    tp = td;
                    tp_index = td_index;
                    tp_index_t_3 = (tp_index + t) * 3;
                    e = tp[tp_index_t_3];

                    while (true) {

                        b >>= (tp[tp_index_t_3 + 1]);
                        k -= (tp[tp_index_t_3 + 1]);

                        if ((e & 16) != 0) {
                            
                            e &= 15;
                            while (k < e) {         
                                n--;
                                b |= (z_next_in[p++] & 0xff) << k;
                                k += 8;
                            }

                            d = tp[tp_index_t_3 + 2] + (b & inflate_mask[e]);

                            b >>= (e);
                            k -= (e);

                            
                            m -= c;
                            if (q >= d) {                
                                
                                r=q-d;
                                if(q-r>0 && 2>(q-r)){
                                    s.window[q++]=s.window[r++]; 
                                    s.window[q++]=s.window[r++]; 
                                    c-=2;
                                } else{
                                    System.arraycopy(s.window, r, s.window, q, 2);
                                    q+=2; r+=2; c-=2;
                                }
                            } else {                  
                                r = q - d;
                                do {
                                    r += s.end;          
                                } while (r < 0);         
                                e = s.end - r;
                                if (c > e) {             
                                    c -= e;              
                                    if (q - r > 0 && e > (q - r)) {
                                        do {
                                            s.window[q++] = s.window[r++];
                                        } while (--e!=0);
                                    } else{
                                        System.arraycopy(s.window, r, s.window, q, e);
                                        q += e;
                                        r += e;
                                        e=0;
                                    }
                                    r = 0;                  
                                }

                            }

                            
                            if (q - r > 0 && c > (q - r)) {
                                do {
                                    s.window[q++] = s.window[r++];
                                } while(--c != 0);
                            } else {
                                System.arraycopy(s.window, r, s.window, q, c);
                                q += c;
                                r += c;
                                c = 0;
                            }
                            break;
                        } else if ((e & 64) == 0) {
                            t += tp[tp_index_t_3+2];
                            t += (b & inflate_mask[e]);
                            tp_index_t_3 = (tp_index + t) * 3;
                            e = tp[tp_index_t_3];
                        } else {
                            ZStream.setMsg("invalid distance code");
                            throw new ZError(ZError.Z_DATA_ERROR);
                        }
                    }
                    break;
                }

                if ((e & 64) == 0) {
                    t += tp[tp_index_t_3 + 2];
                    t += (b&inflate_mask[e]);
                    tp_index_t_3 = (tp_index + t) * 3;
                    e = tp[tp_index_t_3];
                    if (e == 0) {

                        b >>= (tp[tp_index_t_3 + 1]);
                        k -= (tp[tp_index_t_3 + 1]);

                        s.window[q++] = (byte)tp[tp_index_t_3 + 2];
                        m--;
                        break;
                    }

                } else if ((e & 32) != 0) {

                    c = z.avail_in - n;
                    c = (k >> 3) < c ? k >> 3 : c;
                    n += c;
                    p -= c;
                    k -= c << 3;

                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.next_in_index = p;
                    s.write = q;

                    s.result = Z_STREAM_END;
                    return;

                } else {
                    ZStream.setMsg("invalid literal/length code");
                    throw new ZError(ZError.Z_DATA_ERROR);
                }
            }
        } while (m >= 258 && n >= 10);

        
        c = z.avail_in - n;
        c = (k >> 3) < c ? k >> 3 : c;
        n += c;
        p -= c;
        k -= c << 3;

        s.bitb = b;
        s.bitk = k;
        z.avail_in = n;
        z.next_in_index = p;
        s.write = q;

        s.result = Z_OK;
    }
}


