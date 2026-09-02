/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.tools;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Locale;

/**
 * Utility that scans tables exposed by Mdbora and measures their performance through the JDBC API.
 *
 * This class does not access Jackcess or driver internals directly. Every operation is performed through JDBC.
 */
public final class MdboraDatabaseScanner {

  private static final String DEFAULT_SCHEMA = "PUBLIC";
  private static final int DEFAULT_FETCH_SIZE = 500;

  private MdboraDatabaseScanner() {
  }

  /**
   * Scans all tables without printing column metadata and uses the performance-oriented hash algorithm.
   *
   * @param connection Mdbora JDBC connection
   *
   * @throws SQLException if a JDBC operation fails
   */
  public static void scanAllTables(Connection connection) throws SQLException {
    scanAllTables(connection, false, false);
  }

  /**
   * Scans all visible tables in the {@code PUBLIC} schema.
   *
   * @param connection Mdbora JDBC connection
   * @param printColumns whether column metadata should be printed
   * @param validate whether the deep validation hash should be used
   *
   * @throws SQLException if a JDBC operation fails
   */
  public static void scanAllTables(Connection connection, boolean printColumns, boolean validate) throws SQLException {
    validateConnection(connection);

    DatabaseMetaData databaseMetadata = connection.getMetaData();

    long totalStartedAt = System.nanoTime();

    long totalRows = 0L;
    int tableCount = 0;

    System.out.printf(
            Locale.ROOT,
            "Driver JDBC: %s %s%n",
            databaseMetadata.getDriverName(),
            databaseMetadata.getDriverVersion());

    System.out.printf(
            Locale.ROOT,
            "Base de datos: %s %s%n",
            databaseMetadata.getDatabaseProductName(),
            databaseMetadata.getDatabaseProductVersion());

    System.out.printf(
            Locale.ROOT,
            "Solo lectura: %s%n",
            connection.isReadOnly());

    System.out.printf(
            Locale.ROOT,
            "Modo: %s%n%n",
            validate ? "validación" : "rendimiento");

    printHeapLimit();
    printH2MemorySettings(connection);

    /*
         * Se deja tableTypes a null para no depender del nombre exacto
         * que H2 asigne a las tablas creadas mediante TableEngine.
     */
    String[] tableTypes = null;

    try (ResultSet tables = databaseMetadata.getTables(null, DEFAULT_SCHEMA, "%", tableTypes)) {
      while (tables.next()) {
        String schemaName = tables.getString("TABLE_SCHEM");
        String tableName = tables.getString("TABLE_NAME");
        String tableType = tables.getString("TABLE_TYPE");

        if (isScannableTable(schemaName, tableName, tableType)) {
          TableScanResult result = scanTable(connection, schemaName, tableName, printColumns, validate);

          totalRows += result.getRowCount();
          tableCount++;
        }
      }
    }

    double totalSeconds = elapsedSeconds(totalStartedAt);
    double totalRowsPerSecond = calculateRate(totalRows, totalSeconds);

    printSeparator();

    System.out.printf(Locale.forLanguageTag("es-ES"), "TOTAL: %,d tablas, %,d filas, %.3f s, %,.0f filas/s%n", tableCount, totalRows, totalSeconds, totalRowsPerSecond);
  }

