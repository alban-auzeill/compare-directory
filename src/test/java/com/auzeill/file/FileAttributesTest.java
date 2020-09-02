package com.auzeill.file;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileAttributesTest {

  @Test
  void type() {
    assertThat(FileAttributes.Type.fromString("f")).isEqualTo(FileAttributes.Type.FILE);
    assertThat(FileAttributes.Type.fromString("d")).isEqualTo(FileAttributes.Type.DIRECTORY);
    assertThat(FileAttributes.Type.fromString("l")).isEqualTo(FileAttributes.Type.SYMBOLIC_LINK);
    assertThatThrownBy(() -> FileAttributes.Type.fromString("x")).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid code: x");
  }

  @Test
  void from_string_to_string() {
    // relativeLinuxPath|size|owner|group|permissions|modifiedTime|sha1
    String description = "path/file.txt|f|2087|alban|alban|rw-rw-r--|2020-09-02T13:32:46.222112Z|b6589fc6ab0dc82cf12099d1c2d40ab994e8410c";
    FileAttributes attributes = FileAttributes.fromString(description);
    assertThat(attributes.relativeLinuxPath).isEqualTo("path/file.txt");
    assertThat(attributes.type).isEqualTo(FileAttributes.Type.FILE);
    assertThat(attributes.size).isEqualTo(2087);
    assertThat(attributes.owner).isEqualTo("alban");
    assertThat(attributes.group).isEqualTo("alban");
    assertThat(attributes.permissions).isEqualTo("rw-rw-r--");
    assertThat(attributes.modifiedTime).isEqualTo("2020-09-02T13:32:46.222112Z");
    assertThat(attributes.sha1OrSymbolicLink).isEqualTo("b6589fc6ab0dc82cf12099d1c2d40ab994e8410c");
    assertThat(attributes.toString()).isEqualTo(description);
  }

  @Test
  void from_file() throws IOException {
    Path baseDir = Paths.get("src", "test");
    StatContext context = new StatContext(baseDir, baseDir, true);
    Path path = context.baseDirectory.resolve(Paths.get("resources", "data.txt"));
    FileAttributes attributes = FileAttributes.fromPath(context, path);
    assertThat(attributes.relativeLinuxPath).isEqualTo("resources/data.txt");
    assertThat(attributes.type).isEqualTo(FileAttributes.Type.FILE);
    assertThat(attributes.size).isEqualTo(4);
    assertThat(attributes.owner).isEqualTo("alban");
    assertThat(attributes.group).isEqualTo("alban");
    assertThat(attributes.permissions).isEqualTo("rw-rw-r--");
    assertThat(attributes.modifiedTime).isEqualTo("2020-09-02T15:37:56.940581Z");
    assertThat(attributes.sha1OrSymbolicLink).isEqualTo("a8fdc205a9f19cc1c7507a60c4f01b13d11d7fd0");
    assertThat(attributes.toString()).isEqualTo("resources/data.txt|f|4|alban|alban|rw-rw-r--|2020-09-02T15:37:56.940581Z|a8fdc205a9f19cc1c7507a60c4f01b13d11d7fd0");
  }

  @Test
  void from_link() throws IOException {
    Path baseDir = Paths.get("src", "test");
    StatContext context = new StatContext(baseDir, baseDir, true);
    Path path = context.baseDirectory.resolve(Paths.get("resources", "link.txt"));
    FileAttributes attributes = FileAttributes.fromPath(context, path);
    assertThat(attributes.relativeLinuxPath).isEqualTo("resources/link.txt");
    assertThat(attributes.type).isEqualTo(FileAttributes.Type.SYMBOLIC_LINK);
    assertThat(attributes.size).isEqualTo(8);
    assertThat(attributes.owner).isEqualTo("alban");
    assertThat(attributes.group).isEqualTo("alban");
    assertThat(attributes.permissions).isEqualTo("rwxrwxrwx");
    assertThat(attributes.modifiedTime).isEqualTo("2020-09-02T15:43:48.680382Z");
    assertThat(attributes.sha1OrSymbolicLink).isEqualTo("data.txt");
    assertThat(attributes.toString()).isEqualTo("resources/link.txt|l|8|alban|alban|rwxrwxrwx|2020-09-02T15:43:48.680382Z|data.txt");
  }

}
