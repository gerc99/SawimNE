



package com.jcraft.jzlib;


final class InfTree {

    static final private int MANY=1440;

    public static final int fixed_bl = 9;
    public static final int fixed_bd = 5;

    public static int[] fixed_tl = ArrayLoader.readIntArray("/fixed_tl.zlib");
    public static int[] fixed_td = ArrayLoader.readIntArray("/fixed_td.zlib");

    
    static final int[] cplens = { 
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
        35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0
    };

    
    static final int[] cplext = { 
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
        3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 112, 112  
    };

    static final int[] cpdist = { 
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
        257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
        8193, 12289, 16385, 24577
    };

    static final int[] cpdext = { 
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11,
        12, 12, 13, 13};

    
    static final int BMAX = 15;         

    int[] hn = null;  
    int[] v = null;   
    int[] c = null;   
    int[] r = null;   
    int[] u = null;   
    int[] x = null;   

    private void huft_build(int[] b, 
            int bindex,
            int n,   
            int s,   
            int[] d, 
            int[] e, 
            int[] t, 
            int[] m, 
            int[] hp,
            int[] hn,
            int[] v  
            ) throws ZError {
        
        
        
        
        

        int a;                       
        int f;                       
        int g;                       
        int h;                       
        int i;                       
        int j;                       
        int k;                       
        int l;                       
        int mask;                    
        int p;                       
        int q;                       
        int w;                       
        int xp;                      
        int y;                       
        int z;                       

        

        p = 0; i = n;
        do {
            c[b[bindex + p]]++; p++; i--;   
        } while (0 != i);

        if (c[0] == n) {                
            t[0] = -1;
            m[0] = 0;
            return;
        }

        
        l = m[0];
        for (j = 1; j <= BMAX; ++j) {
            if (0 != c[j]) break;
        }
        k = j;                        
        if (l < j) {
            l = j;
        }
        for (i = BMAX; i != 0; --i) {
            if (0 != c[i]) break;
        }
        g = i;                        
        if (l > i) {
            l = i;
        }
        m[0] = l;

        
        for (y = 1 << j; j < i; j++, y <<= 1){
            if ((y -= c[j]) < 0) {
                throw new ZError(ZError.Z_DATA_ERROR);
            }
        }
        if ((y -= c[i]) < 0) {
            throw new ZError(ZError.Z_DATA_ERROR);
        }
        c[i] += y;

        
        x[1] = j = 0;
        p = 1;  xp = 2;
        while (--i!=0) {                 
            x[xp] = (j += c[p]);
            xp++;
            p++;
        }

        
        i = 0; p = 0;
        do {
            if ((j = b[bindex+p]) != 0) {
                v[x[j]++] = i;
            }
            p++;
        } while (++i < n);
        n = x[g];                     

        
        x[0] = i = 0;                 
        p = 0;                        
        h = -1;                       
        w = -l;                       
        u[0] = 0;                     
        q = 0;                        
        z = 0;                        

        
        for (; k <= g; k++) {
            a = c[k];
            while (a--!=0) {
                
                
                while (k > w + l) {
                    h++;
                    w += l;                 
                    
                    z = g - w;
                    z = (z > l) ? l : z;        
                    if ((f=1<<(j=k-w))>a+1) {     
                        
                        f -= a + 1;               
                        xp = k;
                        if (j < z) {
                            while (++j < z) {        
                                if ((f <<= 1) <= c[++xp])
                                    break;              
                                f -= c[xp];           
                            }
                        }
                    }
                    z = 1 << j;                 

                    
                    if (hn[0] + z > MANY) {       
                        throw new ZError(ZError.Z_DATA_ERROR); 
                    }
                    u[h] = q =  hn[0];   
                    hn[0] += z;

                    
                    if(h!=0) {
                        x[h]=i;           
                        r[0]=(byte)j;     
                        r[1]=(byte)l;     
                        j=i>>>(w - l);
                        r[2] = (int)(q - u[h-1] - j);               
                        System.arraycopy(r, 0, hp, (u[h-1]+j)*3, 3); 
                    } else {
                        t[0] = q;               
                    }
                }

                
                r[1] = (byte)(k - w);
                if (p >= n){
                    r[0] = 128 + 64;      
                } else if (v[p] < s) {
                    r[0] = (byte)(v[p] < 256 ? 0 : 32 + 64);  
                    r[2] = v[p++];          
                } else {
                    r[0]=(byte)(e[v[p]-s]+16+64); 
                    r[2]=d[v[p++] - s];
                }

                
                f = 1 << (k - w);
                for (j = i >>> w; j < z; j += f) {
                    System.arraycopy(r, 0, hp, (q + j) * 3, 3);
                }

                
                for (j = 1 << (k - 1); (i & j)!=0; j >>>= 1) {
                    i ^= j;
                }
                i ^= j;

                
                mask = (1 << w) - 1;      
                while ((i & mask) != x[h]) {
                    h--;                    
                    w -= l;
                    mask = (1 << w) - 1;
                }
            }
        }
        
        if (y != 0 && g != 1) {
            ZStream.setMsg("incomplete some tree");
            throw new ZError(ZError.Z_BUF_ERROR);
        }
    }


    
    void inflate_trees_bits(int[] c, int[] bb, int[] tb, int[] hp) throws ZError {
        initWorkArea(19);
        hn[0] = 0;
        huft_build(c, 0, 19, 19, null, null, tb, bb, hp, hn, v);
        if (0 == bb[0]) {
            ZStream.setMsg("incomplete dynamic bit lengths tree");
            throw new ZError(ZError.Z_DATA_ERROR);
        }
    }

    void inflate_trees_dynamic(int nl,   
            int nd,   
            int[] c,  
            int[] bl, 
            int[] bd, 
            int[] tl, 
            int[] td, 
            int[] hp 
            ) throws ZError {

        
        initWorkArea(288);
        hn[0] = 0;
        huft_build(c, 0, nl, 257, cplens, cplext, tl, bl, hp, hn, v);
        if (0 == bl[0]) {
            ZStream.setMsg("incomplete literal/length tree");
            throw new ZError(ZError.Z_DATA_ERROR);
        }

        
        initWorkArea(288);
        huft_build(c, nl, nd, 0, cpdist, cpdext, td, bd, hp, hn, v);

        if (bd[0] == 0 && nl > 257) {
            ZStream.setMsg("empty distance tree with lengths");
            throw new ZError(ZError.Z_DATA_ERROR);
        }
    }

    private void initWorkArea(int vsize) {
        if (null == hn) {
            hn = new int[1];
            v = new int[vsize];
            c = new int[BMAX + 1];
            r = new int[3];
            u = new int[BMAX];
            x = new int[BMAX + 1];

        } else {
            if (v.length < vsize) {
                v = new int[vsize];
            } else {
                for (int i = 0; i < vsize; ++i) {v[i] = 0;}
            }
            for (int i = 0; i < BMAX + 1; ++i) {c[i] = 0;}
            for (int i = 0; i < 3; ++i) {r[i] = 0;}
            
            System.arraycopy(c, 0, u, 0, BMAX);
            
            System.arraycopy(c, 0, x, 0, BMAX+1);
        }
    }
}


