package sawim.modules;


import DrawControls.icons.AniImageList;
import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import ru.sawim.General;
import sawim.comm.StringConvertor;
import protocol.net.TcpSocket;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

public final class Emotions {
    private ImageList images;

    private int[] selEmotionsIndexes;
    private String[] selEmotionsWord;
    private String[] selEmotionsSmileNames;

    private String smileChars;
    private int[] textCorrIndexes;
    private String[] textCorrWords;
    private boolean isAniSmiles = false;

    private Emotions() {}
    public static final Emotions instance = new Emotions();

    private static final int PARSER_NONE        = 0;
    private static final int PARSER_NUMBER      = 1;
    private static final int PARSER_MNAME       = 2;
    private static final int PARSER_NAME        = 3;
    private static final int PARSER_LONG_NAME   = 4;
    private static final int PARSER_FIRST_SMILE = 5;
    private static final int PARSER_SMILE       = 6;

    private void smileParser(String content, Vector textCorr, Vector selEmotions) {
        Integer curIndex = new Integer(0);
        String smileName = "";
        String word = "";
        int beginPos = 0;
        int state = PARSER_NONE;
        int len = content.length();
        for (int i = 0; i <= len; ++i) {
            char ch = (i < len) ? content.charAt(i) : '\n';
            if ('\r' == ch) continue;
            switch (state) {
                case PARSER_NONE:
                    if ('"' == ch) {
                        state = PARSER_LONG_NAME;
                        beginPos = i + 1;
                    } else if ((' ' == ch) || ('\n' == ch)) {
                    } else {
                        state = PARSER_NUMBER;
                        beginPos = i;
                    }
                    break;

                case PARSER_NUMBER:
                    if (' ' == ch) {
                        state = PARSER_MNAME;
                        smileName = content.substring(beginPos, i).trim();
                        beginPos = i + 1;
                        try {
                            Integer.parseInt(smileName);
                        } catch (Exception e) {
                            state = PARSER_FIRST_SMILE;
                        }
                    }
                    break;

                case PARSER_MNAME:
                    if ('"' == ch) {
                        state = PARSER_LONG_NAME;
                        beginPos = i + 1;
                    } else if (' ' == ch) {
                    } else {
                        state = PARSER_NAME;
                        beginPos = i;
                    }
                    break;

                case PARSER_NAME:
                    if (' ' == ch) {
                        state = PARSER_FIRST_SMILE;
                        smileName = content.substring(beginPos, i).trim();
                        beginPos = i + 1;
                    }
                    break;

                case PARSER_LONG_NAME:
                    if ('"' == ch) {
                        state = PARSER_FIRST_SMILE;
                        smileName = content.substring(beginPos, i).trim();
                        beginPos = i + 1;
                    }
                    break;


                case PARSER_FIRST_SMILE:
                    switch (ch) {
                        case ',':
                            word = content.substring(beginPos, i).trim();
                            if (word.length() != 0) {
                                state = PARSER_SMILE;
                                selEmotions.addElement(new Object[] {curIndex, word, smileName});
                                textCorr.addElement(new Object[] {word, curIndex});
                                if (smileName.length() == 0) {
                                    smileName = word;
                                }
                            }
                            beginPos = i + 1;
                            break;
                        case '\n':
                            state = PARSER_NONE;
                            word = content.substring(beginPos, i).trim();
                            if (word.length() != 0) {
                                selEmotions.addElement(new Object[] {curIndex, word, smileName});
                                textCorr.addElement(new Object[] {word, curIndex});
                                if (smileName.length() == 0) {
                                    smileName = word;
                                }
                            }
                            curIndex = new Integer(curIndex.intValue() + 1);
                            break;
                    }
                    break;

                case PARSER_SMILE:
                    switch (ch) {
                        case ',':
                            word = content.substring(beginPos, i).trim();
                            if ((0 < word.length()) && (word.length() < 30)) {
                                textCorr.addElement(new Object[] {word, curIndex});
                            }
                            beginPos = i + 1;
                            break;
                        case '\n':
                            state = PARSER_NONE;
                            word = content.substring(beginPos, i).trim();
                            if ((0 < word.length()) && (word.length() < 30)) {
                                textCorr.addElement(new Object[] {word, curIndex});
                            }
                            curIndex = new Integer(curIndex.intValue() + 1);
                            break;
                    }
                    break;
            }
        }
    }

