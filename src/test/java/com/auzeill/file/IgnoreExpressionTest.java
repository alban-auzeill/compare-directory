package com.auzeill.file;

import com.auzeill.file.IgnoreExpression.EndWith;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IgnoreExpressionTest {

  @Test
  void matcher() {
    IgnoreExpression.Matcher matcher = new IgnoreExpression.Matcher();
    assertThat(matcher.ignore(Paths.get("parent", "file.txt"), "file.txt")).isFalse();
    matcher.add("file");
    assertThat(matcher.ignore(Paths.get("parent", "file.txt"), "file.txt")).isFalse();
    matcher.add("file.txt");
    assertThat(matcher.ignore(Paths.get("parent", "file.txt"), "file.txt")).isTrue();

    assertThat(matcher.ignore(Paths.get("parent", "sub", "file.txt"), "sub/file.txt")).isFalse();
    matcher.add("*/file.txt");
    assertThat(matcher.ignore(Paths.get("parent", "sub", "file.txt"), "sub/file.txt")).isTrue();

    assertThat(matcher.ignore(Paths.get("src", "test", "resources", "subfolder"), "subfolder")).isFalse();
    matcher.add("(?sibling:unknown.txt)subfolder");
    assertThat(matcher.ignore(Paths.get("src", "test", "resources", "subfolder"), "subfolder")).isFalse();
    matcher.add("(?sibling:data.txt)subfolder");
    assertThat(matcher.ignore(Paths.get("src", "test", "resources", "subfolder"), "subfolder")).isTrue();
  }

  @Test
  void end_with() {
    assertThat(EndWith.create("not-end-with")).isNull();
    assertThat(EndWith.create("*.txt")).isNotNull();
    assertThat(EndWith.create("*.txt").ignore(Paths.get("/parent/file.txt"), "file.txt")).isTrue();
    assertThat(EndWith.create("*.txt").ignore(Paths.get("/parent/file-txt"), "file-txt")).isFalse();
  }

}
