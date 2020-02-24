package com.zhaoxi.Open_source_Android.web3.rlp;

import java.util.ArrayList;

public class RlpDecoder {

    public static int OFFSET_SHORT_STRING = 0x80;

    public static int OFFSET_LONG_STRING = 0xb7;

    public static int OFFSET_SHORT_LIST = 0xc0;

    public static int OFFSET_LONG_LIST = 0xf7;


    /**
     * Parse wire byte[] message into RLP elements.
     *
     * @param rlpEncoded - RLP encoded byte-array
     * @return recursive RLP structure
     */
    public static RlpList decode(byte[] rlpEncoded) {
        RlpList rlpList = new RlpList(new ArrayList<RlpType>());
        traverse(rlpEncoded, 0, rlpEncoded.length, rlpList);
        return rlpList;
    }

    private static void traverse(byte[] data, int startPos, int endPos, RlpList rlpList) {

        try {
            if (data == null || data.length == 0) {
                return;
            }

            while (startPos < endPos) {

                int prefix = data[startPos] & 0xff;

                if (prefix < OFFSET_SHORT_STRING) {

                    // 1. the data is a string if the range of the
                    // first byte(i.e. prefix) is [0x00, 0x7f],
                    // and the string is the first byte itself exactly;

                    byte[] rlpData = {(byte) prefix};
                    rlpList.getValues().add(RlpString.create(rlpData));
                    startPos += 1;

                } else if (prefix == OFFSET_SHORT_STRING) {

                    // null
                    rlpList.getValues().add(RlpString.create(new byte[0]));
                    startPos += 1;

                } else if (prefix > OFFSET_SHORT_STRING && prefix <= OFFSET_LONG_STRING) {

                    // 2. the data is a string if the range of the
                    // first byte is [0x80, 0xb7], and the string
                    // which length is equal to the first byte minus 0x80
                    // follows the first byte;

                    byte strLen = (byte) (prefix - OFFSET_SHORT_STRING);

                    byte[] rlpData = new byte[strLen];
                    System.arraycopy(data, startPos + 1, rlpData, 0, strLen);

                    rlpList.getValues().add(RlpString.create(rlpData));
                    startPos += 1 + strLen;

                } else if (prefix > OFFSET_LONG_STRING && prefix < OFFSET_SHORT_LIST) {

                    // 3. the data is a string if the range of the
                    // first byte is [0xb8, 0xbf], and the length of the
                    // string which length in bytes is equal to the
                    // first byte minus 0xb7 follows the first byte,
                    // and the string follows the length of the string;

                    byte lenOfStrLen = (byte) (prefix - OFFSET_LONG_STRING);
                    int strLen = calcLength(lenOfStrLen, data, startPos);

                    // now we can parse an item for data[1]..data[length]
                    byte[] rlpData = new byte[strLen];
                    System.arraycopy(data, startPos + lenOfStrLen + 1, rlpData, 0, strLen);

                    rlpList.getValues().add(RlpString.create(rlpData));
                    startPos += lenOfStrLen + strLen + 1;

                } else if (prefix >= OFFSET_SHORT_LIST && prefix <= OFFSET_LONG_LIST) {

                    // 4. the data is a list if the range of the
                    // first byte is [0xc0, 0xf7], and the concatenation of
                    // the RLP encodings of all items of the list which the
                    // total payload is equal to the first byte minus 0xc0 follows the first byte;

                    byte listLen = (byte) (prefix - OFFSET_SHORT_LIST);

                    RlpList newLevelList = new RlpList(new ArrayList<RlpType>());
                    traverse(data, startPos + 1, startPos + listLen + 1, newLevelList);
                    rlpList.getValues().add(newLevelList);

                    startPos += 1 + listLen;

                } else if (prefix > OFFSET_LONG_LIST) {

                    // 5. the data is a list if the range of the
                    // first byte is [0xf8, 0xff], and the total payload of the
                    // list which length is equal to the
                    // first byte minus 0xf7 follows the first byte,
                    // and the concatenation of the RLP encodings of all items of
                    // the list follows the total payload of the list;

                    byte lenOfListLen = (byte) (prefix - OFFSET_LONG_LIST);
                    int listLen = calcLength(lenOfListLen, data, startPos);

                    RlpList newLevelList = new RlpList(new ArrayList<RlpType>());
                    traverse(data, startPos + lenOfListLen + 1,
                            startPos + lenOfListLen + listLen + 1, newLevelList);
                    rlpList.getValues().add(newLevelList);

                    startPos += lenOfListLen + listLen + 1;
                }

            }
        } catch (Exception e) {
            throw new RuntimeException("RLP wrong encoding", e);
        }
    }

    private static int calcLength(int lengthOfLength, byte[] data, int pos) {
        byte pow = (byte) (lengthOfLength - 1);
        int length = 0;
        for (int i = 1; i <= lengthOfLength; ++i) {
            length += (data[pos + i] & 0xff) << (8 * pow);
            pow--;
        }
        return length;
    }

}
