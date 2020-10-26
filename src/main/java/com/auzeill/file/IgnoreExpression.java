package com.auzeill.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public interface IgnoreExpression {

  boolean ignore(Path absolutePath, String relativePath);

  class Matcher implements IgnoreExpression {

    private final List<Function<String, IgnoreExpression>> SUPPORTED_MATCHERS = Arrays.asList(
      EndWith::create,
      ConditionalSibling::create);

    public final Set<String> ignoredRelativePaths = new HashSet<>();
    public final List<IgnoreExpression> advancedMatchers = new ArrayList<>();

    public void add(String relativePath) {
      if (relativePath.isBlank() || relativePath.startsWith("#")) {
        return;
      }
      for (Function<String, IgnoreExpression> candidate : SUPPORTED_MATCHERS) {
        IgnoreExpression matcher = candidate.apply(relativePath);
        if (matcher != null) {
          advancedMatchers.add(matcher);
          return;
        }
      }
      ignoredRelativePaths.add(relativePath);
    }

    public boolean ignore(Path absolutePath, String relativePath) {
      return ignoredRelativePaths.contains(relativePath) ||
        advancedMatchers.stream().anyMatch(matcher -> matcher.ignore(absolutePath, relativePath));
    }

  }

  class EndWith implements IgnoreExpression {

    public final String suffix;

    public EndWith(String suffix) {
      this.suffix = suffix;
    }

    public static IgnoreExpression create(String pattern) {
      if (pattern.startsWith("*")) {
        return new EndWith(pattern.substring(1));
      }
      return null;
    }

    public boolean ignore(Path absolutePath, String relativePath) {
      return relativePath.endsWith(suffix);
    }

  }

  class ConditionalSibling implements IgnoreExpression {

    private static final String PREFIX = "(?sibling:";
    public final String siblingPattern;
    public final String relativePathPattern;

    public ConditionalSibling(String siblingPattern, String relativePath) {
      this.siblingPattern = siblingPattern;
      this.relativePathPattern = relativePath;
    }

    public static IgnoreExpression create(String pattern) {
      if (pattern.startsWith(PREFIX)) {
        int separator = pattern.indexOf(')', PREFIX.length());
        if (separator != -1) {
          return new ConditionalSibling(pattern.substring(PREFIX.length(), separator), pattern.substring(separator + 1));
        }
      }
      return null;
    }

    public boolean ignore(Path absolutePath, String relativePath) {
      if (!absolutePath.getFileName().toString().equals(relativePathPattern)) {
        return false;
      }
      Path parent = absolutePath.getParent();
      return parent != null && Files.exists(parent.resolve(siblingPattern));
    }

  }

}
