/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.jdbc;

import io.github.dreamtangerine.mdbora.access.AccessDatabaseRegistry;
import io.github.dreamtangerine.mdbora.internal.TemporaryH2;
import io.github.spannm.jackcess.Database;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Owns the resources associated with a Mdbora JDBC connection.
 */
final class MdboraConnectionContext implements AutoCloseable {

  private final String id;
  private final String url;
  private final Database database;
  private final TemporaryH2 temporaryH2;
  private boolean closed;

  /**
   * Creates a context for an open Mdbora connection.
   *
   * @param id database registry identifier
   * @param url original Mdbora JDBC URL
   * @param database open Access database
   * @param temporaryH2 internal temporary SQL catalog
   */
  MdboraConnectionContext(String id, String url, Database database, TemporaryH2 temporaryH2) {
    this.id = id;
    this.url = url;
    this.database = database;
    this.temporaryH2 = temporaryH2;
  }

  /**
   * Returns the original Mdbora JDBC URL.
   *
   * @return connection URL
   */
  String getUrl() {
    return url;
  }

  /**
   * Returns a description of the Access file format.
   *
   * @return Access database file format
   */
  /**
   * Returns a description of the Access file format.
   *
   * @return Access database file format
   * @throws SQLException if the file format cannot be determined
   */
  String getDatabaseProductVersion() throws SQLException {
    try {
      Database.FileFormat fileFormat = database.getFileFormat();

      return fileFormat.name();
    } catch (IOException exception) {
      throw new SQLException("Unable to determine the Access database file format", exception);
    }
  }

  String getDatabaseId() {
    return id;
  }
  
  /**
   * Indicates whether the context has been closed.
   *
   * @return {@code true} if the context is closed
   */
  synchronized boolean isClosed() {
    return closed;
  }

  /**
   * Releases the internal SQL catalog, registry entry and Access database.
   *
   * @throws Exception if one or more resources cannot be released
   */
  @Override
  public synchronized void close() throws Exception {
    if (!closed) {
      closed = true;

      Exception failure = closeTemporaryH2();
      Exception detachFailure = detachDatabase();

      failure = combineFailures(failure, detachFailure);

      Exception databaseFailure = closeDatabase();

      failure = combineFailures(failure, databaseFailure);

      if (failure != null) {
        throw failure;
      }
    }
  }

  private Exception closeTemporaryH2() {
    Exception result = null;

    try {
      temporaryH2.close();
    } catch (Exception exception) {
      result = exception;
    }

    return result;
  }

  private Exception detachDatabase() {
    Exception result = null;

    try {
      AccessDatabaseRegistry.detach(id);
    } catch (RuntimeException exception) {
      result = exception;
    }

    return result;
  }

  private Exception closeDatabase() {
    Exception result = null;

    try {
      database.close();
    } catch (Exception exception) {
      result = exception;
    }

    return result;
  }

  private static Exception combineFailures(Exception currentFailure, Exception additionalFailure) {
    Exception result = currentFailure;

    if (result == null) {
      result = additionalFailure;
    } else if (additionalFailure != null) {
      result.addSuppressed(additionalFailure);
    }

    return result;
  }
}
