package com.auzeill.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class StatsTest {

  @Test
  void test_directory() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] {Paths.get("src", "test", "resources").toString()});
    assertThat(FileAttributesTest.forceSysFields(new String(out.toByteArray(), UTF_8)))
      .isEqualTo("" +
        "data.txt|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|a8fdc205a9f19cc1c7507a60c4f01b13d11d7fd0" + System.lineSeparator() +
        "link.txt|l|8|alban|alban|rwxrwxrwx|2020-09-02T15:43:48.680382Z|data.txt" + System.lineSeparator() +
        ".|d|12|alban|alban|rwxrwxr-x|2020-09-02T15:43:48.680382Z|76a57a2f4b5b9eab89b7ac66722b7bc480b81b6b" + System.lineSeparator());
  }

  @Test
  void test_file() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] {Paths.get("src", "test", "resources", "data.txt").toString()});
    assertThat(FileAttributesTest.forceSysFields(new String(out.toByteArray(), UTF_8)))
      .isEqualTo("data.txt|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|a8fdc205a9f19cc1c7507a60c4f01b13d11d7fd0" + System.lineSeparator());
  }

  @Test
  void test_ignore() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] { "--ignore", "link.txt", Paths.get("src", "test", "resources").toString()});
    assertThat(FileAttributesTest.forceSysFields(new String(out.toByteArray(), UTF_8)))
      .isEqualTo("" +
        "data.txt|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|a8fdc205a9f19cc1c7507a60c4f01b13d11d7fd0" + System.lineSeparator() +
        ".|d|4|alban|alban|rwxrwxr-x|2020-09-02T15:43:48.680382Z|ad404b01110b7ae2547f71361377c2f72194319a" + System.lineSeparator());
  }

  @Test
  void save_in_default_stats_directory(@TempDir Path tempDir) throws IOException {
    Files.writeString(tempDir.resolve("f1"), "abcd", UTF_8);
    Files.writeString(tempDir.resolve("f2"), "efgh", UTF_8);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] { "--save", tempDir.toString() });
    String output = new String(out.toByteArray(), UTF_8);
    assertThat(output).matches(tempDir.toString() + File.separator + ".directory-stats" + File.separator + "stat-\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}h\\d{2}m\\d{2}s\\d{3}" + System.lineSeparator());
    String statPath = output.replaceFirst("[\r\n]+$", "");
    String fileContent = Files.readString(Paths.get(statPath), UTF_8);
    assertThat(FileAttributesTest.forceSysFields(fileContent)).isEqualTo("" +
      "f1|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|81fe8bfe87576c3ecb22426f8e57847382917acf" + System.lineSeparator() +
      "f2|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|2aed8aa9f826c21ef07d5ee15b48eea06e9c8a62" + System.lineSeparator() +
      ".|d|8|alban|alban|rwx------|2020-09-02T15:43:48.680382Z|e3d1e234ae764b4360a8984d25798fefec22910d" + System.lineSeparator());
  }

  @Test
  void save_in_custom_stats_directory(@TempDir Path tempDir) throws IOException {
    Files.writeString(tempDir.resolve("f1"), "abcd", UTF_8);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] { "--save", "--stats-directory", tempDir.toString() + File.separator + ".custom-stats", tempDir.toString() });
    String output = new String(out.toByteArray(), UTF_8);
    assertThat(output).matches(tempDir.toString() + File.separator + ".custom-stats" + File.separator + "stat-\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}h\\d{2}m\\d{2}s\\d{3}" + System.lineSeparator());
    String statPath = output.replaceFirst("[\r\n]+$", "");
    String fileContent = Files.readString(Paths.get(statPath), UTF_8);
    assertThat(FileAttributesTest.forceSysFields(fileContent)).isEqualTo("" +
      "f1|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|81fe8bfe87576c3ecb22426f8e57847382917acf" + System.lineSeparator() +
      ".|d|4|alban|alban|rwx------|2020-09-02T15:43:48.680382Z|e72bd3cc1814a0e1186ddb826977df13207264f0" + System.lineSeparator());
  }

  @Test
  void reuse_sha1_from_previous_save(@TempDir Path tempDir) throws IOException {
    Files.writeString(tempDir.resolve("f1"), "abcd", UTF_8);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] { "--save", tempDir.toString() });
    String output = new String(out.toByteArray(), UTF_8);
    assertThat(output).matches(tempDir.toString() + File.separator + ".directory-stats" + File.separator + "stat-\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}h\\d{2}m\\d{2}s\\d{3}" + System.lineSeparator());
    String statPath = output.replaceFirst("[\r\n]+$", "");
    String fileContent = Files.readString(Paths.get(statPath), UTF_8);
    assertThat(FileAttributesTest.forceSysFields(fileContent)).isEqualTo("" +
      "f1|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|81fe8bfe87576c3ecb22426f8e57847382917acf" + System.lineSeparator() +
      ".|d|4|alban|alban|rwx------|2020-09-02T15:43:48.680382Z|e72bd3cc1814a0e1186ddb826977df13207264f0" + System.lineSeparator());

    String newContentWithForcedSha1 = fileContent
      .replace("81fe8bfe87576c3ecb22426f8e57847382917acf" , "0123456789abcdef0123456789abcdef01234567");

    Files.writeString(Paths.get(statPath), newContentWithForcedSha1, UTF_8);

    out = new ByteArrayOutputStream();
    stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] { tempDir.toString() });
    assertThat(FileAttributesTest.forceSysFields(new String(out.toByteArray(), UTF_8)))
      .isEqualTo("" +
      "f1|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|0123456789abcdef0123456789abcdef01234567" + System.lineSeparator() +
      ".|d|4|alban|alban|rwx------|2020-09-02T15:43:48.680382Z|158b484ae1f6f64f89da22397d25fbdafad02252" + System.lineSeparator());
  }

  @Test
  void diff_from_previous(@TempDir Path tempDir) throws IOException {
    Files.writeString(tempDir.resolve("f1"), "abcd", UTF_8);
    Files.writeString(tempDir.resolve("f2"), "efgh", UTF_8);
    Files.writeString(tempDir.resolve("f3"), "ijdk", UTF_8);
    Files.writeString(tempDir.resolve("f5"), "efgh", UTF_8);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] { "--save", tempDir.toString() });
    String output = new String(out.toByteArray(), UTF_8);
    assertThat(output).matches(tempDir.toString() + File.separator + ".directory-stats" + File.separator + "stat-\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}h\\d{2}m\\d{2}s\\d{3}" + System.lineSeparator());
    String statPath = output.replaceFirst("[\r\n]+$", "");
    String fileContent = Files.readString(Paths.get(statPath), UTF_8);
    assertThat(FileAttributesTest.forceSysFields(fileContent)).isEqualTo("" +
      "f1|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|81fe8bfe87576c3ecb22426f8e57847382917acf" + System.lineSeparator() +
      "f2|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|2aed8aa9f826c21ef07d5ee15b48eea06e9c8a62" + System.lineSeparator() +
      "f3|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|cca5603cae64651971a35bc1488f0d23ddabdff9" + System.lineSeparator() +
      "f5|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|2aed8aa9f826c21ef07d5ee15b48eea06e9c8a62" + System.lineSeparator() +
      ".|d|16|alban|alban|rwx------|2020-09-02T15:43:48.680382Z|f81b3e16656c1bd84d7522ed2f83fa996ffd8497" + System.lineSeparator());


    Files.delete(tempDir.resolve("f2"));
    Files.writeString(tempDir.resolve("f3"), "changed content", UTF_8);
    Files.writeString(tempDir.resolve("f4"), "new file content", UTF_8);

    out = new ByteArrayOutputStream();
    stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] { "--diff", tempDir.toString() });
    assertThat(FileAttributesTest.forceSysFields(new String(out.toByteArray(), UTF_8)))
      .isEqualTo("" +
        "-del- f2|f|4|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|2aed8aa9f826c21ef07d5ee15b48eea06e9c8a62" + System.lineSeparator() +
        "~mod~ f3| size 4 -> 15 | modifiedTime 2020-09-02T15:43:48.680382Z -> 2020-09-02T15:43:48.680382Z | sha1OrSymbolicLink cca5603cae64651971a35bc1488f0d23ddabdff9 -> 1fa817e97796161063e307eac706bb8b06cf956c |" + System.lineSeparator() +
        "+new+ f4|f|16|alban|alban|rw-r--r--|2020-09-02T15:43:48.680382Z|17c494d126c27755e2134a4388178d808b139ce9" + System.lineSeparator() +
        "~mod~ .| size 16 -> 39 | modifiedTime 2020-09-02T15:43:48.680382Z -> 2020-09-02T15:43:48.680382Z | sha1OrSymbolicLink f81b3e16656c1bd84d7522ed2f83fa996ffd8497 -> 2d8050016ac5619885aaac0402f8c5d88cceb077 |" + System.lineSeparator());
  }

}
