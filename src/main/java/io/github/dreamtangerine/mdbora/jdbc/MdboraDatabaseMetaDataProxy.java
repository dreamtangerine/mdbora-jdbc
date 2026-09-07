package io.github.dreamtangerine.mdbora.jdbc;

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
import io.github.dreamtangerine.mdbora.access.AccessDatabaseRegistry;
import io.github.dreamtangerine.mdbora.access.AccessTableResolver;
import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Index;
import io.github.spannm.jackcess.Relationship;
import io.github.spannm.jackcess.Table;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.h2.tools.SimpleResultSet;

/**
 * Provides Mdbora-specific JDBC metadata while delegating structural metadata operations to the internal SQL engine.
 *
 * <p>
 * Identity and capability methods are overridden to describe Mdbora and Microsoft Access instead of exposing the internal SQL implementation.</p>
 */
final class MdboraDatabaseMetaDataProxy implements InvocationHandler {

  private static final String DEFAULT_SCHEMA = "PUBLIC";
  private static final String DATABASE_PRODUCT_NAME = "Microsoft Access";
  private static final String DRIVER_NAME = "Mdbora JDBC Driver";

  private final MdboraConnectionContext context;
  private final Connection connection;
  private final DatabaseMetaData delegate;

  private MdboraDatabaseMetaDataProxy(MdboraConnectionContext context, Connection connection, DatabaseMetaData delegate) {
    this.context = context;
    this.connection = connection;
    this.delegate = delegate;
  }

  /**
   * Wraps database metadata with Mdbora-specific identity and capabilities.
   *
   * @param context Mdbora connection context
   * @param connection public JDBC connection proxy
   * @param delegate internal database metadata
   * @return wrapped database metadata
   */
  static DatabaseMetaData wrap(MdboraConnectionContext context, Connection connection, DatabaseMetaData delegate) {
    DatabaseMetaData result = (DatabaseMetaData) Proxy.newProxyInstance(
            DatabaseMetaData.class.getClassLoader(),
            new Class<?>[]{DatabaseMetaData.class},
            new MdboraDatabaseMetaDataProxy(context, connection, delegate));

    return result;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
    String methodName = method.getName();

    Object result;

    if ("getDatabaseProductName".equals(methodName)) {
      result = DATABASE_PRODUCT_NAME;
    } else if ("getDatabaseProductVersion".equals(methodName)) {
      result = context.getDatabaseProductVersion();
    } else if ("getDatabaseMajorVersion".equals(methodName)) {
      result = 0;
    } else if ("getDatabaseMinorVersion".equals(methodName)) {
      result = 0;
    } else if ("getDriverName".equals(methodName)) {
      result = DRIVER_NAME;
    } else if ("getDriverVersion".equals(methodName)) {
      result = MdboraVersion.getVersion();
    } else if ("getDriverMajorVersion".equals(methodName)) {
      result = MdboraVersion.MAJOR_VERSION;
    } else if ("getDriverMinorVersion".equals(methodName)) {
      result = MdboraVersion.MINOR_VERSION;
    } else if ("getURL".equals(methodName)) {
      result = context.getUrl();
    } else if ("getUserName".equals(methodName)) {
      result = "";
    } else if ("getConnection".equals(methodName)) {
      result = connection;
    } else if ("isReadOnly".equals(methodName)) {
      result = true;
    } else if ("supportsTransactions".equals(methodName)) {
      result = false;
    } else if ("supportsSavepoints".equals(methodName)) {
      result = false;
    } else if ("supportsStoredProcedures".equals(methodName)) {
      result = false;
    } else if ("supportsBatchUpdates".equals(methodName)) {
      result = false;
    } else if ("supportsGetGeneratedKeys".equals(methodName)) {
      result = false;
    } else if ("supportsMultipleTransactions".equals(methodName)) {
      result = false;
    } else if ("supportsDataDefinitionAndDataManipulationTransactions".equals(methodName)) {
      result = false;
    } else if ("supportsDataManipulationTransactionsOnly".equals(methodName)) {
      result = false;
    } else if ("dataDefinitionCausesTransactionCommit".equals(methodName)) {
      result = false;
    } else if ("dataDefinitionIgnoredInTransactions".equals(methodName)) {
      result = false;
    } else if ("getDefaultTransactionIsolation".equals(methodName)) {
      result = Connection.TRANSACTION_NONE;
    } else if ("unwrap".equals(methodName)) {
      result = unwrap(proxy, arguments);
    } else if ("isWrapperFor".equals(methodName)) {
      result = isWrapperFor(proxy, arguments);
    } else if ("equals".equals(methodName)) {
      result = proxy == arguments[0];
    } else if ("hashCode".equals(methodName)) {
      result = System.identityHashCode(proxy);
    } else if ("toString".equals(methodName)) {
      result = "MdboraDatabaseMetaData[" + context.getUrl() + "]";
    } else if ("getPrimaryKeys".equals(methodName)) {
      result = getPrimaryKeys(
              (String) arguments[0],
              (String) arguments[1],
              (String) arguments[2]);
    } else if ("getIndexInfo".equals(methodName)) {
      result = getIndexInfo(
              (String) arguments[0],
              (String) arguments[1],
              (String) arguments[2],
              (Boolean) arguments[3],
              (Boolean) arguments[4]);
    } else if ("getImportedKeys".equals(methodName)) {
      result = getImportedKeys(
              (String) arguments[0],
              (String) arguments[1],
              (String) arguments[2]);
    } else if ("getExportedKeys".equals(methodName)) {
      result = getExportedKeys(
              (String) arguments[0],
              (String) arguments[1],
              (String) arguments[2]);
    } else if ("getCrossReference".equals(methodName)) {
      result = getCrossReference(
              (String) arguments[0],
              (String) arguments[1],
              (String) arguments[2],
              (String) arguments[3],
              (String) arguments[4],
              (String) arguments[5]);
    } else if ("getCatalogs".equals(methodName)) {
      result = getCatalogs();
    } else if ("getCatalogTerm".equals(methodName)) {
      result = "";
    } else if (isUnsupportedCatalogCapability(methodName)) {
      result = false;
    } else if ("getSchemas".equals(methodName)) {
      result = getSchemas(arguments);
    } else {
      result = invokeDelegate(method, arguments);
    }

    return result;
  }

