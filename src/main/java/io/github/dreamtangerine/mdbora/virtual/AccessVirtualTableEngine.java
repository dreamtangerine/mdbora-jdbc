/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.virtual;

import io.github.spannm.jackcess.Database;
import java.io.IOException;
import io.github.dreamtangerine.mdbora.access.AccessDatabaseRegistry;
import io.github.spannm.jackcess.TableDefinition;
import io.github.spannm.jackcess.TableMetaData;
import java.util.Iterator;
import java.util.Locale;
import org.h2.api.TableEngine;
import org.h2.command.ddl.CreateTableData;

/**
 * H2 table engine that creates virtual tables backed by Jackcess Access tables registered for the current Mdbora connection.
 */
public final class AccessVirtualTableEngine implements TableEngine {

  /**
   * Creates a table engine instance for H2 service loading.
   */
  public AccessVirtualTableEngine() {
  }

  @Override
  public org.h2.table.Table createTable(CreateTableData data) {
    validateEngineParameters(data);

    String databaseId = data.tableEngineParams.get(0);
    String tableName = data.tableEngineParams.get(1);

    Database database = AccessDatabaseRegistry.require(databaseId);

    TableMetaData tableMetadata = requireTableMetadata(database, tableName);
    TableDefinition definition;

    try {
      definition = AccessVirtualTableRegistrar.require(database, tableMetadata);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read the Access table definition: " + tableName, exception);
    }

    if (definition == null) {
      throw new IllegalStateException("No local Access table definition is available: " + tableName);
    }

    return new AccessVirtualTable(data, databaseId, tableName, definition);
  }

  private static TableMetaData requireTableMetadata(Database database, String tableName) {
    TableMetaData result = null;
    String expectedName = tableName.toUpperCase(Locale.ROOT);
    Iterator<TableMetaData> it = database.newTableMetaDataIterable().iterator();
    
    while (null == result && it.hasNext()) {
      TableMetaData tableMetadata =it.next();
      String currentName = tableMetadata.getName().toUpperCase(Locale.ROOT);

      if (expectedName.equals(currentName)) {
        result = tableMetadata;
      }
    }

    if (result == null) {
      throw new IllegalArgumentException("Access table metadata not found: " + tableName);
    }

    return result;
  }

  /**
   * Validates the H2 table engine creation parameters.
   *
   * <p>
   * Mdbora virtual tables require exactly two engine parameters: the registered Access database identifier and the Access table name.</p>
   *
   * @param data H2 table creation data
   * @throws IllegalArgumentException if the creation data is {@code null}, the parameter list is missing, or the number of parameters is not valid
   */
  private static void validateEngineParameters(CreateTableData data) {

    if (data == null) {
      throw new IllegalArgumentException("The H2 table creation data cannot be null");
    }

    if (data.tableEngineParams == null) {
      throw new IllegalArgumentException("Mdbora requires table engine parameters");
    }

    if (data.tableEngineParams.size() != 2) {
      throw new IllegalArgumentException("Mdbora requires exactly two table engine parameters: databaseId and tableName");
    }

    String databaseId = data.tableEngineParams.get(0);
    String tableName = data.tableEngineParams.get(1);

    if (databaseId == null || databaseId.isBlank()) {
      throw new IllegalArgumentException("The Access database identifier cannot be null or blank");
    }

    if (tableName == null || tableName.isBlank()) {
      throw new IllegalArgumentException("The Access table name cannot be null or blank");
    }
  }
}
