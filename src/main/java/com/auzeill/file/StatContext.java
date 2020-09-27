package com.auzeill.file;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StatContext {

  public static final String DEFAULT_STATS_DIRECTORY = ".directory-stats";
  public static final String STATS_DIRECTORY_OPTION = "--stats-directory";
  private static final String NO_SHA1 = "--no-sha1";
  private static final String IGNORE = "--ignore";
  private static final String SAVE = "--save";
  private static final String DIFF = "--diff";

  public final Path baseDirectory;
  public final Path statsDirectory;
  public final Path rootPath;
  public final boolean computeSha1;
  public final boolean save;
  public final boolean diff;
  public final Set<String> ignoreSet;
  public final Map<String, FileAttributes> lastFileAttributesMap;
  public final List<String> previousPathsToDiff;

  public StatContext(Path baseDirectory, Path rootPath, boolean computeSha1) {
    this.baseDirectory = baseDirectory;
    this.statsDirectory = this.baseDirectory.resolve(StatContext.DEFAULT_STATS_DIRECTORY);
    this.rootPath = rootPath;
    this.computeSha1 = computeSha1;
    this.save = false;
    this.diff = false;
    this.ignoreSet = new HashSet<>();
    this.lastFileAttributesMap = new HashMap<>();
    this.previousPathsToDiff = Collections.emptyList();
  }

  public StatContext(String[] args) throws IOException {
    List<String> arguments = new ArrayList<>(Arrays.asList(args));
    this.computeSha1 = !arguments.remove(NO_SHA1);
    this.save = arguments.remove(SAVE);
    this.diff = arguments.remove(DIFF);
    this.ignoreSet = new HashSet<>();
    int ignorePos = arguments.lastIndexOf(IGNORE);
    while (ignorePos != -1 && ignorePos + 1 < arguments.size()) {
      ignoreSet.add(arguments.get(ignorePos + 1));
      arguments.remove(ignorePos + 1);
      arguments.remove(ignorePos);
      ignorePos = arguments.lastIndexOf(IGNORE);
    }
    String customStatsDirectory = "";
    int statsDirectoryOptionPos = arguments.lastIndexOf(STATS_DIRECTORY_OPTION);
    if (statsDirectoryOptionPos != -1 && statsDirectoryOptionPos + 1 < arguments.size()) {
      customStatsDirectory = arguments.get(statsDirectoryOptionPos + 1);
      arguments.remove(statsDirectoryOptionPos + 1);
      arguments.remove(statsDirectoryOptionPos);
    }
    if (arguments.isEmpty()) {
      this.rootPath = Paths.get(".").toRealPath(LinkOption.NOFOLLOW_LINKS);
    } else if (arguments.size() == 1) {
      this.rootPath = Paths.get(arguments.get(0)).toRealPath(LinkOption.NOFOLLOW_LINKS);
    } else {
      throw new IllegalArgumentException("Invalid arguments: " + String.join(" ", arguments));
    }
    if (Files.isDirectory(this.rootPath)) {
      this.baseDirectory = this.rootPath;
    } else {
      this.baseDirectory = this.rootPath.getParent();
    }
    if (!customStatsDirectory.isEmpty()) {
      this.statsDirectory = Paths.get(customStatsDirectory);
      this.ignoreSet.add(this.baseDirectory.relativize(this.statsDirectory).toString());
    } else {
      this.statsDirectory = this.baseDirectory.resolve(StatContext.DEFAULT_STATS_DIRECTORY);
      this.ignoreSet.add(StatContext.DEFAULT_STATS_DIRECTORY);
    }
    this.lastFileAttributesMap = new HashMap<>();
    if (Files.isDirectory(statsDirectory)) {
      Path ignorePath = statsDirectory.resolve("ignore");
      if (Files.exists(ignorePath)) {
        this.ignoreSet.addAll(Files.readAllLines(ignorePath, UTF_8));
      }
      Optional<String> lastStat = Files.list(statsDirectory)
        .map(p -> p.getFileName().toString())
        .filter(name -> name.startsWith("stat-"))
        .max(Comparator.comparing(Object::toString));
      if (lastStat.isPresent()) {
        Path lastStatPath = statsDirectory.resolve(lastStat.get());
        for (String line : Files.readAllLines(lastStatPath, UTF_8)) {
          FileAttributes attributes = FileAttributes.fromString(line);
          lastFileAttributesMap.put(attributes.relativeLinuxPath, attributes);
        }
      }
    }

    this.previousPathsToDiff = this.lastFileAttributesMap.keySet().stream()
      .sorted(Comparator.comparing(StatContext::pathToSort))
      .collect(Collectors.toCollection(LinkedList::new));
  }

  private static String pathToSort(String path) {
    if (path.equals(".")) {
      return "\uFFFF";
    }
    return path.replace('\\', '\uFFFF').replace('/', '\uFFFF');
  }

  public Path newStatSavedPath() {
    SimpleDateFormat format = new SimpleDateFormat("'stat'-yyyy.MM.dd-HH'h'mm'm'ss's'SSS", Locale.ROOT);
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    return statsDirectory.resolve(format.format(new Date()));
  }

  public boolean include(String relativeLinuxPath) {
    return !ignoreSet.contains(relativeLinuxPath);
  }

  public void printStats(PrintStream out, FileAttributes attributes) {
    if (diff) {
      Iterator<String> previousIterator = previousPathsToDiff.iterator();
      boolean compared = false;
      while (previousIterator.hasNext()) {
        FileAttributes prevAttributes = this.lastFileAttributesMap.get(previousIterator.next());
        int comp = pathToSort(prevAttributes.relativeLinuxPath).compareTo(pathToSort(attributes.relativeLinuxPath));
        if (comp < 0) {
          out.println("-del- " + prevAttributes.toString());
          previousIterator.remove();
        } else if (comp == 0) {
          compared = true;
          previousIterator.remove();
          if (!prevAttributes.toString().equals(attributes.toString())) {
            StringBuilder diffLine = new StringBuilder();
            diffLine.append("~mod~ ").append(attributes.relativeLinuxPath).append("|");
            if (prevAttributes.type != attributes.type) {
              diffLine.append(" type ").append(prevAttributes.type).append(" -> ").append(attributes.type).append(" |");
            }
            if (prevAttributes.size != attributes.size) {
              diffLine.append(" size ").append(prevAttributes.size).append(" -> ").append(attributes.size).append(" |");
            }
            if (!prevAttributes.owner.equals(attributes.owner)) {
              diffLine.append(" owner ").append(prevAttributes.owner).append(" -> ").append(attributes.owner).append(" |");
            }
            if (!prevAttributes.group.equals(attributes.group)) {
              diffLine.append(" group ").append(prevAttributes.group).append(" -> ").append(attributes.group).append(" |");
            }
            if (!prevAttributes.permissions.equals(attributes.permissions)) {
              diffLine.append(" permissions ").append(prevAttributes.permissions).append(" -> ").append(attributes.permissions).append(" |");
            }
            if (!prevAttributes.modifiedTime.equals(attributes.modifiedTime)) {
              diffLine.append(" modifiedTime ").append(prevAttributes.modifiedTime).append(" -> ").append(attributes.modifiedTime).append(" |");
            }
            if (!prevAttributes.sha1OrSymbolicLink.equals(attributes.sha1OrSymbolicLink)) {
              diffLine.append(" sha1OrSymbolicLink ").append(prevAttributes.sha1OrSymbolicLink).append(" -> ").append(attributes.sha1OrSymbolicLink).append(" |");
            }
            out.println(diffLine.toString());
          }
        } else {
          break;
        }
      }
      if (!compared) {
        out.println("+new+ " + attributes.toString());
      }
    } else {
      out.println(attributes.toString());
    }
  }

  public void exitStats(PrintStream out) {
    if (diff) {
      Iterator<String> previousIterator = previousPathsToDiff.iterator();
      while (previousIterator.hasNext()) {
        FileAttributes prevAttributes = this.lastFileAttributesMap.get(previousIterator.next());
        out.println("-del- " + prevAttributes.toString());
        previousIterator.remove();
      }
    }
  }

}
