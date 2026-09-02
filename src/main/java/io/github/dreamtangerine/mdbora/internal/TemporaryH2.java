/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.stream.Stream;
import io.github.dreamtangerine.mdbora.config.MdboraConfiguration;

/**
 * Manages the temporary SQL catalog used internally by Mdbora.
 *
 * The instance owns both the internal connection and its temporary directory and releases them when closed.
 */
public final class TemporaryH2 implements AutoCloseable {

  private static final int LAZY_QUERY_EXECUTION = 1;

  private final Path directory;
  private final Connection connection;

  private boolean closed;

  private TemporaryH2(Path directory, Connection connection) {
    this.directory = directory;
    this.connection = connection;
  }

  /**
   * Creates the temporary catalog and applies the internal configuration.
   *
   * @param configuration Mdbora connection configuration
   *
   * @return open temporary catalog
   *
   * @throws IOException if the temporary directory cannot be created
   * @throws SQLException if the internal connection cannot be opened or configured
   */
  public static TemporaryH2 open(MdboraConfiguration configuration) throws IOException, SQLException {
    if (configuration == null) {
      throw new IllegalArgumentException("La configuración de Mdbora no puede ser nula");
    }

    Path directory = Files.createTempDirectory("mdbora-");
    Connection connection = null;
    TemporaryH2 result = null;

    try {
      Path catalogFile = directory.resolve("catalog").toAbsolutePath();
      String url = createJdbcUrl(catalogFile);

      connection = DriverManager.getConnection(url, "sa", "");

      configure(connection, configuration);

      result = new TemporaryH2(directory, connection);
    } catch (SQLException | RuntimeException exception) {
      closeConnectionAfterFailure(connection);
      deleteDirectoryAfterFailure(directory);

      throw exception;
    }

    return result;
  }

  /**
   * Returns the internal SQL connection.
   *
   * @return internal SQL connection
   */
  public Connection connection() {
    return connection;
  }

  /**
   * Returns the temporary directory owned by this instance.
   *
   * @return temporary directory
   */
  public Path directory() {
    return directory;
  }

  /**
   * Indicates whether this instance has been closed.
   *
   * @return {@code true} if the instance is closed
   */
  public boolean isClosed() {
    return closed;
  }

  private static String createJdbcUrl(Path catalogFile) {
    return "jdbc:h2:file:" + catalogFile + ";DB_CLOSE_ON_EXIT=FALSE";
  }

  private static void configure(Connection connection, MdboraConfiguration configuration) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      setIntegerSetting(statement, "CACHE_SIZE", configuration.getCacheSize());
      setIntegerSetting(statement, "MAX_MEMORY_ROWS", configuration.getMaxInMemoryRows());
      setIntegerSetting(statement, "LAZY_QUERY_EXECUTION", LAZY_QUERY_EXECUTION);
    }
  }

  private static void setIntegerSetting(Statement statement, String settingName, int value) throws SQLException {
    String sql = "SET " + settingName + " " + value;

    statement.execute(sql);
  }

  @Override
  public synchronized void close() throws Exception {
    if (!closed) {
      closed = true;

      Exception failure = closeConnection();
      Exception deleteFailure = deleteDirectory();

      if (failure == null) {
        failure = deleteFailure;
      } else if (deleteFailure != null) {
        failure.addSuppressed(deleteFailure);
      }

      if (failure != null) {
        throw failure;
      }
    }
  }

  private Exception closeConnection() {
    Exception failure = null;

    try {
      if (!connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException exception) {
      failure = exception;
    }

    return failure;
  }

  private Exception deleteDirectory() {
    Exception failure = null;

    try {
      deleteDirectoryTree(directory);
    } catch (IOException exception) {
      failure = exception;
    }

    return failure;
  }

  private static void closeConnectionAfterFailure(Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException ignored) {
        // Se conserva la excepción original de apertura.
      }
    }
  }

  private static void deleteDirectoryAfterFailure(Path directory) {
    try {
      deleteDirectoryTree(directory);
    } catch (IOException ignored) {
      // Se conserva la excepción original de apertura.
    }
  }

  private static void deleteDirectoryTree(Path directory) throws IOException {
    if (directory != null && Files.exists(directory)) {
      try (Stream<Path> paths = Files.walk(directory)) {
        IOException[] failure = new IOException[1];

        paths.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                  if (failure[0] == null) {
                    try {
                      Files.deleteIfExists(path);
                    } catch (IOException exception) {
                      failure[0] = exception;
                    }
                  }
                });

        if (failure[0] != null) {
          throw failure[0];
        }
      }
    }
  }
}
