/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.access;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Table;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import org.h2.message.DbException;

/**
 * Resolves Jackcess tables when an H2 operation needs to access their data.
 */
public final class AccessTableResolver {

  private AccessTableResolver() {
  }

  /**
   * Resolves an Access table using its registered database identifier.
   *
   * @param databaseId registered Access database identifier
   * @param tableName Access table name
   * @return resolved Jackcess table
   * @throws DbException if the table cannot be opened
   */
  public static Table requireTable(String databaseId, String tableName) {
    Table table = findTable(databaseId, tableName);

    if (table == null) {
      throw DbException.getInvalidValueException("tableName", tableName);
    }

    return table;
  }

  /**
   * Finds an Access table in a registered database.
   *
   * <p>
   * The table name comparison is case-insensitive, while the original name stored in the Access database is used to open the table.</p>
   *
   * <p>
   * This method returns {@code null} when no matching table exists. If the table exists but cannot be opened, the Jackcess exception is converted into an H2 database
   * exception.</p>
   *
   * @param databaseId registered Access database identifier
   * @param tableName requested Access table name
   * @return resolved Jackcess table, or {@code null} if no matching table exists
   * @throws DbException if the registered database cannot be found or the table cannot be opened
   */
  public static Table findTable(String databaseId, String tableName) {
    try {
      Database database = AccessDatabaseRegistry.require(databaseId);
      String actualTableName = findTableName(database, tableName);
      Table result = null;

      if (actualTableName != null) {
        result = database.getTable(actualTableName);
      }

      return result;
    } catch (IOException exception) {
      throw DbException.convert(exception);
    }
  }

  /**
   * Finds the original Access table name without distinguishing character case.
   *
   * <p>
   * The returned value preserves the spelling and character case stored in the Access database.</p>
   *
   * @param database source Access database
   * @param requestedTableName requested table name
   * @return original Access table name, or {@code null} if no matching table exists
   */
  private static String findTableName(Database database, String requestedTableName) throws IOException {
    String result = null;

    if (requestedTableName != null) {
      Set<String> names = database.getTableNames();

      if (names.contains(requestedTableName)) {
        result = requestedTableName;
      } else {

        for (Iterator<String> it = names.iterator(); null == result && it.hasNext();) {
          String currentTableName = it.next();

          if (currentTableName.equalsIgnoreCase(requestedTableName)) {
            result = currentTableName;
          }
        }
      }
    }

    return result;
  }

  /**
   * Resolves an Access index from a table.
   *
   * @param databaseId registered Access database identifier
   * @param tableName Access table name
   * @param indexName Access index name
   * @return resolved Jackcess index
   * @throws DbException if the table or index cannot be resolved
   */
  public static io.github.spannm.jackcess.Index requireIndex(String databaseId, String tableName, String indexName) {
    Table table = requireTable(databaseId, tableName);
    io.github.spannm.jackcess.Index index = null;

    try {
      index = table.getIndex(indexName);
    } catch (IllegalArgumentException exception) {
      throw DbException.convert(exception);
    }

    if (index == null) {
      throw DbException.getInvalidValueException("indexName", indexName);
    }

    return index;
  }
}
