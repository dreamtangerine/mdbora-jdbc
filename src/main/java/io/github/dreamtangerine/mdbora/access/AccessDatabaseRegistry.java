/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.access;

import io.github.spannm.jackcess.Database;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the association between temporary database identifiers and open
 * Jackcess database instances used by H2 external tables.
 *
 * <p>The registry is process-local and thread-safe. Registered databases must
 * be detached when their owning Mdbora connection is closed.</p>
 */
public final class AccessDatabaseRegistry {

  private static final Map<String, Database> DATABASES = new ConcurrentHashMap<>();

  private AccessDatabaseRegistry() {
  }

  /**
   * Registers an open Access database under a unique identifier.
   *
   * @param id unique identifier used by the virtual table engine
   * @param database open Jackcess database
   * @throws NullPointerException if either argument is {@code null}
   * @throws IllegalStateException if the identifier is already registered
   */
  public static void register(String id, Database database) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(database);
    
    if (DATABASES.putIfAbsent(id, database) != null) {
      throw new IllegalStateException("Base ya registrada: " + id);
    }
  }

  /**
   * Returns the database registered under the supplied identifier.
   *
   * @param id database identifier
   * @return the registered Jackcess database
   * @throws IllegalArgumentException if no database is registered with the id
   */
  public static Database require(String id) {
    Database database = DATABASES.get(id);
    
    if (database == null) {
      throw new IllegalArgumentException("Base Access no registrada: " + id);
    }
    
    return database;
  }

  /**
   * Removes a database association from the registry.
   *
   * @param id database identifier to remove
   */
  public static void detach(String id) {
    DATABASES.remove(id);
  }
}