    public void load() {
        boolean loaded = false;
        try {
            loaded = loadAll();
        } catch (Exception ex) {
        }
        if (!loaded) {
            selEmotionsIndexes    = null;
            selEmotionsWord       = null;
            selEmotionsSmileNames = null;
            images = null;
        }
    }

    private ImageList loadIcons(int iconsSize) throws IOException {
        ImageList emoImages = null;
        emoImages = new AniImageList();
        emoImages.load("/smiles", iconsSize, iconsSize);
        if (0 < emoImages.size()) {
            isAniSmiles = true;
            return emoImages;
        }
        emoImages = new ImageList();
        emoImages.load("/smiles.png", iconsSize, iconsSize);
        return emoImages;
    }

    private boolean loadAll() {
        images = null;
        Vector textCorr = new Vector();
        Vector selEmotions = new Vector();

        General.gc();
        long mem = Runtime.getRuntime().freeMemory();

        InputStream stream = null;
        stream = General.getResourceAsStream("/smiles/smiles.txt");
        if (null == stream) {
            stream = General.getResourceAsStream("/smiles.txt");
        }
        if (null == stream) {
            return false;
        }
        ImageList emoImages = null;
        try {
            DataInputStream dos = new DataInputStream(stream);
            int iconsSize = readIntFromStream(dos);
            emoImages = loadIcons(iconsSize);
            byte[] str = new byte[dos.available()];
            dos.read(str);
            String content = StringConvertor.utf8beByteArrayToString(str, 0, str.length);
            smileParser(content, textCorr, selEmotions);
            TcpSocket.close(dos);
        } catch (Exception e) {
        }
        TcpSocket.close(stream);
        if (0 == emoImages.size()) {
            return false;
        }
        int size = selEmotions.size();
        selEmotionsIndexes    = new int[size];
        selEmotionsWord       = new String[size];
        selEmotionsSmileNames = new String[size];
        for (int i = 0; i < size; ++i) {
            Object[] data            = (Object[])selEmotions.elementAt(i);
            selEmotionsIndexes[i]    = ((Integer)data[0]).intValue();
            selEmotionsWord[i]       = (String)data[1];
            selEmotionsSmileNames[i] = (String)data[2];
        }

        size = textCorr.size();
        textCorrWords   = new String[size];
        textCorrIndexes = new int[size];
        StringBuffer fisrtChars = new StringBuffer(textCorr.size());
        for (int i = 0; i < size; ++i) {
            Object[] data = (Object[])textCorr.elementAt(i);
            textCorrWords[i]   = (String)data[0];
            textCorrIndexes[i] = ((Integer)data[1]).intValue();

            fisrtChars.append(textCorrWords[i].charAt(0));
        }
        this.smileChars = fisrtChars.toString();

        DebugLog.println("Emotions used (full): "+(mem - Runtime.getRuntime().freeMemory()));
        selEmotions.removeAllElements();
        selEmotions = null;
        textCorr.removeAllElements();
        textCorr = null;
        General.gc();
        DebugLog.println("Emotions used: "+(mem - Runtime.getRuntime().freeMemory()));
        General.gc();
        images = emoImages;
        return true;
    }

    private int readIntFromStream(DataInputStream stream) throws IOException {
        int value = 0;
        byte digit = stream.readByte();
        while (digit >= '0' && digit <= '9') {
            value = 10 * value + (digit - '0');
            digit = stream.readByte();
        }
        while (digit != '\n') {
            digit = stream.readByte();
        }
        return value;
    }

    public String getSmileChars() {
        return smileChars;
    }

    public Icon getSmileIcon(int smileIndex) {
        return images.iconAt(textCorrIndexes[smileIndex]);
    }

    public String getSmileText(int smileIndex) {
        return textCorrWords[smileIndex];
    }

    public String getSmileCode(int smileIndex) {
        return selEmotionsWord[smileIndex];
    }

    public int count() {
        return selEmotionsIndexes.length;
    }

    public Icon getSmile(int smileIndex) {
        return images.iconAt(smileIndex);
    }

    public boolean isAniSmiles() {
        return isAniSmiles;
    }
}