/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora;

import java.sql.Driver;
import io.github.dreamtangerine.mdbora.codec.AccessGuidCodec;
import io.github.dreamtangerine.mdbora.jdbc.MdboraDriver;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MdboraTest {

  @Test
  void acceptsMdboraUrls() throws Exception {
    Driver driver = new MdboraDriver();

    assertTrue(driver.acceptsURL("jdbc:mdbora:C:/data/test.mdb"));
    assertFalse(driver.acceptsURL("jdbc:h2:mem:test"));
  }

  @Test
  void normalizesGuidsInBothDirections() {
    String guid = "0000ADF7-3266-4C06-8DDC-D8D2D851676F";

    assertEquals(guid, AccessGuidCodec.toJdbc("{" + guid + "}"));
    assertEquals("{" + guid + "}", AccessGuidCodec.toJackcess("'" + guid + "'"));
  }
}
