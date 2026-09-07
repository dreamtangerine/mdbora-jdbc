/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.jdbc;

import io.github.dreamtangerine.mdbora.config.MdboraConfiguration;
import io.github.dreamtangerine.mdbora.config.MdboraProperty;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * JDBC driver for read-only access to Microsoft Access MDB and ACCDB files.
 *
 * <p>
 * The driver accepts URLs beginning with {@value #URL_PREFIX} and is discoverable through the JDBC service-provider mechanism.</p>
 */
public final class MdboraDriver implements Driver {

  /**
   * JDBC URL prefix recognized by this driver.
   */
  public static final String URL_PREFIX = "jdbc:mdbora:";

  static {
    try {
      DriverManager.registerDriver(new MdboraDriver());
    } catch (SQLException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  /**
   * Creates a Mdbora JDBC driver.
   */
  public MdboraDriver() {
  }

  @Override
  public Connection connect(String url, Properties properties) throws SQLException {
    Connection result = null;

    if (acceptsURL(url)) {
      result = MdboraConnectionFactory.open(url, properties);
    }

    return result;
  }

  @Override
  public boolean acceptsURL(String url) {
    return url != null
            && url.startsWith(URL_PREFIX);
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties properties) {
    Properties effectiveProperties = properties;

    if (effectiveProperties == null) {
      effectiveProperties = new Properties();
    }

    DriverPropertyInfo[] result = {
      createReadOnlyProperty(effectiveProperties),
      createCacheSizeProperty(effectiveProperties),
      createMaxInMemoryRowsProperty(effectiveProperties),
      createIncludeLinkedTablesProperty(effectiveProperties)
    };

    return result;
  }

  private static DriverPropertyInfo createReadOnlyProperty(Properties properties) {
    String value = properties.getProperty(MdboraProperty.READ_ONLY, Boolean.toString(MdboraConfiguration.DEFAULT_READ_ONLY));

    return createPropertyInfo(
            MdboraProperty.READ_ONLY,
            value,
            "Indicates whether the connection is read-only. The current version only supports true.",
            new String[]{"true"});
  }

  private static DriverPropertyInfo createCacheSizeProperty(Properties properties) {

    String value = properties.getProperty(MdboraProperty.CACHE_SIZE, Integer.toString(MdboraConfiguration.DEFAULT_CACHE_SIZE));

    return createPropertyInfo(
            MdboraProperty.CACHE_SIZE,
            value,
            "Memory budget for the internal cache, expressed in KiB.",
            null);
  }

  private static DriverPropertyInfo createMaxInMemoryRowsProperty(Properties properties) {
    String value = properties.getProperty(MdboraProperty.MAX_IN_MEMORY_ROWS, Integer.toString(MdboraConfiguration.DEFAULT_MAX_IN_MEMORY_ROWS));

    return createPropertyInfo(
            MdboraProperty.MAX_IN_MEMORY_ROWS,
            value,
            "Maximum number of rows that supported operations may retain in memory before using temporary storage.",
            null);
  }

  private static DriverPropertyInfo createIncludeLinkedTablesProperty(Properties properties) {
    String value = properties.getProperty(MdboraProperty.INCLUDE_LINKED_TABLES, Boolean.toString(MdboraConfiguration.DEFAULT_INCLUDE_LINKED_TABLES));

    return createPropertyInfo(
            MdboraProperty.INCLUDE_LINKED_TABLES,
            value,
            "Indicates whether externally linked tables should be exposed by the connection.",
            new String[]{"true", "false"});
  }

  private static DriverPropertyInfo createPropertyInfo(String name, String value, String description, String[] choices) {
    DriverPropertyInfo result = new DriverPropertyInfo(name, value);

    result.description = description;
    result.required = false;
    result.choices = choices;

    return result;
  }

  @Override
  public int getMajorVersion() {
    return MdboraVersion.MAJOR_VERSION;
  }

  @Override
  public int getMinorVersion() {
    return MdboraVersion.MINOR_VERSION;
  }

  @Override
  public boolean jdbcCompliant() {
    return false;
  }

  @Override
  public Logger getParentLogger() {
    return Logger.getLogger("io.github.dreamtangerine.mdbora");
  }
}