  /**
   * Scans a specific table.
   *
   * @param connection JDBC connection
   * @param schemaName schema name
   * @param tableName table name
   * @param printColumns whether column metadata should be printed
   * @param validate whether the deep validation hash should be calculated
   *
   * @return table scan result
   *
   * @throws SQLException if a JDBC operation fails
   */
  public static TableScanResult scanTable(Connection connection, String schemaName, String tableName, boolean printColumns, boolean validate) throws SQLException {
    validateConnection(connection);

    String qualifiedTableName = qualifiedIdentifier(schemaName, tableName);

    String sql = "SELECT * FROM " + qualifiedTableName;

    long startedAt = System.nanoTime();

    long rowCount = 0L;
    long checksum = 1L;

    try (Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      statement.setFetchSize(DEFAULT_FETCH_SIZE);

      try (ResultSet rows = statement.executeQuery(sql)) {
        ResultSetMetaData metadata = rows.getMetaData();

        int columnCount = metadata.getColumnCount();

        if (printColumns) {
          printTableColumns(schemaName, tableName, metadata);
        }

        while (rows.next()) {
          rowCount++;

          if (validate) {
            long rowHash = calculateRowHash(rows, columnCount);

            checksum = checksum * 31L + rowHash;
          } else {
            checksum = updateFastHash(rows, columnCount, checksum);
          }
        }
      }
    }

    double seconds = elapsedSeconds(startedAt);

    double rowsPerSecond = calculateRate(rowCount, seconds);

    TableScanResult result = new TableScanResult(schemaName, tableName, rowCount, seconds, rowsPerSecond, checksum, validate);

    printTableResult(result);

    return result;
  }

  /**
   * Scans a specific table without printing column metadata and uses performance mode.
   *
   * @param connection JDBC connection
   * @param tableName table name del esquema PUBLIC
   *
   * @return table scan result
   *
   * @throws SQLException if a JDBC operation fails
   */
  public static TableScanResult scanTable(Connection connection, String tableName) throws SQLException {
    return scanTable(connection, DEFAULT_SCHEMA, tableName, false, false);
  }

  private static void validateConnection(Connection connection) throws SQLException {
    if (connection == null) {
      throw new SQLException("La conexión JDBC no puede ser nula");
    }

    if (connection.isClosed()) {
      throw new SQLException("La conexión JDBC está cerrada");
    }
  }

  private static boolean isScannableTable(String schemaName, String tableName, String tableType) {
    boolean scannable;

    if (schemaName == null || tableName == null || tableType == null) {
      scannable = false;
    } else {
      boolean publicSchema = DEFAULT_SCHEMA.equalsIgnoreCase(schemaName);
      boolean systemTable = tableName.startsWith("SYS") || tableName.startsWith("INFORMATION_SCHEMA");
      boolean tableTypeSupported = isTableTypeSupported(tableType);

      scannable = publicSchema && !systemTable && tableTypeSupported;
    }

    return scannable;
  }

  private static boolean isTableTypeSupported(String tableType) {
    String normalizedType = tableType == null ? "" : tableType.toUpperCase(Locale.ROOT);
    /*
     * H2 may expose external tables with different type names depending on
     * the version. Accept types containing TABLE while excluding views.
     */
    boolean containsTable = normalizedType.contains("TABLE");
    boolean view = normalizedType.contains("VIEW");

    return containsTable && !view;
  }

  private static void printTableColumns(String schemaName, String tableName, ResultSetMetaData metadata) throws SQLException {
    int columnCount = metadata.getColumnCount();

    System.out.println();

    System.out.printf(Locale.ROOT, "Tabla: %s.%s%n", schemaName, tableName);

    for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
      String columnName = metadata.getColumnName(columnIndex);
      String columnLabel = metadata.getColumnLabel(columnIndex);
      String typeName = metadata.getColumnTypeName(columnIndex);
      int jdbcType = metadata.getColumnType(columnIndex);
      int precision = metadata.getPrecision(columnIndex);
      int scale = metadata.getScale(columnIndex);
      int nullableCode = metadata.isNullable(columnIndex);
      String nullable = nullableDescription(nullableCode);

      System.out.printf(
              Locale.ROOT,
              "    %-36s "
              + "label=%-24s "
              + "tipo=%-20s "
              + "jdbc=%-5d "
              + "precision=%-10d "
              + "escala=%-5d "
              + "nullable=%s%n",
              columnName,
              columnLabel,
              typeName,
              jdbcType,
              precision,
              scale,
              nullable);
    }

