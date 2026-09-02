/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.virtual;


import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Table;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import io.github.dreamtangerine.mdbora.codec.AccessTypeMapper;
import io.github.dreamtangerine.mdbora.internal.SqlNames;

/**
 * Registers all user tables from an Access database as H2 external tables.
 */
public final class AccessVirtualTableRegistrar {

  private static final String ENGINE = AccessVirtualTableEngine.class.getName();

  private AccessVirtualTableRegistrar() {
  }

  /**
   * Registers every table exposed by the supplied Access database.
   *
   * @param connection internal SQL connection receiving the external tables
   * @param id identifier used to resolve the Access database
   * @param database open Jackcess database
   * @throws IOException if Access metadata cannot be read
   * @throws SQLException if a virtual table cannot be created
   */
  public static void registerAll(Connection connection, String id, Database database) throws IOException, SQLException {
    for (String name : database.getTableNames()) {
      Table table = database.getTable(name);
      StringBuilder sql = new StringBuilder("CREATE TABLE ").append(SqlNames.identifier(name)).append(" (");
      boolean first = true;

      for (Column column : table.getColumns()) {
        if (!first) {
          sql.append(", ");
        }
        
        first = false;
        sql.append(SqlNames.identifier(column.getName())).append(' ').append(AccessTypeMapper.h2TypeDeclaration(column));
      }
      
      sql.append(") ENGINE ").append(SqlNames.identifier(ENGINE)).append(" WITH ").append(SqlNames.identifier(id)).append(", ").append(SqlNames.identifier(name));
      
      try (Statement statement = connection.createStatement()) {
        statement.execute(sql.toString());
      }
    }
  }
}
