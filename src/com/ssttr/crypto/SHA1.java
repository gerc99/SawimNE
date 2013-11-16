

package com.ssttr.crypto;


public final class SHA1 extends MessageDigest {
    private int state[] = new int[5];
    private long count;


    public SHA1() {
        state = new int[5];
        count = 0;
        if (block == null)
            block = new int[16];
        digestBits = new byte[20];
        digestValid = false;
    }


    private int block[] = new int[16];
    private int blockIndex;


    final int rol(int value, int bits) {
        int q = (value << bits) | (value >>> (32 - bits));
        return q;
    }

    final int blk0(int i) {
        block[i] = (rol(block[i], 24) & 0xFF00FF00) | (rol(block[i], 8) & 0x00FF00FF);
        return block[i];
    }

    final int blk(int i) {
        block[i & 15] = rol(block[(i + 13) & 15] ^ block[(i + 8) & 15] ^
                block[(i + 2) & 15] ^ block[i & 15], 1);
        return (block[i & 15]);
    }

    final void R0(int data[], int v, int w, int x, int y, int z, int i) {
        data[z] += ((data[w] & (data[x] ^ data[y])) ^ data[y]) +
                blk0(i) + 0x5A827999 + rol(data[v], 5);
        data[w] = rol(data[w], 30);
    }

    final void R1(int data[], int v, int w, int x, int y, int z, int i) {
        data[z] += ((data[w] & (data[x] ^ data[y])) ^ data[y]) +
                blk(i) + 0x5A827999 + rol(data[v], 5);
        data[w] = rol(data[w], 30);
    }

    final void R2(int data[], int v, int w, int x, int y, int z, int i) {
        data[z] += (data[w] ^ data[x] ^ data[y]) +
                blk(i) + 0x6ED9EBA1 + rol(data[v], 5);
        data[w] = rol(data[w], 30);
    }

    final void R3(int data[], int v, int w, int x, int y, int z, int i) {
        data[z] += (((data[w] | data[x]) & data[y]) | (data[w] & data[x])) +
                blk(i) + 0x8F1BBCDC + rol(data[v], 5);
        data[w] = rol(data[w], 30);
    }

    final void R4(int data[], int v, int w, int x, int y, int z, int i) {
        data[z] += (data[w] ^ data[x] ^ data[y]) +
                blk(i) + 0xCA62C1D6 + rol(data[v], 5);
        data[w] = rol(data[w], 30);
    }


    int dd[] = new int[5];


