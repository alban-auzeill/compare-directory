package com.auzeill.file;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Stats {

  public static void main(String[] args) throws IOException {
    stats(System.out, args);
  }

  public static void stats(PrintStream out, String[] args) throws IOException {
    StatContext context = new StatContext(args);
    if (context.save) {
      Path outPath = context.newStatSavedPath();
      Files.createDirectories(outPath.getParent());
      try(FileOutputStream fileOutputStream = new FileOutputStream(outPath.toFile())) {
        try (PrintStream outFile = new PrintStream(new BufferedOutputStream(fileOutputStream))) {
          stats(outFile, context, context.rootPath);
        }
      }
      out.println(outPath.toString());
    } else {
      stats(out, context, context.rootPath);
    }
  }

  public static FileAttributes stats(PrintStream out, StatContext context, Path path) throws IOException {
    if (path.getFileName().toString().equals(StatContext.DIRECTORY_STATS)) {
      return null;
    }
    FileAttributes attributes = FileAttributes.fromPath(context, path);
    if (context.include(attributes.relativeLinuxPath)) {
      if (attributes.type == FileAttributes.Type.DIRECTORY) {
        long size = 0;
        StringBuilder allSha1 = new StringBuilder();
        List<Path> childPaths = Files.list(path).sorted(Comparator.comparing(p -> p.getFileName().toString())).collect(Collectors.toList());
        for (Path childPath : childPaths) {
          FileAttributes childAttributes = stats(out, context, childPath);
          if (childAttributes != null) {
            size += childAttributes.size;
            allSha1.append(childAttributes.sha1OrSymbolicLink);
          }
        }
        attributes = new FileAttributes(
          attributes.relativeLinuxPath,
          attributes.type,
          size,
          attributes.owner,
          attributes.group,
          attributes.permissions,
          attributes.modifiedTime,
          context.computeSha1 ? Sha1.digest(allSha1.toString().getBytes(UTF_8)) : "");
      }
      out.println(attributes.toString());
      return attributes;
    } else {
      return null;
    }
  }

}
