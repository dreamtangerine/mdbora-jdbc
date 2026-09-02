/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;



/**
 * Command-line example that opens an Access database through Mdbora and
 * either lists its public tables or executes a supplied SQL query.
 */
public final class DriverExample {

  private DriverExample() {
  }

  /**
   * Runs the command-line example.
   *
   * @param args Access file path followed optionally by an SQL query
   * @throws Exception if the database cannot be opened or queried
   */
  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage: DriverExample <file.mdb|file.accdb> [SQL]");
      System.exit(2);
    }
    
    String sql = args.length > 1 ? join(args, 1) : "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' ORDER BY TABLE_NAME";
    
    try (Connection connection = DriverManager.getConnection("jdbc:mdbora:" + args[0]); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
      ResultSetMetaData md = rows.getMetaData();
    
      while (rows.next()) {
        for (int c = 1; c <= md.getColumnCount(); c++) {
          if (c > 1) {
            System.out.print(" | ");
          }
          
          System.out.print(rows.getObject(c));
        }
        
        System.out.println();
      }
    }
  }

  private static String join(String[] a, int start) {
    StringBuilder s = new StringBuilder();
    
    for (int i = start; i < a.length; i++) {
      if (i > start) {
        s.append(' ');
      }
      
      s.append(a[i]);
    }
    
    return s.toString();
  }
}
