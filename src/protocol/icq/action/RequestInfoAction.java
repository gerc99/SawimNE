


package protocol.icq.action;


import protocol.icq.IcqContact;
import protocol.icq.packet.FromIcqSrvPacket;
import protocol.icq.packet.Packet;
import protocol.icq.packet.ToIcqSrvPacket;
import ru.sawim.SawimException;
import ru.sawim.comm.ArrayReader;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.search.UserInfo;

public class RequestInfoAction extends IcqAction {


    private static final int TIMEOUT = 10;


    private UserInfo strData;


    private int packetCounter;
    private IcqContact contact;
    private boolean done = false;


    public RequestInfoAction(UserInfo data, IcqContact item) {
        super();
        packetCounter = 0;
        contact = item;
        strData = data;
        strData.uin = contact.getUserId();
    }

    public UserInfo getUserInfo() {
        return strData;
    }


    public void init() throws SawimException {


        Util stream = new Util();
        try {
            stream.writeWordLE(ToIcqSrvPacket.CLI_META_REQMOREINFO_TYPE);
            stream.writeDWordLE(Long.parseLong(strData.uin));
            sendPacket(new ToIcqSrvPacket(0, getIcq().getUserId(), ToIcqSrvPacket.CLI_META_SUBCMD, new byte[0], stream.toByteArray()));
        } catch (Exception ignored) {
            requestNew();
        }


        active();
    }

    private void requestNew() throws SawimException {
        Util stream = new Util();
        byte[] uin = strData.uin.getBytes();

        stream.writeWordLE(ToIcqSrvPacket.CLI_META_REQUEST_FULL_INFO);
        stream.writeWordLE(30 + uin.length);
        stream.writeWordBE(0x05b9);
        stream.writeWordBE(ToIcqSrvPacket.CLI_META_REQUEST_FULL_INFO);
        stream.writeDWordBE(0x00000000);
        stream.writeDWordBE(0x00000000);
        stream.writeDWordBE(0x04e30000);
        stream.writeDWordBE(0x00020003);
        stream.writeDWordBE(0x00000001);
        stream.writeWordBE(4 + uin.length);
        stream.writeWordBE(0x0032);
        stream.writeWordBE(uin.length);
        stream.writeByteArray(uin);
        sendPacket(new ToIcqSrvPacket(0, getIcq().getUserId(), ToIcqSrvPacket.CLI_META_SUBCMD, new byte[0], stream.toByteArray()));
    }


    private String readAsciiz(ArrayReader stream) {
        int len = stream.getWordLE();
        if (len == 0) {
            return "";
        }
        byte[] buffer = stream.getArray(len);

        return StringConvertor.byteArrayToWinString(buffer, 0, buffer.length).trim();
    }


