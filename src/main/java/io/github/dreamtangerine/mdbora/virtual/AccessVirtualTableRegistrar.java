/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.virtual;

import io.github.dreamtangerine.mdbora.codec.AccessTypeMapper;
import io.github.dreamtangerine.mdbora.config.MdboraConfiguration;
import io.github.dreamtangerine.mdbora.internal.SqlNames;
import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.TableDefinition;
import io.github.spannm.jackcess.TableMetaData;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Registers Access tables as external tables in the internal SQL engine.
 *
 * <p>
 * This class is public only to support communication between internal components located in different packages. It is not part of the supported public Mdbora API and may change
 * without notice.</p>
 */
public final class AccessVirtualTableRegistrar {

  private static final String ENGINE = AccessVirtualTableEngine.class.getName();

  private AccessVirtualTableRegistrar() {
  }

  /**
   * Registers the Access tables exposed by the supplied database.
   *
   * <p>
   * System tables are always ignored. Linked tables are registered only when they are enabled in the supplied connection configuration.</p>
   *
   * @param connection internal SQL connection
   * @param id registered Access database identifier
   * @param database source Access database
   * @param configuration Mdbora connection configuration
   * @throws IOException if Access metadata or table definitions cannot be read
   * @throws SQLException if an external table cannot be registered
   */
  public static void registerAll(Connection connection, String id, Database database, MdboraConfiguration configuration) throws IOException, SQLException {
    validateArguments(connection, id, database, configuration);
    Iterable<? extends TableMetaData> tableMetadataCollection = database.newTableMetaDataIterable();

    for (TableMetaData tableMetadata : tableMetadataCollection) {
      boolean register = shouldRegister(tableMetadata, configuration);

      if (register) {
        registerTable(connection, id, database, tableMetadata);
      }
    }
  }

  /**
   * Validates the arguments required to register the virtual tables.
   *
   * @param connection internal SQL connection
   * @param id registered Access database identifier
   * @param database source Access database
   * @param configuration Mdbora connection configuration
   * @throws NullPointerException if an argument is {@code null}
   */
  private static void validateArguments(Connection connection, String id, Database database, MdboraConfiguration configuration) {
    Objects.requireNonNull(connection, "The SQL connection cannot be null");
    Objects.requireNonNull(id, "The database identifier cannot be null");
    Objects.requireNonNull(database, "The Access database cannot be null");
    Objects.requireNonNull(configuration, "The Mdbora configuration cannot be null");
  }

  /**
   * Determines whether a table should be registered.
   *
   * <p>
   * System tables are never registered. A linked table is registered only when linked-table support is explicitly enabled in the connection configuration.</p>
   *
   * @param tableMetadata Access table metadata
   * @param configuration Mdbora connection configuration
   * @return {@code true} if the table should be registered
   */
  private static boolean shouldRegister(TableMetaData tableMetadata, MdboraConfiguration configuration) {
    boolean register = !tableMetadata.isSystem();

    if (register && tableMetadata.isLinked()) {
      register = configuration.isIncludeLinkedTables();
    }

    return register;
  }

  /**
   * Opens an Access table and registers the corresponding external table.
   *
   * @param connection internal SQL connection
   * @param id registered Access database identifier
   * @param database source Access database
   * @param tableMetadata metadata of the table to register
   * @throws IOException if the Access table cannot be opened
   * @throws SQLException if the external SQL table cannot be created
   */
  private static void registerTable(Connection connection, String id, Database database, TableMetaData tableMetadata) throws IOException, SQLException {
    TableDefinition definition = require(database, tableMetadata);

    if (definition == null) {
      throw new IOException("No local table definition is available for: " + tableMetadata.getName());
    }

    String tableName = tableMetadata.getName();
    String sql = createTableSql(id, tableName, definition);

    executeCreateTable(connection, sql);
  }

  /**
   * Executes the SQL statement that creates an external table.
   *
   * @param connection internal SQL connection
   * @param sql table creation statement
   * @throws SQLException if the statement cannot be executed
   */
  private static void executeCreateTable(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  /**
   * Builds the H2 SQL statement used to register an Access table as a virtual table.
   *
   * <p>
   * The generated statement contains the table name, its column definitions, the Mdbora table engine class, the registered database identifier and the original Access table
   * name.</p>
   *
   * @param id registered Access database identifier
   * @param tableName original Access table name
   * @param definition local Access table definition containing the columns and their types
   * @return SQL statement used to create the H2 virtual table
   */
  private static String createTableSql(String id, String tableName, TableDefinition definition) {
    StringBuilder sql = new StringBuilder();

    sql.append("CREATE TABLE ")
            .append(
                    SqlNames.identifier(tableName))
            .append(" (");

    appendColumns(sql, definition);

    sql.append(") ENGINE ")
            .append(
                    SqlNames.identifier(ENGINE))
            .append(" WITH ")
            .append(
                    SqlNames.identifier(id))
            .append(", ")
            .append(
                    SqlNames.identifier(tableName));

    return sql.toString();
  }

  /**
   * Appends the Access column definitions to an H2 virtual table creation statement.
   *
   * <p>
   * Each Access column name is quoted as an SQL identifier and its data type is converted to the corresponding H2 type declaration. Column definitions are separated by commas.</p>
   *
   * @param sql SQL statement being constructed
   * @param definition local Access table definition containing the columns to append
   */
  private static void appendColumns(StringBuilder sql, TableDefinition definition) {
    boolean first = true;

    for (Column column : definition.getColumns()) {

      if (!first) {
        sql.append(", ");
      }

      appendColumn(sql, column);

      first = false;
    }
  }

  /**
   * Appends one Access column definition to an H2 virtual table creation statement.
   *
   * @param sql SQL statement being constructed
   * @param column Access column to append
   */
  private static void appendColumn(StringBuilder sql, Column column) {
    String columnName = SqlNames.identifier(column.getName());
    String typeDeclaration = AccessTypeMapper.h2TypeDeclaration(column);

    sql.append(columnName).append(' ').append(typeDeclaration);
  }

  /**
   * Resolves the structural definition of an Access table.
   *
   * <p>
   * ODBC linked tables may provide a locally stored definition without opening the external data source. Other table types are opened temporarily and used as their own structural
   * definition.</p>
   *
   * <p>
   * The returned definition must be used only while registering the H2 virtual table. Long-lived Mdbora objects must not retain references to the returned definition, its columns,
   * or its indexes.</p>
   *
   * @param database source Access database
   * @param tableMetadata Access table metadata
   * @return structural definition of the Access table
   * @throws IOException if the definition cannot be obtained
   */
  static TableDefinition require(Database database, TableMetaData tableMetadata) throws IOException {
    TableDefinition definition = null;
    boolean linkedOdbc = tableMetadata.getType() == TableMetaData.Type.LINKED_ODBC;

    if (linkedOdbc) {
      definition = tableMetadata.getTableDefinition(database);
    } else {
      definition = tableMetadata.open(database);
    }

    if (definition == null) {
      throw new IOException("Unable to obtain the Access table definition: " + tableMetadata.getName());
    }
    
    return definition;
  }
}
