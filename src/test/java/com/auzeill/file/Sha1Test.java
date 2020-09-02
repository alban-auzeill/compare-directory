package com.auzeill.file;

import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sha1Test {

  @Test
  void sha1() throws IOException {
    assertThat(Sha1.digest(Paths.get("src", "test", "resources", "data.txt")))
      .isEqualTo("a8fdc205a9f19cc1c7507a60c4f01b13d11d7fd0");
  }

}