  private static Object unwrap(Object proxy, Object[] arguments) throws SQLException {
    Class<?> requestedType = (Class<?>) arguments[0];
    Object result = null;

    if (requestedType.isInstance(proxy)) {
      result = proxy;
    } else {
      throw new SQLException("Mdbora database metadata cannot be unwrapped as " + requestedType.getName());
    }

    return result;
  }

  private static boolean isWrapperFor(Object proxy, Object[] arguments) {
    Class<?> requestedType = (Class<?>) arguments[0];

    return requestedType.isInstance(proxy);
  }

  private Object invokeDelegate(Method method, Object[] arguments) throws Throwable {
    try {
      return method.invoke(delegate, arguments);
    } catch (InvocationTargetException exception) {
      throw exception.getCause();
    } catch (IllegalAccessException exception) {
      throw new SQLException("Unable to invoke database metadata method: " + method.getName(), exception);
    }
  }

  /**
   * Returns the primary key columns defined for an Access table.
   *
   * @param catalog catalog name, currently ignored
   * @param schema schema name
   * @param tableName Access table name
   * @return JDBC primary-key metadata
   * @throws SQLException if the Access table cannot be inspected
   */
  private ResultSet getPrimaryKeys(String catalog, String schema, String tableName) throws SQLException {
    SimpleResultSet result = createPrimaryKeysResultSet();
    boolean validRequest = acceptsCatalog(catalog) && acceptsSchema(schema) && tableName != null;

    if (validRequest) {
      Table table = findTable(tableName);

      if (table != null) {
        Index primaryKey = findPrimaryKey(table);

        if (primaryKey != null) {
          appendPrimaryKey(result, table, primaryKey);
        }
      }
    }

    return result;
  }

  private static SimpleResultSet createPrimaryKeysResultSet() {
    SimpleResultSet result = new SimpleResultSet();

    result.addColumn("TABLE_CAT", Types.VARCHAR, 0, 0);
    result.addColumn("TABLE_SCHEM", Types.VARCHAR, 0, 0);
    result.addColumn("TABLE_NAME", Types.VARCHAR, 0, 0);
    result.addColumn("COLUMN_NAME", Types.VARCHAR, 0, 0);
    result.addColumn("KEY_SEQ", Types.SMALLINT, 0, 0);
    result.addColumn("PK_NAME", Types.VARCHAR, 0, 0);

    return result;
  }

  private static Index findPrimaryKey(Table table) {
    Index result = null;

    for (Iterator<? extends Index> it = table.getIndexes().iterator(); null == result && it.hasNext();) {
      Index index = it.next();

      if (index.isPrimaryKey()) {
        result = index;
      }
    }

    return result;
  }

