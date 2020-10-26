package com.auzeill.file;

import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatContextTest {

  @Test
  void no_argument() throws IOException {
    StatContext context = new StatContext(new String[] {});
    assertThat(context.baseDirectory.toString()).isEqualTo(Paths.get(".").toRealPath().toString());
    assertThat(context.computeSha1).isEqualTo(true);
  }

  @Test
  void one_argument() throws IOException {
    StatContext context = new StatContext(new String[] {"src"});
    assertThat(context.baseDirectory.toString()).isEqualTo(Paths.get("src").toRealPath().toString());
    assertThat(context.computeSha1).isEqualTo(true);
    assertThat(context.save).isEqualTo(false);
    assertThat(context.diff).isEqualTo(false);
  }

  @Test
  void no_sha1() throws IOException {
    StatContext context = new StatContext(new String[] {"--no-sha1", "src"});
    assertThat(context.baseDirectory.toString()).isEqualTo(Paths.get("src").toRealPath().toString());
    assertThat(context.computeSha1).isEqualTo(false);
  }

  @Test
  void save() throws IOException {
    StatContext context = new StatContext(new String[] {"--save", "src"});
    assertThat(context.baseDirectory.toString()).isEqualTo(Paths.get("src").toRealPath().toString());
    assertThat(context.save).isEqualTo(true);
  }

  @Test
  void diff() throws IOException {
    StatContext context = new StatContext(new String[] {"--diff", "src"});
    assertThat(context.baseDirectory.toString()).isEqualTo(Paths.get("src").toRealPath().toString());
    assertThat(context.diff).isEqualTo(true);
  }

  @Test
  void ignore() throws IOException {
    StatContext context = new StatContext(new String[] {"--ignore", "f1", "--ignore", "*/.git", "--ignore", "*/target", "--ignore", "f2", "src"});
    assertThat(context.baseDirectory.toString()).isEqualTo(Paths.get("src").toRealPath().toString());
    assertThat(context.ignoreMatcher.ignoredRelativePaths).containsExactlyInAnyOrder("f1", "f2", ".directory-stats");
    assertThat(context.ignoreMatcher.advancedMatchers).hasSize(3);
    assertThat(((IgnoreExpression.EndWith) context.ignoreMatcher.advancedMatchers.get(0)).suffix).isEqualTo("/.git");
    assertThat(((IgnoreExpression.EndWith) context.ignoreMatcher.advancedMatchers.get(1)).suffix).isEqualTo("/target");
    assertThat(((IgnoreExpression.EndWith) context.ignoreMatcher.advancedMatchers.get(2)).suffix).isEqualTo("/.directory-stats");
  }

}
