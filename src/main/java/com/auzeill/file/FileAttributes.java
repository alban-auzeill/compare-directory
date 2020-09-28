package com.auzeill.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;

public class FileAttributes {

  public enum Type {
    FILE("f"),
    DIRECTORY("d"),
    SYMBOLIC_LINK("l");

    public final String code;

    Type(String code) {
      this.code = code;
    }

    public static Type fromString(String code) {
      for (Type type : values()) {
        if (type.code.equals(code)) {
          return type;
        }
      }
      throw new IllegalArgumentException("Invalid code: " + code);
    }

    @Override
    public String toString() {
      return code;
    }

  }

  public final String relativeLinuxPath;
  public final Type type;
  public final long size;
  public final String owner;
  public final String group;
  public final String permissions;
  public final String modifiedTime;
  public final String sha1OrSymbolicLink;

  public FileAttributes(String relativeLinuxPath, Type type, long size, String owner, String group, String permissions, String modifiedTime, String sha1OrSymbolicLink) {
    this.relativeLinuxPath = relativeLinuxPath;
    this.type = type;
    this.size = size;
    this.owner = owner;
    this.group = group;
    this.permissions = permissions;
    this.modifiedTime = modifiedTime;
    this.sha1OrSymbolicLink = sha1OrSymbolicLink;
  }

  public static FileAttributes fromPath(StatContext context, Path path) throws IOException {
    boolean isRootPath = context.baseDirectory.equals(path);
    String relativeLinuxPath = isRootPath ? "." : normalize(context.baseDirectory.relativize(path));
    Type type;
    String sha1OrSymbolicLink = "";
    if (Files.isSymbolicLink(path)) {
      type = Type.SYMBOLIC_LINK;
      sha1OrSymbolicLink = normalize(Files.readSymbolicLink(path));
    } else if (Files.isDirectory(path)) {
      type = Type.DIRECTORY;
    } else if (Files.isRegularFile(path)) {
      type = Type.FILE;
    } else if (Files.exists(path)) {
      throw new IllegalArgumentException("Unsupported file type: " + path);
    } else {
      throw new IllegalArgumentException("File not found: " + path);
    }
    PosixFileAttributes posixAttributes = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    long size = posixAttributes.size();
    String owner = posixAttributes.owner().getName();
    String group = posixAttributes.group().getName();
    String permissions = PosixFilePermissions.toString(posixAttributes.permissions());
    String modifiedTime = posixAttributes.lastModifiedTime().toString(); // last time the file's content has been modified
    if (type == Type.FILE) {
      sha1OrSymbolicLink = resolveSha1(context, path, relativeLinuxPath, size, modifiedTime);
    }
    return new FileAttributes(relativeLinuxPath, type, size, owner, group, permissions, modifiedTime, sha1OrSymbolicLink);
  }

  private static String resolveSha1(StatContext context, Path path, String relativeLinuxPath, long size, String modifiedTime) throws IOException {
    if (context.computeSha1) {
      FileAttributes lastAttributes = context.lastFileAttributesMap.get(relativeLinuxPath);
      if (lastAttributes != null && !lastAttributes.sha1OrSymbolicLink.isEmpty() &&
        lastAttributes.type == Type.FILE && lastAttributes.size == size && lastAttributes.modifiedTime.equals(modifiedTime)) {
        return lastAttributes.sha1OrSymbolicLink;
      }
      return Sha1.digest(path);
    }
    return "";
  }

  public static FileAttributes fromString(String fileDescription) {
    String[] parts = fileDescription.split("\\|", -1);
    if (parts.length != 8) {
      throw new IllegalArgumentException("Invalid fileDescription: " + fileDescription);
    }
    String relativeLinuxPath = parts[0];
    Type type = Type.fromString(parts[1]);
    long size = Long.parseLong(parts[2]);
    String owner = parts[3];
    String group = parts[4];
    String permissions = parts[5];
    String modifiedTime = parts[6];
    String sha1OrSymbolicLink = parts[7];
    return new FileAttributes(relativeLinuxPath, type, size, owner, group, permissions, modifiedTime, sha1OrSymbolicLink);
  }

  @Override
  public String toString() {
    return relativeLinuxPath + "|" + type + "|" + size + "|" + owner + "|" + group + "|" + permissions + "|" + modifiedTime + "|" + sha1OrSymbolicLink;
  }

  private static String normalize(Path path) {
    String textPath = path.toString();
    if (File.separatorChar == '\\') {
      textPath = textPath.replace('\\', '/');
    }
    return textPath;
  }

  public static int comparePath(String path1, String path2) {
    if (path1.equals(path2)) {
      return 0;
    } else if (path1.equals(".")) {
      return +1;
    } else if (path2.equals(".")) {
      return -1;
    }
    int i = 0;
    while (i < path1.length() && i < path2.length()) {
      char ch1 = normalize(path1.charAt(i));
      char ch2 = normalize(path2.charAt(i));
      if (ch1 != ch2) {
        return comparePath(ch1, ch2);
      }
      i++;
    }
    if (path1.length() == path2.length()) {
      return 0;
    } else if (path1.length() < path2.length()) {
      return normalize(path2.charAt(i)) == '/' ? +1 : -1;
    } else {
      return normalize(path1.charAt(i)) == '/' ? -1 : +1;
    }
  }

  public static int comparePath(char ch1, char ch2) {
    if (ch1 == ch2) {
      return 0;
    } else  if (ch1 == '/') {
      return -1;
    } else if (ch2 == '/') {
      return +1;
    } else if (ch1 < ch2) {
      return -1;
    } else {
      return +1;
    }
  }

  private static char normalize(char ch) {
    return ch == '\\' ? '/' : ch;
  }

}