  private static void appendPrimaryKey(SimpleResultSet result, Table table, Index primaryKey) {
    short keySequence = 1;

    for (Index.Column column : primaryKey.getColumns()) {
      result.addRow(null, DEFAULT_SCHEMA, table.getName(), column.getName(), keySequence, primaryKey.getName());

      keySequence++;
    }
  }

  /**
   * Returns the indexes defined for an Access table.
   *
   * @param catalog catalog name, currently ignored
   * @param schema schema name
   * @param tableName Access table name
   * @param uniqueOnly whether only unique indexes should be returned
   * @param approximate whether approximate statistics are acceptable
   * @return JDBC index metadata
   * @throws SQLException if the Access table cannot be inspected
   */
  private ResultSet getIndexInfo(String catalog, String schema, String tableName, boolean uniqueOnly, boolean approximate) throws SQLException {
    SimpleResultSet result = createIndexInfoResultSet();
    boolean validRequest = acceptsCatalog(catalog) && acceptsSchema(schema) && tableName != null;

    if (validRequest) {
      Table table = findTable(tableName);

      if (table != null) {
        appendIndexes(result, table, uniqueOnly);
      }
    }

    return result;
  }

  private static SimpleResultSet createIndexInfoResultSet() {
    SimpleResultSet result = new SimpleResultSet();

    result.addColumn("TABLE_CAT", Types.VARCHAR, 0, 0);
    result.addColumn("TABLE_SCHEM", Types.VARCHAR, 0, 0);
    result.addColumn("TABLE_NAME", Types.VARCHAR, 0, 0);
    result.addColumn("NON_UNIQUE", Types.BOOLEAN, 0, 0);
    result.addColumn("INDEX_QUALIFIER", Types.VARCHAR, 0, 0);
    result.addColumn("INDEX_NAME", Types.VARCHAR, 0, 0);
    result.addColumn("TYPE", Types.SMALLINT, 0, 0);
    result.addColumn("ORDINAL_POSITION", Types.SMALLINT, 0, 0);
    result.addColumn("COLUMN_NAME", Types.VARCHAR, 0, 0);
    result.addColumn("ASC_OR_DESC", Types.VARCHAR, 0, 0);
    result.addColumn("CARDINALITY", Types.BIGINT, 0, 0);
    result.addColumn("PAGES", Types.BIGINT, 0, 0);
    result.addColumn("FILTER_CONDITION", Types.VARCHAR, 0, 0);

    return result;
  }

  private static void appendIndexes(SimpleResultSet result, Table table, boolean uniqueOnly) {
    for (Index index : table.getIndexes()) {
      boolean unique = index.isPrimaryKey() || index.isUnique();
      boolean include = !uniqueOnly || unique;

      if (include) {
        appendIndexColumns(result, table, index, unique);
      }
    }
  }

  private static void appendIndexColumns(SimpleResultSet result, Table table, Index index, boolean unique) {
    short ordinalPosition = 1;

    for (Index.Column column : index.getColumns()) {
      String sortOrder = column.isAscending() ? "A" : "D";

      result.addRow(
              null,
              DEFAULT_SCHEMA,
              table.getName(),
              !unique,
              null,
              index.getName(),
              DatabaseMetaData.tableIndexOther,
              ordinalPosition,
              column.getName(),
              sortOrder,
              0L,
              0L,
              null);

      ordinalPosition++;
    }
  }

  /**
   * Resolves the requested Access table for a metadata operation.
   *
   * @param tableName requested Access table name
   * @return resolved Jackcess table
   * @throws SQLException if the table cannot be resolved
   */
  private Table findTable(String tableName) throws SQLException {

    try {
      return AccessTableResolver.requireTable(context.getDatabaseId(), tableName);
    } catch (RuntimeException exception) {
      throw new SQLException("Unable to read Access metadata for table: " + tableName, exception);
    }
  }

  private static boolean acceptsSchema(String schema) {
    return schema == null || schema.isEmpty() || DEFAULT_SCHEMA.equals(schema.toUpperCase(Locale.ROOT));
  }

  private static boolean acceptsCatalog(String catalog) {
    return catalog == null || catalog.isEmpty();

  }