    void transform() {


        dd[0] = state[0];
        dd[1] = state[1];
        dd[2] = state[2];
        dd[3] = state[3];
        dd[4] = state[4];

        R0(dd, 0, 1, 2, 3, 4, 0);
        R0(dd, 4, 0, 1, 2, 3, 1);
        R0(dd, 3, 4, 0, 1, 2, 2);
        R0(dd, 2, 3, 4, 0, 1, 3);
        R0(dd, 1, 2, 3, 4, 0, 4);
        R0(dd, 0, 1, 2, 3, 4, 5);
        R0(dd, 4, 0, 1, 2, 3, 6);
        R0(dd, 3, 4, 0, 1, 2, 7);
        R0(dd, 2, 3, 4, 0, 1, 8);
        R0(dd, 1, 2, 3, 4, 0, 9);
        R0(dd, 0, 1, 2, 3, 4, 10);
        R0(dd, 4, 0, 1, 2, 3, 11);
        R0(dd, 3, 4, 0, 1, 2, 12);
        R0(dd, 2, 3, 4, 0, 1, 13);
        R0(dd, 1, 2, 3, 4, 0, 14);
        R0(dd, 0, 1, 2, 3, 4, 15);
        R1(dd, 4, 0, 1, 2, 3, 16);
        R1(dd, 3, 4, 0, 1, 2, 17);
        R1(dd, 2, 3, 4, 0, 1, 18);
        R1(dd, 1, 2, 3, 4, 0, 19);
        R2(dd, 0, 1, 2, 3, 4, 20);
        R2(dd, 4, 0, 1, 2, 3, 21);
        R2(dd, 3, 4, 0, 1, 2, 22);
        R2(dd, 2, 3, 4, 0, 1, 23);
        R2(dd, 1, 2, 3, 4, 0, 24);
        R2(dd, 0, 1, 2, 3, 4, 25);
        R2(dd, 4, 0, 1, 2, 3, 26);
        R2(dd, 3, 4, 0, 1, 2, 27);
        R2(dd, 2, 3, 4, 0, 1, 28);
        R2(dd, 1, 2, 3, 4, 0, 29);
        R2(dd, 0, 1, 2, 3, 4, 30);
        R2(dd, 4, 0, 1, 2, 3, 31);
        R2(dd, 3, 4, 0, 1, 2, 32);
        R2(dd, 2, 3, 4, 0, 1, 33);
        R2(dd, 1, 2, 3, 4, 0, 34);
        R2(dd, 0, 1, 2, 3, 4, 35);
        R2(dd, 4, 0, 1, 2, 3, 36);
        R2(dd, 3, 4, 0, 1, 2, 37);
        R2(dd, 2, 3, 4, 0, 1, 38);
        R2(dd, 1, 2, 3, 4, 0, 39);
        R3(dd, 0, 1, 2, 3, 4, 40);
        R3(dd, 4, 0, 1, 2, 3, 41);
        R3(dd, 3, 4, 0, 1, 2, 42);
        R3(dd, 2, 3, 4, 0, 1, 43);
        R3(dd, 1, 2, 3, 4, 0, 44);
        R3(dd, 0, 1, 2, 3, 4, 45);
        R3(dd, 4, 0, 1, 2, 3, 46);
        R3(dd, 3, 4, 0, 1, 2, 47);
        R3(dd, 2, 3, 4, 0, 1, 48);
        R3(dd, 1, 2, 3, 4, 0, 49);
        R3(dd, 0, 1, 2, 3, 4, 50);
        R3(dd, 4, 0, 1, 2, 3, 51);
        R3(dd, 3, 4, 0, 1, 2, 52);
        R3(dd, 2, 3, 4, 0, 1, 53);
        R3(dd, 1, 2, 3, 4, 0, 54);
        R3(dd, 0, 1, 2, 3, 4, 55);
        R3(dd, 4, 0, 1, 2, 3, 56);
        R3(dd, 3, 4, 0, 1, 2, 57);
        R3(dd, 2, 3, 4, 0, 1, 58);
        R3(dd, 1, 2, 3, 4, 0, 59);
        R4(dd, 0, 1, 2, 3, 4, 60);
        R4(dd, 4, 0, 1, 2, 3, 61);
        R4(dd, 3, 4, 0, 1, 2, 62);
        R4(dd, 2, 3, 4, 0, 1, 63);
        R4(dd, 1, 2, 3, 4, 0, 64);
        R4(dd, 0, 1, 2, 3, 4, 65);
        R4(dd, 4, 0, 1, 2, 3, 66);
        R4(dd, 3, 4, 0, 1, 2, 67);
        R4(dd, 2, 3, 4, 0, 1, 68);
        R4(dd, 1, 2, 3, 4, 0, 69);
        R4(dd, 0, 1, 2, 3, 4, 70);
        R4(dd, 4, 0, 1, 2, 3, 71);
        R4(dd, 3, 4, 0, 1, 2, 72);
        R4(dd, 2, 3, 4, 0, 1, 73);
        R4(dd, 1, 2, 3, 4, 0, 74);
        R4(dd, 0, 1, 2, 3, 4, 75);
        R4(dd, 4, 0, 1, 2, 3, 76);
        R4(dd, 3, 4, 0, 1, 2, 77);
        R4(dd, 2, 3, 4, 0, 1, 78);
        R4(dd, 1, 2, 3, 4, 0, 79);

        state[0] += dd[0];
        state[1] += dd[1];
        state[2] += dd[2];
        state[3] += dd[3];
        state[4] += dd[4];
    }


    public void init() {

        state[0] = 0x67452301;
        state[1] = 0xEFCDAB89;
        state[2] = 0x98BADCFE;
        state[3] = 0x10325476;
        state[4] = 0xC3D2E1F0;
        count = 0;
        digestBits = new byte[20];
        digestValid = false;
        blockIndex = 0;
    }


    public synchronized void update(byte b) {
        int mask = (8 * (blockIndex & 3));

        count += 8;
        block[blockIndex >> 2] &= ~(0xff << mask);
        block[blockIndex >> 2] |= (b & 0xff) << mask;
        blockIndex++;
        if (blockIndex == 64) {
            transform();
            blockIndex = 0;
        }
    }


    public synchronized void update(byte input[]) {
        update(input, 0, input.length);
    }


    public void finish() {
        byte bits[] = new byte[8];
        int i;

        for (i = 0; i < 8; i++) {
            bits[i] = (byte) ((count >>> (((7 - i) * 8))) & 0xff);
        }

        update((byte) 128);
        while (blockIndex != 56)
            update((byte) 0);

        update(bits);
        for (i = 0; i < 20; i++) {
            digestBits[i] = (byte)
                    ((state[i >> 2] >> ((3 - (i & 3)) * 8)) & 0xff);
        }
        digestValid = true;
    }


    public String getAlg() {
        return "sha-1";
    }


}

