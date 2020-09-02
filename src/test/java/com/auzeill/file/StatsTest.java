package com.auzeill.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class StatsTest {

  @Test
  void test_directory() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] {Paths.get("src", "test", "resources").toString()});
    assertThat(new String(out.toByteArray(), UTF_8))
      .isEqualTo("" +
        "data.txt|f|4|alban|alban|rw-rw-r--|2020-09-02T15:37:56.940581Z|a8fdc205a9f19cc1c7507a60c4f01b13d11d7fd0" + System.lineSeparator() +
        "link.txt|l|8|alban|alban|rwxrwxrwx|2020-09-02T15:43:48.680382Z|data.txt" + System.lineSeparator() +
        ".|d|12|alban|alban|rwxrwxr-x|2020-09-02T15:43:48.680382Z|76a57a2f4b5b9eab89b7ac66722b7bc480b81b6b" + System.lineSeparator());
  }

  @Test
  void test_file() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] {Paths.get("src", "test", "resources", "data.txt").toString()});
    assertThat(new String(out.toByteArray(), UTF_8))
      .isEqualTo("data.txt|f|4|alban|alban|rw-rw-r--|2020-09-02T15:37:56.940581Z|a8fdc205a9f19cc1c7507a60c4f01b13d11d7fd0" + System.lineSeparator());
  }

  @Test
  void test_ignore() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(out, true, UTF_8);
    Stats.stats(stream, new String[] { "--ignore", "link.txt", Paths.get("src", "test", "resources").toString()});
    assertThat(new String(out.toByteArray(), UTF_8))
      .isEqualTo("" +
        "data.txt|f|4|alban|alban|rw-rw-r--|2020-09-02T15:37:56.940581Z|a8fdc205a9f19cc1c7507a60c4f01b13d11d7fd0" + System.lineSeparator() +
        ".|d|4|alban|alban|rwxrwxr-x|2020-09-02T15:43:48.680382Z|ad404b01110b7ae2547f71361377c2f72194319a" + System.lineSeparator());
  }

}