    public boolean forward(Packet packet) throws SawimException {
        boolean consumed = false;


        if (packet instanceof FromIcqSrvPacket) {
            FromIcqSrvPacket fromIcqSrvPacket = (FromIcqSrvPacket) packet;


            if (fromIcqSrvPacket.getSubcommand() != FromIcqSrvPacket.SRV_META_SUBCMD) {
                return false;
            }


            ArrayReader stream = fromIcqSrvPacket.getReader();


            try {

                int type = stream.getWordLE();
                stream.getByte();
                if (FromIcqSrvPacket.SRV_META_FULL_INFO == type) {
                    stream.skip(5 * 2 + 21);
                    processFillInfo(stream);
                    return true;
                }
                switch (type) {
                    case FromIcqSrvPacket.SRV_META_GENERAL_TYPE: {
                        strData.nick = readAsciiz(stream);
                        strData.firstName = readAsciiz(stream);
                        strData.lastName = readAsciiz(stream);
                        strData.email = readAsciiz(stream);
                        strData.homeCity = readAsciiz(stream);
                        strData.homeState = readAsciiz(stream);
                        strData.homePhones = readAsciiz(stream);
                        strData.homeFax = readAsciiz(stream);
                        strData.homeAddress = readAsciiz(stream);
                        strData.cellPhone = readAsciiz(stream);
                        packetCounter++;
                        consumed = true;
                        break;
                    }

                    case 0x00DC: {
                        strData.age = stream.getWordLE();
                        strData.gender = (byte) stream.getByte();
                        strData.homePage = readAsciiz(stream);
                        int year = stream.getWordLE();
                        int mon = stream.getByte();
                        int day = stream.getByte();
                        strData.birthDay = (year != 0)
                                ? (day + "." + (mon < 10 ? "0" : "") + mon + "." + year)
                                : null;
                        packetCounter++;
                        consumed = true;
                        break;
                    }

                    case 0x00D2: {
                        strData.workCity = readAsciiz(stream);
                        strData.workState = readAsciiz(stream);
                        strData.workPhone = readAsciiz(stream);
                        strData.workFax = readAsciiz(stream);
                        strData.workAddress = readAsciiz(stream);

                        readAsciiz(stream);
                        stream.getWordLE();
                        strData.workCompany = readAsciiz(stream);
                        strData.workDepartment = readAsciiz(stream);
                        strData.workPosition = readAsciiz(stream);
                        packetCounter++;
                        consumed = true;
                        break;
                    }

                    case 0x00E6: {
                        strData.about = readAsciiz(stream);
                        packetCounter++;
                        consumed = true;
                        break;
                    }

                    case 0x00F0: {
                        StringBuilder sb = new StringBuilder();
                        int counter = stream.getByte();
                        for (int i = 0; i < counter; ++i) {
                            stream.getWordLE();
                            String item = readAsciiz(stream);
                            if (item.length() == 0) continue;
                            if (sb.length() != 0) sb.append(", ");
                            sb.append(item);
                        }
                        strData.interests = sb.toString();
                        packetCounter++;
                        consumed = true;
                        break;
                    }
                }

            } catch (Exception e) {

                DebugLog.panic("Request Info action", e);

            }
            if (packetCounter >= 5) {
                requestNew();
            }


            strData.setOptimalName();
            strData.updateProfileView();
        }

        return consumed;
    }

    private void processFillInfo(ArrayReader stream) {
        done = true;
        int len = stream.getWordBE();
        int offset = stream.getOffset();
        strData.nick = str(stream.getTlvData(0x0046, offset, len));
        strData.firstName = str(stream.getTlvData(0x0064, offset, len));
        strData.lastName = str(stream.getTlvData(0x006e, offset, len));
        strData.gender = stream.getTlvData(0x0082, offset, len)[0];
        strData.homePage = str(stream.getTlvData(0x00fa, offset, len));
        strData.about = str(stream.getTlvData(0x0186, offset, len));
        strData.homeState = getTvlData(0x0096, 0x0078, stream, offset, len);
        strData.homeCity = getTvlData(0x0096, 0x0064, stream, offset, len);
        strData.homeAddress = getTvlData(0x0096, 0x006e, stream, offset, len);
        if (StringConvertor.isEmpty(strData.homeCity) && StringConvertor.isEmpty(strData.homeAddress)) {
            strData.homeCity = getTvlData(0x00a0, 0x0064, stream, offset, len);
            strData.homeAddress = getTvlData(0x00a0, 0x006e, stream, offset, len);
        }
        strData.workState = getTvlData(0x0118, 0x00be, stream, offset, len);
        strData.workCity = getTvlData(0x0118, 0x00b4, stream, offset, len);
        strData.workDepartment = getTvlData(0x0118, 0x007D, stream, offset, len);
        strData.workCompany = getTvlData(0x0118, 0x006e, stream, offset, len);
        strData.workPosition = getTvlData(0x0118, 0x0064, stream, offset, len);

        strData.setOptimalName();
        strData.updateProfileView();


    }

    private String getTvlData(int type, int subtype, ArrayReader stream, int offset, int len) {
        byte[] data = stream.getTlvData(type, offset, len);
        if (null == data) return null;
        if (0 == subtype) return str(data);
        ArrayReader sub = new ArrayReader(data, 2);
        int subLen = sub.getWordBE();
        data = sub.getTlvData(subtype, 4, subLen);
        if (null == data) return null;
        return str(data);
    }

    private String str(byte[] data) {
        if (null == data) return null;
        return StringConvertor.utf8beByteArrayToString(data, 0, data.length).trim();
    }


    public boolean isCompleted() {
        return done;
    }


    public boolean isError() {
        return isNotActive(TIMEOUT);
    }


}


