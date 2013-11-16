


package com.jcraft.jzlib;


final class Inflate {

    static final public int MAX_WBITS = 15;


    static final private int PRESET_DICT = 0x20;


    static final private int Z_DEFLATED = 8;

    static final private int Z_OK = 0;
    static final private int Z_STREAM_END = 1;
    static final private int Z_BUF_ERROR = -5;

    static final private int METHOD = 0;
    static final private int FLAG = 1;
    static final private int DICT4 = 2;
    static final private int DICT3 = 3;
    static final private int DICT2 = 4;
    static final private int DICT1 = 5;
    static final private int DICT0 = 6;
    static final private int BLOCKS = 7;
    static final private int CHECK4 = 8;
    static final private int CHECK3 = 9;
    static final private int CHECK2 = 10;
    static final private int CHECK1 = 11;
    static final private int DONE = 12;

    private int mode;


    private int method;


    private long adlerHash;
    private long need;


    private int nowrap;
    private int wbits;

    private InfBlocks blocks;

    public Inflate() {
    }

    private int inflateReset() {
        mode = nowrap != 0 ? BLOCKS : METHOD;
        blocks.reset();
        return Z_OK;
    }

    int inflateInit(int w, ZBuffers z) {


        nowrap = 0;
        if (w < 0) {
            w = -w;
            nowrap = 1;
        }


        if (w < 8 || w > 15) {
            return JZlib.Z_STREAM_ERROR;
        }
        wbits = w;

        blocks = new InfBlocks(z, 0 == nowrap, 1 << w);


        inflateReset();
        return Z_OK;
    }

    int getErrCode() {
        return blocks.result;
    }

    void inflate(ZBuffers z) throws ZError {
        if (null == z.next_in) {
            throw new ZError(ZError.Z_STREAM_ERROR);
        }

        blocks.result = Z_BUF_ERROR;
        while (true) {
            switch (mode) {
                case METHOD:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;

                    z.avail_in--;
                    method = z.next_in[z.next_in_index++];
                    if (Z_DEFLATED != (method & 0xf)) {
                        ZStream.setMsg("unknown compression method");
                        throw new ZError(ZError.Z_DATA_ERROR);
                    }
                    if ((method >> 4) + 8 > wbits) {
                        ZStream.setMsg("invalid window size");
                        throw new ZError(ZError.Z_DATA_ERROR);
                    }
                    mode = FLAG;

                case FLAG:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;

                    z.avail_in--;
                    int b = z.next_in[z.next_in_index++] & 0xff;

                    if ((((method << 8) + b) % 31) != 0) {
                        ZStream.setMsg("incorrect header check");
                        throw new ZError(ZError.Z_DATA_ERROR);
                    }

                    if ((b & PRESET_DICT) == 0) {
                        mode = BLOCKS;
                        break;
                    }
                    mode = DICT4;

                case DICT4:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;

                    z.avail_in--;
                    need = ((z.next_in[z.next_in_index++] & 0xff) << 24) & 0xff000000L;
                    mode = DICT3;

                case DICT3:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;

                    z.avail_in--;
                    need += ((z.next_in[z.next_in_index++] & 0xff) << 16) & 0xff0000L;
                    mode = DICT2;

                case DICT2:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;

                    z.avail_in--;
                    need += ((z.next_in[z.next_in_index++] & 0xff) << 8) & 0xff00L;
                    mode = DICT1;

                case DICT1:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;

                    z.avail_in--;
                    need += (z.next_in[z.next_in_index++] & 0xffL);

                    mode = DICT0;
                    throw new ZError(ZError.Z_NEED_DICT);

                case DICT0:
                    ZStream.setMsg("need dictionary");
                    throw new ZError(ZError.Z_STREAM_ERROR);

                case BLOCKS:
                    blocks.proc();
                    if (blocks.result != Z_STREAM_END) {
                        return;
                    }
                    blocks.result = Z_OK;
                    adlerHash = blocks.getAdlerHash();
                    blocks.reset();
                    if (nowrap != 0) {
                        mode = DONE;
                        break;
                    }
                    mode = CHECK4;

                case CHECK4:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;

                    z.avail_in--;
                    need = ((z.next_in[z.next_in_index++] & 0xff) << 24) & 0xff000000L;
                    mode = CHECK3;

                case CHECK3:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;

                    z.avail_in--;
                    need += ((z.next_in[z.next_in_index++] & 0xff) << 16) & 0xff0000L;
                    mode = CHECK2;

                case CHECK2:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;

                    z.avail_in--;
                    need += ((z.next_in[z.next_in_index++] & 0xff) << 8) & 0xff00L;
                    mode = CHECK1;

                case CHECK1:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;

                    z.avail_in--;
                    need += (z.next_in[z.next_in_index++] & 0xffL);

                    if (adlerHash != need) {
                        ZStream.setMsg("incorrect data check");
                        throw new ZError(ZError.Z_DATA_ERROR);
                    }
                    mode = DONE;

                case DONE:
                    blocks.result = Z_STREAM_END;
                    return;

                default:
                    throw new ZError(ZError.Z_STREAM_ERROR);
            }
        }
    }
}