    System.out.println();
  }

  private static String nullableDescription(int nullableCode) {
    String description;

    switch (nullableCode) {
      case ResultSetMetaData.columnNoNulls:
        description = "false";
        break;

      case ResultSetMetaData.columnNullable:
        description = "true";
        break;

      default:
        description = "desconocido";
        break;
    }

    return description;
  }

  private static long updateFastHash(ResultSet rows, int columnCount, long checksum) throws SQLException {
    long result = checksum;

    for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {

      Object value = rows.getObject(columnIndex);

      if (value != null) {
        result = result * 31L + fastValueHash(value);
      }
    }

    return result;
  }

  private static int fastValueHash(Object value) {
    int hash;

    if (value instanceof byte[]) {
      byte[] bytes = (byte[]) value;

      hash = sampledBinaryHash(bytes);
    } else {
      hash = value.hashCode();
    }

    return hash;
  }

  /**
   * Calculates a lightweight hash for binary data without reading every byte.
   */
  private static int sampledBinaryHash(byte[] bytes) {
    int hash = bytes.length;

    if (bytes.length > 0) {
      int firstIndex = 0;
      int middleIndex = bytes.length / 2;
      int lastIndex = bytes.length - 1;

      hash = 31 * hash + bytes[firstIndex];
      hash = 31 * hash + bytes[middleIndex];
      hash = 31 * hash + bytes[lastIndex];
    }

    return hash;
  }

  private static long calculateRowHash(ResultSet rows, int columnCount) throws SQLException {
    long rowHash = 1L;

    for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
      Object value = rows.getObject(columnIndex);
      int valueHash = stableValueHash(value);

      rowHash = rowHash * 31L + valueHash;
    }

    return rowHash;
  }

  private static int stableValueHash(Object value) {
    int hash;

    if (value == null) {
      hash = 0;
    } else if (value instanceof byte[]) {
      hash = Arrays.hashCode((byte[]) value);
    } else if (value instanceof BigDecimal) {
      BigDecimal decimal = ((BigDecimal) value).stripTrailingZeros();

      hash = decimal.hashCode();
    } else if (value instanceof Timestamp) {
      Timestamp timestamp = (Timestamp) value;
      int millisecondHash = Long.hashCode(timestamp.getTime());

      hash = 31 * millisecondHash + timestamp.getNanos();
    } else if (value instanceof java.sql.Date) {
      java.sql.Date date = (java.sql.Date) value;

      hash = Long.hashCode(date.getTime());
    } else if (value instanceof java.sql.Time) {
      java.sql.Time time = (java.sql.Time) value;

      hash = Long.hashCode(time.getTime());
    } else if (value instanceof java.util.Date) {
      java.util.Date date = (java.util.Date) value;

      hash = Long.hashCode(date.getTime());

    } else {
      hash = value.hashCode();
    }

    return hash;
  }

  private static String qualifiedIdentifier(String schemaName, String tableName) {
    String qualifiedName;

    if (schemaName == null || schemaName.isBlank()) {
      qualifiedName = identifier(tableName);
    } else {
      qualifiedName = identifier(schemaName) + "." + identifier(tableName);
    }

    return qualifiedName;
  }

  private static String identifier(String value) {
    String escapedValue = value.replace("\"", "\"\"");
    String identifier = "\"" + escapedValue + "\"";

    return identifier;
  }

  private static double elapsedSeconds(long startedAt) {
    long elapsedNanos = System.nanoTime() - startedAt;
    double seconds = elapsedNanos / 1_000_000_000.0d;

    return seconds;
  }

  private static double calculateRate(long rowCount, double seconds) {
    double rowsPerSecond;

    if (seconds > 0.0d) {
      rowsPerSecond = rowCount / seconds;
    } else {
      rowsPerSecond = rowCount;
    }

    return rowsPerSecond;
  }

  private static void printTableResult(TableScanResult result) {
    System.out.printf(
            Locale.forLanguageTag("es-ES"),
            "%-36s %,12d filas  "
            + "%8.3f s  "
            + "%,12.0f filas/s  "
            + "hash=%d  "
            + "modo=%s%n",
            result.getTableName(),
            result.getRowCount(),
            result.getSeconds(),
            result.getRowsPerSecond(),
            result.getChecksum(),
            result.isValidation()
            ? "validación"
            : "rendimiento");
  }

  private static void printSeparator() {
    System.out.println(
            "------------------------------------------------"
            + "------------------------------------------------");
  }

  /**
   * Immutable result of a table scan.
   */
  public static final class TableScanResult {

    private final String schemaName;
    private final String tableName;
    private final long rowCount;
    private final double seconds;
    private final double rowsPerSecond;
    private final long checksum;
    private final boolean validation;

    private TableScanResult(String schemaName, String tableName, long rowCount, double seconds, double rowsPerSecond, long checksum, boolean validation) {
      this.schemaName = schemaName;
      this.tableName = tableName;
      this.rowCount = rowCount;
      this.seconds = seconds;
      this.rowsPerSecond = rowsPerSecond;
      this.checksum = checksum;
      this.validation = validation;
    }

    /**
     * Returns the schema containing the scanned table.
     *
     * @return schema name
     */
    public String getSchemaName() {
      return schemaName;
    }

    /**
     * Returns the scanned table name.
     *
     * @return table name
     */
    public String getTableName() {
      return tableName;
    }

    /**
     * Returns the number of rows read from the table.
     *
     * @return row count
     */
    public long getRowCount() {
      return rowCount;
    }

    /**
     * Returns the elapsed scan time in seconds.
     *
     * @return elapsed seconds
     */
    public double getSeconds() {
      return seconds;
    }

    /**
     * Returns the measured scan throughput.
     *
     * @return rows read per second
     */
    public double getRowsPerSecond() {
      return rowsPerSecond;
    }

    /**
     * Returns the checksum calculated while scanning rows.
     *
     * @return calculated checksum
     */
    public long getChecksum() {
      return checksum;
    }

    /**
     * Indicates whether the validation hashing mode was used.
     *
     * @return {@code true} for validation mode
     */
    public boolean isValidation() {
      return validation;
    }
  }

  private static void printHeapLimit() {
    long maxMemory = Runtime.getRuntime().maxMemory();

    double maxMemoryMegabytes = maxMemory / 1024.0d / 1024.0d;

    System.out.printf(Locale.ROOT, "Heap máximo JVM: %.2f MB%n", maxMemoryMegabytes);
  }

  private static void printH2MemorySettings(Connection connection) throws SQLException {
    printH2DatabaseSettings(connection);
    printH2SessionMemorySettings(connection);
  }

  private static void printH2DatabaseSettings(Connection connection) throws SQLException {

    String sql
            = "SELECT SETTING_NAME, SETTING_VALUE "
            + "FROM INFORMATION_SCHEMA.SETTINGS "
            + "WHERE UPPER(SETTING_NAME) LIKE '%MEMORY%' "
            + "   OR UPPER(SETTING_NAME) LIKE '%CACHE%' "
            + "ORDER BY SETTING_NAME";

    try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {

      System.out.println("Configuración de base H2:");

      boolean found = false;

      while (result.next()) {
        found = true;

        String name = result.getString("SETTING_NAME");

        String value = result.getString("SETTING_VALUE");

        System.out.printf(Locale.ROOT, "    %-36s %s%n", name, value);
      }

      if (!found) {
        System.out.println("    Sin ajustes explícitos de memoria.");
      }
    }
  }

  private static void printH2SessionMemorySettings(Connection connection) throws SQLException {
    String sql
            = "SELECT STATE_KEY, STATE_COMMAND "
            + "FROM INFORMATION_SCHEMA.SESSION_STATE "
            + "WHERE UPPER(STATE_COMMAND) LIKE '%MEMORY%' "
            + "   OR UPPER(STATE_COMMAND) LIKE '%CACHE%' "
            + "   OR UPPER(STATE_COMMAND) LIKE '%LAZY%' "
            + "ORDER BY STATE_KEY";

    try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
      System.out.println("Configuración de sesión H2:");
      boolean found = false;

      while (result.next()) {
        found = true;

        String key = result.getString("STATE_KEY");
        String command = result.getString("STATE_COMMAND");

        System.out.printf(Locale.ROOT, "    %-36s %s%n", key, command);
      }

      if (!found) {
        System.out.println("    Sin ajustes de sesión relacionados.");
      }
    }
  }
}
