package com.example.utils;

import com.yubico.webauthn.data.ByteArray;

public class BytesUtil {

  public static byte[] longToBytes(long longValue) {
    long l = longValue;
    byte[] result = new byte[8];
    for (int i = 7; i >= 0; i--) {
      result[i] = (byte) (l & 0xFF);
      l >>= 8;
    }
    return result;
  }

  public static long bytesToLong(byte[] b) {
    long result = 0;
    for (int i = 0; i < 8; i++) {
      result <<= 8;
      result |= b[i] & 0xFF;
    }
    return result;
  }

  // Convert byte array to ByteArray
  public static ByteArray bytesToByteArray(byte[] bytes) {
    return new ByteArray(bytes);
  }

  // Convert ByteArray to byte array
  public static byte[] byteArrayToBytes(ByteArray byteArray) {
    return byteArray.getBytes();
  }

}
