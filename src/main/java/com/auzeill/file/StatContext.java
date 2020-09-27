package com.auzeill.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StatContext {

  public static final String DEFAULT_STATS_DIRECTORY = ".directory-stats";
  public static final String STATS_DIRECTORY_OPTION = "--stats-directory";
  private static final String NO_SHA1 = "--no-sha1";
  private static final String IGNORE = "--ignore";
  private static final String SAVE = "--save";

  public final Path baseDirectory;
  public final Path statsDirectory;
  public final Path rootPath;
  public final boolean computeSha1;
  public final boolean save;
  public final Set<String> ignoreSet;
  public final Map<String, FileAttributes> lastFileAttributesMap;

  public StatContext(Path baseDirectory, Path rootPath, boolean computeSha1) {
    this.baseDirectory = baseDirectory;
    this.statsDirectory = this.baseDirectory.resolve(StatContext.DEFAULT_STATS_DIRECTORY);
    this.rootPath = rootPath;
    this.computeSha1 = computeSha1;
    this.save = false;
    this.ignoreSet = new HashSet<>();
    this.lastFileAttributesMap = new HashMap<>();
  }

  public StatContext(String[] args) throws IOException {
    List<String> arguments = new ArrayList<>(Arrays.asList(args));
    this.computeSha1 = !arguments.remove(NO_SHA1);
    this.save = arguments.remove(SAVE);
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
  }

  public Path newStatSavedPath() {
    SimpleDateFormat format = new SimpleDateFormat("'stat'-yyyy.MM.dd-HH'h'mm'm'ss's'SSS", Locale.ROOT);
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    return statsDirectory.resolve(format.format(new Date()));
  }

  public boolean include(String relativeLinuxPath) {
    return !ignoreSet.contains(relativeLinuxPath);
  }

}
