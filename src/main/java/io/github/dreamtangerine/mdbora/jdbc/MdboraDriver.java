/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;
import io.github.dreamtangerine.mdbora.config.MdboraConfiguration;
import io.github.dreamtangerine.mdbora.config.MdboraProperty;

/**
 * JDBC driver for read-only access to Microsoft Access MDB and ACCDB files.
 *
 * <p>The driver accepts URLs beginning with {@value #URL_PREFIX} and is
 * discoverable through the JDBC service-provider mechanism.</p>
 */
public final class MdboraDriver implements Driver {

  /** JDBC URL prefix recognized by this driver. */
  public static final String URL_PREFIX = "jdbc:mdbora:";

  /**
   * Creates a JDBC driver instance.
   */
  public MdboraDriver() {
  }

  static {
    try {
      DriverManager.registerDriver(new MdboraDriver());
    } catch (SQLException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    Connection result = null;

    if (acceptsURL(url)) {
      result = MdboraConnectionFactory.open(url, info);
    }

    return result;
  }

  @Override
  public boolean acceptsURL(String url) {
    return url != null && url.startsWith(URL_PREFIX);
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties properties) {
    if (properties == null) {
      properties = new Properties();
    }

    DriverPropertyInfo readOnly
            = createPropertyInfo(
                    MdboraProperty.READ_ONLY,
                    properties.getProperty(
                            MdboraProperty.READ_ONLY,
                            Boolean.toString(
                                    MdboraConfiguration.DEFAULT_READ_ONLY)),
                    "Indica si la conexión es de solo lectura. "
                    + "La versión actual solamente admite true.",
                    new String[]{"true"});

    DriverPropertyInfo cacheSize
            = createPropertyInfo(
                    MdboraProperty.CACHE_SIZE,
                    properties.getProperty(
                            MdboraProperty.CACHE_SIZE,
                            Integer.toString(
                                    MdboraConfiguration.DEFAULT_CACHE_SIZE)),
                    "Presupuesto de memoria para la caché interna, "
                    + "expresado en KiB.",
                    null);

    DriverPropertyInfo maxInMemoryRows
            = createPropertyInfo(
                    MdboraProperty.MAX_IN_MEMORY_ROWS,
                    properties.getProperty(
                            MdboraProperty.MAX_IN_MEMORY_ROWS,
                            Integer.toString(
                                    MdboraConfiguration.DEFAULT_MAX_IN_MEMORY_ROWS)),
                    "Número máximo de filas que determinadas "
                    + "operaciones pueden mantener en memoria "
                    + "antes de utilizar almacenamiento temporal.",
                    null);

    DriverPropertyInfo[] result = {
      readOnly,
      cacheSize,
      maxInMemoryRows
    };

    return result;
  }

  private static DriverPropertyInfo createPropertyInfo(String name, String value, String description, String[] choices) {
    DriverPropertyInfo propertyInfo = new DriverPropertyInfo(name, value);

    propertyInfo.description = description;
    propertyInfo.required = false;
    propertyInfo.choices = choices;

    return propertyInfo;
  }

  @Override
  public int getMajorVersion() {
    return 0;
  }

  @Override
  public int getMinorVersion() {
    return 1;
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