  /**
   * Returns the foreign keys imported by the requested Access table.
   *
   * <p>
   * An imported key is a foreign key defined in the requested table that references a key in another table.</p>
   *
   * @param catalog catalog name
   * @param schema schema name
   * @param tableName foreign-key table name
   * @return JDBC foreign-key metadata
   * @throws SQLException if the Access relationships cannot be read
   */
  private ResultSet getImportedKeys(String catalog, String schema, String tableName) throws SQLException {
    SimpleResultSet result = createForeignKeysResultSet();
    boolean validRequest = acceptsCatalog(catalog) && acceptsSchema(schema) && tableName != null;

    if (validRequest) {
      readRelationships(result, null, tableName);
    }

    return result;
  }

  /**
   * Returns the foreign keys exported by the requested Access table.
   *
   * <p>
   * An exported key is a foreign key in another table that references a key in the requested table.</p>
   *
   * @param catalog catalog name
   * @param schema schema name
   * @param tableName referenced table name
   * @return JDBC foreign-key metadata
   * @throws SQLException if the Access relationships cannot be read
   */
  private ResultSet getExportedKeys(String catalog, String schema, String tableName) throws SQLException {
    SimpleResultSet result = createForeignKeysResultSet();

    boolean validRequest = acceptsCatalog(catalog) && acceptsSchema(schema) && tableName != null;

    if (validRequest) {
      readRelationships(result, tableName, null);
    }

    return result;
  }

  /**
   * Returns foreign-key relationships between the requested parent and foreign Access tables.
   *
   * @param parentCatalog parent table catalog
   * @param parentSchema parent table schema
   * @param parentTable parent or referenced table name
   * @param foreignCatalog foreign table catalog
   * @param foreignSchema foreign table schema
   * @param foreignTable foreign-key table name
   * @return JDBC foreign-key metadata
   * @throws SQLException if the Access relationships cannot be read
   */
  private ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
    SimpleResultSet result = createForeignKeysResultSet();
    boolean validRequest = acceptsCatalog(parentCatalog) && acceptsSchema(parentSchema) && acceptsCatalog(foreignCatalog) && acceptsSchema(foreignSchema) && parentTable != null && foreignTable != null;

    if (validRequest) {
      readRelationships(result, parentTable, foreignTable);
    }

