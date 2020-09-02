package com.auzeill.file;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class Sha1 {

  public static String digest(byte[] data) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
      byte[] sha1 = messageDigest.digest(data);
      return byteToHex(sha1);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String digest(Path path) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      InputStream fis = new FileInputStream(path.toFile());
      int n = 0;
      byte[] buffer = new byte[8192];
      while (n != -1) {
        n = fis.read(buffer);
        if (n > 0) {
          digest.update(buffer, 0, n);
        }
      }
      return byteToHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String byteToHex(final byte[] data) {
    Formatter formatter = new Formatter();
    for (byte b : data) {
      formatter.format("%02x", b);
    }
    String result = formatter.toString();
    formatter.close();
    return result;
  }

}
