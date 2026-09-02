/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.jdbc;

import io.github.spannm.jackcess.Database;
import io.github.dreamtangerine.mdbora.access.AccessDatabaseRegistry;
import io.github.dreamtangerine.mdbora.internal.TemporaryH2;

final class MdboraConnectionContext implements AutoCloseable {

  private final String id;
  private final Database database;
  private final TemporaryH2 h2;
  private boolean closed;

  MdboraConnectionContext(String id, Database database, TemporaryH2 h2) {
    this.id = id;
    this.database = database;
    this.h2 = h2;
  }

  @Override
  public synchronized void close() throws Exception {
    if (!closed) {
      closed = true;

      Exception failure = null;

      try {
        h2.close();
      } catch (Exception e) {
        failure = e;
      }

      AccessDatabaseRegistry.detach(id);

      try {
        database.close();
      } catch (Exception e) {
        if (null == failure) {
          failure = e;
        } else {
          failure.addSuppressed(e);
        }
      }

      if (failure != null) {
        throw failure;
      }
    }
  }
}
