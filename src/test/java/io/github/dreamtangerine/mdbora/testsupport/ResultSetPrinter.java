/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.testsupport;

import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

/**
 * Utility class for printing the structure and contents of JDBC result sets.
 *
 * <p>
 * This class is intended for tests and diagnostic tools. Printing a result set consumes all its remaining rows and leaves its cursor positioned after the last row.</p>
 */
public final class ResultSetPrinter {

  private static final String COLUMN_SEPARATOR = " | ";

  private static final String NULL_VALUE = "<null>";

  private ResultSetPrinter() {
  }

  /**
   * Prints a result set to the standard output stream.
   *
   * @param resultSet result set to print
   * @throws SQLException if the metadata or rows cannot be read
   * @throws NullPointerException if {@code resultSet} is {@code null}
   */
  public static void print(ResultSet resultSet) throws SQLException {
    print(resultSet, System.out);
  }

  /**
   * Prints a result set to the supplied output stream.
   *
   * @param resultSet result set to print
   * @param output destination output stream
   * @throws SQLException if the metadata or rows cannot be read
   * @throws NullPointerException if an argument is {@code null}
   */
  public static void print(ResultSet resultSet, PrintStream output) throws SQLException {
    Objects.requireNonNull(resultSet, "The result set cannot be null");
    Objects.requireNonNull(output, "The output stream cannot be null");

    ResultSetMetaData metadata = resultSet.getMetaData();

    int columnCount = metadata.getColumnCount();

    printColumnLabels(metadata, columnCount, output);

    printColumnTypes(metadata, columnCount, output);

    printHeaderSeparator(metadata, columnCount, output);

    int rowCount = printRows(resultSet, columnCount, output);

    output.printf(Locale.ROOT, "Rows: %,d%n", rowCount);
  }

  /**
   * Prints a title followed by a result set to the standard output stream.
   *
   * @param title diagnostic section title
   * @param resultSet result set to print
   * @throws SQLException if the metadata or rows cannot be read
   * @throws NullPointerException if an argument is {@code null}
   */
  public static void print(String title, ResultSet resultSet) throws SQLException {
    print(title, resultSet, System.out);
  }

  /**
   * Prints a title followed by a result set to the supplied output stream.
   *
   * @param title diagnostic section title
   * @param resultSet result set to print
   * @param output destination output stream
   * @throws SQLException if the metadata or rows cannot be read
   * @throws NullPointerException if an argument is {@code null}
   */
  public static void print(String title, ResultSet resultSet, PrintStream output) throws SQLException {
    Objects.requireNonNull(title, "The title cannot be null");
    Objects.requireNonNull(resultSet, "The result set cannot be null");
    Objects.requireNonNull(output, "The output stream cannot be null");

    output.println();
    output.println(title);
    output.println("=".repeat(Math.max(1, title.length())));

    print(resultSet, output);
  }

  /**
   * Prints the labels of all result set columns.
   *
   * @param metadata result set metadata
   * @param columnCount number of columns
   * @param output destination output stream
   * @throws SQLException if a column label cannot be read
   */
  private static void printColumnLabels(ResultSetMetaData metadata, int columnCount, PrintStream output) throws SQLException {
    for (int column = 1; column <= columnCount; column++) {

      printColumnSeparator(column, output);

      output.print(metadata.getColumnLabel(column));
    }

    output.println();
  }

  /**
   * Prints the JDBC type names of all result set columns.
   *
   * @param metadata result set metadata
   * @param columnCount number of columns
   * @param output destination output stream
   * @throws SQLException if a column type cannot be read
   */
  private static void printColumnTypes(ResultSetMetaData metadata, int columnCount, PrintStream output) throws SQLException {

    for (int column = 1; column <= columnCount; column++) {
      printColumnSeparator(column, output);

      output.print(metadata.getColumnTypeName(column));
    }

    output.println();
  }

  /**
   * Prints a separator below the result set header.
   *
   * @param metadata result set metadata
   * @param columnCount number of columns
   * @param output destination output stream
   * @throws SQLException if a column label cannot be read
   */
  private static void printHeaderSeparator(ResultSetMetaData metadata, int columnCount, PrintStream output) throws SQLException {
    for (int column = 1; column <= columnCount; column++) {
      printColumnSeparator(column, output);

      String columnLabel = metadata.getColumnLabel(column);
      int separatorLength = Math.max(3, columnLabel.length());

      output.print("-".repeat(separatorLength));
    }

    output.println();
  }

  /**
   * Prints all remaining rows from a result set.
   *
   * @param resultSet result set to consume
   * @param columnCount number of columns
   * @param output destination output stream
   * @return number of rows printed
   * @throws SQLException if a row cannot be read
   */
  private static int printRows(ResultSet resultSet, int columnCount, PrintStream output) throws SQLException {
    int rowCount = 0;

    while (resultSet.next()) {
      rowCount++;

      printCurrentRow(resultSet, columnCount, output);
    }

    return rowCount;
  }

  /**
   * Prints the current result set row.
   *
   * @param resultSet result set positioned on a row
   * @param columnCount number of columns
   * @param output destination output stream
   * @throws SQLException if a column value cannot be read
   */
  private static void printCurrentRow(ResultSet resultSet, int columnCount, PrintStream output) throws SQLException {
    for (int column = 1; column <= columnCount; column++) {
      printColumnSeparator(column, output);

      Object value = resultSet.getObject(column);
      String text = formatValue(value);

      output.print(text);
    }

    output.println();
  }

  /**
   * Converts a JDBC value into its diagnostic representation.
   *
   * @param value JDBC column value
   * @return formatted value
   */
  private static String formatValue(Object value) {

    String result = NULL_VALUE;

    if (value instanceof byte[]) {
      result = formatByteArray((byte[]) value);
    } else if (value != null) {
      result = value.toString();
    }

    return result;
  }

  /**
   * Formats a binary value without printing its complete contents.
   *
   * @param value binary value
   * @return binary value description
   */
  private static String formatByteArray(byte[] value) {
    return String.format(Locale.ROOT, "<binary: %,d bytes>", value.length);
  }

  /**
   * Prints the separator placed before a result set column.
   *
   * @param column one-based column position
   * @param output destination output stream
   */
  private static void printColumnSeparator(int column, PrintStream output) {
    if (column > 1) {
      output.print(COLUMN_SEPARATOR);
    }
  }
}
