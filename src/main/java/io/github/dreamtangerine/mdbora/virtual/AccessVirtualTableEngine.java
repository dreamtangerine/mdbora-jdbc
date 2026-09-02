/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.virtual;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Table;
import java.io.IOException;
import io.github.dreamtangerine.mdbora.access.AccessDatabaseRegistry;
import org.h2.api.TableEngine;
import org.h2.command.ddl.CreateTableData;

/**
 * H2 table engine that creates virtual tables backed by Jackcess Access
 * tables registered for the current Mdbora connection.
 */
public final class AccessVirtualTableEngine implements TableEngine {

  /**
   * Creates a table engine instance for H2 service loading.
   */
  public AccessVirtualTableEngine() {
  }

  @Override
  public org.h2.table.Table createTable(CreateTableData data) {
    if (data.tableEngineParams == null || data.tableEngineParams.size() != 2) {
      throw new IllegalArgumentException("Mdbora needs databaseId and tableName");
    }
    
    String id = data.tableEngineParams.get(0);
    String name = data.tableEngineParams.get(1);
    Database database = AccessDatabaseRegistry.require(id);
    
    try {
      Table table = database.getTable(name);
    
      if (table == null) {
        throw new IllegalArgumentException("Access table not found: " + name);
      }
      
      return new AccessVirtualTable(data, table);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot open Access table " + name, e);
    }
  }
}