    return result;
  }

  /**
   * Creates an empty JDBC foreign-key metadata result set.
   *
   * @return empty foreign-key metadata result set
   */
  private static SimpleResultSet createForeignKeysResultSet() {
    SimpleResultSet result = new SimpleResultSet();

    result.addColumn("PKTABLE_CAT", Types.VARCHAR, 0, 0);
    result.addColumn("PKTABLE_SCHEM", Types.VARCHAR, 0, 0);
    result.addColumn("PKTABLE_NAME", Types.VARCHAR, 0, 0);
    result.addColumn("PKCOLUMN_NAME", Types.VARCHAR, 0, 0);
    result.addColumn("FKTABLE_CAT", Types.VARCHAR, 0, 0);
    result.addColumn("FKTABLE_SCHEM", Types.VARCHAR, 0, 0);
    result.addColumn("FKTABLE_NAME", Types.VARCHAR, 0, 0);
    result.addColumn("FKCOLUMN_NAME", Types.VARCHAR, 0, 0);
    result.addColumn("KEY_SEQ", Types.SMALLINT, 0, 0);
    result.addColumn("UPDATE_RULE", Types.SMALLINT, 0, 0);
    result.addColumn("DELETE_RULE", Types.SMALLINT, 0, 0);
    result.addColumn("FK_NAME", Types.VARCHAR, 0, 0);
    result.addColumn("PK_NAME", Types.VARCHAR, 0, 0);
    result.addColumn("DEFERRABILITY", Types.SMALLINT, 0, 0);

    return result;
  }

  /**
   * Reads Access relationships and appends those matching the requested parent and foreign table filters.
   *
   * <p>
   * A {@code null} parent table name accepts relationships from every referenced table. A {@code null} foreign table name accepts relationships to every foreign-key table.</p>
   *
   * <p>
   * Relationships are read lazily. This method is invoked only when a JDBC client requests imported keys, exported keys, or a cross-reference between two tables.</p>
   *
   * @param result destination JDBC foreign-key metadata result set
   * @param parentTableName referenced table filter, or {@code null} to accept every parent table
   * @param foreignTableName foreign-key table filter, or {@code null} to accept every foreign table
   * @throws SQLException if the Access relationships cannot be read
   */
  private void readRelationships(SimpleResultSet result, String parentTableName, String foreignTableName) throws SQLException {
    Database database = AccessDatabaseRegistry.require(context.getDatabaseId());

    try {
      List<Relationship> relationships = database.getRelationships();

      for (Relationship relationship : relationships) {

        boolean matches = matchesRelationship(relationship, parentTableName, foreignTableName);

        if (matches) {
          appendRelationship(result, relationship);
        }
      }
    } catch (IOException exception) {
      throw new SQLException("Unable to read Access relationships", exception);
    }
  }

  /**
   * Determines whether an Access relationship match*s the requested filters.
   *
   * @pa*am relationship Access relationshi
   *
   * @param parentTableName referen*ed table filter, or {@code null}
   * @param foreignTableName foreign-k*y table filter, or {@code null}
   * @return {@code true} if the relationship matches both filters
   */
  private static boolean matchesRelationship(Relationship relationship, String parentTableName, String foreignTableName) {
    String currentParentTable = relationship.getFromTable().getName();
    String currentForeignTable = relationship.getToTable().getName();

    boolean matchesParent = parentTableName == null || currentParentTable.equalsIgnoreCase(parentTableName);
    boolean matchesForeign = foreignTableName == null || currentForeignTable.equalsIgnoreCase(foreignTableName);

    return matchesParent && matchesForeign;
  }

  /**
   * Appends all column pairs from an Access relationship to a JDBC metadata result set.
   *
   * @param result destination JDBC metadata result set
   * @param relationship Access relationship
   */
  private static void appendRelationship(SimpleResultSet result, Relationship relationship) {
    Table parentTable = relationship.getFromTable();
    Table foreignTable = relationship.getToTable();

    List<? extends Column> parentColumns = relationship.getFromColumns();
    List<? extends Column> foreignColumns = relationship.getToColumns();

    short updateRule = getUpdateRule(relationship);
    short deleteRule = getDeleteRule(relationship);

    String primaryKeyName = findPrimaryKeyName(parentTable, parentColumns);

    int columnCount = Math.min(parentColumns.size(), foreignColumns.size());

    for (int index = 0; index < columnCount; index++) {

      Column parentColumn = parentColumns.get(index);
      Column foreignColumn = foreignColumns.get(index);

      short keySequence = (short) (index + 1);

      result.addRow(
              null,
              DEFAULT_SCHEMA,
              parentTable.getName(),
              parentColumn.getName(),
              null,
              DEFAULT_SCHEMA,
              foreignTable.getName(),
              foreignColumn.getName(),
              keySequence,
              updateRule,
              deleteRule,
              relationship.getName(),
              primaryKeyName,
              DatabaseMetaData.importedKeyNotDeferrable);
    }
  }

  /**
   * Converts the Access update rule into its JDBC metadata value.
   *
   * @param relationship Access relationship
   * @return JDBC update rule
   */
  private static short getUpdateRule(Relationship relationship) {
    short result = DatabaseMetaData.importedKeyNoAction;

    if (relationship.cascadeUpdates()) {
      result = DatabaseMetaData.importedKeyCascade;
    }

    return result;
  }

  /**
   * Converts the Access delete rule into its JDBC metadata value.
   *
   * @param relationship Access relationship
   * @return JDBC delete rule
   */
  private static short getDeleteRule(Relationship relationship) {
    short result = DatabaseMetaData.importedKeyNoAction;

    if (relationship.cascadeDeletes()) {
      result = DatabaseMetaData.importedKeyCascade;
    } else if (relationship.cascadeNullOnDelete()) {
      result = DatabaseMetaData.importedKeySetNull;
    }

    return result;
  }

  /**
   * Finds the primary or unique index referenced by a relationship.
   *
   * @param parentTable referenced Access table
   * @param relationshipColumns referenced relationship columns
   * @return matching primary or unique index name, or {@code null}
   */
  private static String findPrimaryKeyName(Table parentTable, List<? extends Column> relationshipColumns) {
    String result = null;

    for (Index index : parentTable.getIndexes()) {
      boolean candidate = index.isPrimaryKey() || index.isUnique();
      boolean matchingColumns = false;

      if (candidate) {
        matchingColumns = hasSameColumns(index, relationshipColumns);
      }

      if (result == null && matchingColumns) {
        result = index.getName();
      }
    }

    return result;
  }

  /**
   * Determines whether an index contains the supplied columns in the same order.
   *
   * @param index Access index
   * @param columns expected indexed columns
   * @return {@code true} if names and positions match
   */
  private static boolean hasSameColumns(Index index, List<? extends Column> columns) {
    List<? extends Index.Column> indexColumns = index.getColumns();

    boolean result = indexColumns.size() == columns.size();

    for (int position = 0; position < indexColumns.size() && result; position++) {
      String indexColumnName = indexColumns.get(position).getName();
      String relationshipColumnName = columns.get(position).getName();

      result = indexColumnName.equalsIgnoreCase(relationshipColumnName);
    }

    return result;
  }

  /**
   * Returns the catalogs exposed by the Mdbora connection.
   *
   * <p>
   * Mdbora currently opens one Access database per connection and does not expose it as a JDBC catalog. Therefore, this method returns an empty result set with the structure required by JDBC.</p>
   *
   * @return empty JDBC catalog metadata result set
   */
  private static ResultSet getCatalogs() {
    SimpleResultSet result = new SimpleResultSet();

    result.addColumn("TABLE_CAT", Types.VARCHAR, 0, 0);

    return result;
  }

  /**
   * Determines whether a metadata method asks about an unsupported catalog capability.
   *
   * @param methodName JDBC metadata method name
   * @return {@code true} if the method represents a catalog capability that Mdbora does not support
   */
  private static boolean isUnsupportedCatalogCapability(String methodName) {
    boolean result = "supportsCatalogsInDataManipulation".equals(methodName)
            || "supportsCatalogsInIndexDefinitions".equals(methodName)
            || "supportsCatalogsInPrivilegeDefinitions".equals(methodName)
            || "supportsCatalogsInProcedureCalls".equals(methodName)
            || "supportsCatalogsInTableDefinitions".equals(methodName);

    return result;
  }

  /**
   * Returns the logical JDBC schema exposed by Mdbora.
   *
   * <p>
   * Microsoft Access files do not define schemas. Mdbora exposes Access tables through the logical {@code PUBLIC} schema for JDBC and H2 compatibility.</p>
   *
   * @return JDBC schema metadata containing only {@code PUBLIC}
   */
  private static ResultSet getSchemas() {
    SimpleResultSet result = createSchemasResultSet();

    result.addRow(DEFAULT_SCHEMA, null);

    return result;
  }

  private static SimpleResultSet createSchemasResultSet() {
    SimpleResultSet result = new SimpleResultSet();

    result.addColumn("TABLE_SCHEM", Types.VARCHAR, 0, 0);
    result.addColumn("TABLE_CATALOG", Types.VARCHAR, 0, 0);

    return result;
  }

  /**
   * Returns the logical JDBC schemas matching the requested filters.
   *
   * @param catalog requested catalog
   * @param schemaPattern requested schema pattern
   * @return matching JDBC schema metadata
   */
  private static ResultSet getSchemas(String catalog, String schemaPattern) {
    SimpleResultSet result = createSchemasResultSet();

    boolean include = acceptsCatalog(catalog) && matchesSchemaPattern(schemaPattern);

    if (include) {
      result.addRow(DEFAULT_SCHEMA, null);
    }

    return result;
  }

  /**
   * Determines whether a JDBC schema pattern matches the logical schema exposed by Mdbora.
   *
   * <p>
   * The percent character matches any sequence of characters, and the underscore character matches one character. Pattern matching is case-insensitive.</p>
   *
   * @param schemaPattern requested JDBC schema pattern
   * @return {@code true} if the pattern matches the {@code PUBLIC} schema
   */
  private static boolean matchesSchemaPattern(String schemaPattern) {
    boolean result = true;

    if (schemaPattern != null && !schemaPattern.isBlank()) {
      String regularExpression = schemaPattern
              .replace("\\", "\\\\")
              .replace(".", "\\.")
              .replace("[", "\\[")
              .replace("]", "\\]")
              .replace("(", "\\(")
              .replace(")", "\\)")
              .replace("{", "\\{")
              .replace("}", "\\}")
              .replace("+", "\\+")
              .replace("*", "\\*")
              .replace("?", "\\?")
              .replace("^", "\\^")
              .replace("$", "\\$")
              .replace("|", "\\|")
              .replace("%", ".*")
              .replace("_", ".");

      result = DEFAULT_SCHEMA.matches("(?i)^" + regularExpression + "$");
    }

    return result;
  }

  /**
   * Handles both JDBC variants of {@code getSchemas}.
   *
   * @param arguments method arguments, or {@code null} for the variant without filters
   * @return schemas exposed by Mdbora
   */
  private static ResultSet getSchemas(Object[] arguments) {
    ResultSet result;

    if (arguments == null || arguments.length == 0) {
      result = getSchemas();
    } else {
      result = getSchemas((String) arguments[0], (String) arguments[1]);
    }

    return result;
  }
}
