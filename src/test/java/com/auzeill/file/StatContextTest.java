package com.auzeill.file;

import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static com.auzeill.file.StatContext.pathToSort;
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
  void path_order() {
    assertThat(pathToSort(".")).isEqualTo(pathToSort("."));
    assertThat(pathToSort("a/b")).isLessThan(pathToSort("a"));
    assertThat(pathToSort("a/baaa")).isGreaterThan(pathToSort("a/ba/a"));
    assertThat(pathToSort("dev/github/alban-auzeill/python-sonarqube-nosetests-coverage-demo/.gitignore"))
      .isEqualTo(pathToSort("dev/github/alban-auzeill/python-sonarqube-nosetests-coverage-demo/.gitignore"));
  }

}
