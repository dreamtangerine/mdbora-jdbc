/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.virtual;

import io.github.dreamtangerine.mdbora.access.AccessTableResolver;
import io.github.spannm.jackcess.Table;
import org.h2.command.query.AllColumnsForPlan;
import org.h2.engine.SessionLocal;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.index.IndexType;

import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.IndexColumn;
import org.h2.table.TableFilter;

final class AccessScanIndex extends Index {

  private final String databaseId;
  private final String tableName;

  AccessScanIndex(AccessVirtualTable table, String databaseId, String tableName) {

    super(table, 0, "ACCESS_SCAN", IndexColumn.wrap(table.getColumns()), 0, IndexType.createScan(false));

    this.databaseId = databaseId;
    this.tableName = tableName;
  }

  @Override
  public Cursor find(SessionLocal session, SearchRow first, SearchRow last, boolean reverse) {
    Table accessTable = AccessTableResolver.requireTable(databaseId, tableName);

    return new AccessCursor(accessTable, accessTable.iterator());
  }

  @Override
  public double getCost(SessionLocal session, int[] masks, TableFilter[] filters, int filter, SortOrder sortOrder, AllColumnsForPlan allColumns, boolean select) {
    Table accessTable = AccessTableResolver.requireTable(databaseId, tableName);

    return 10.0d + accessTable.getRowCount();
  }

  @Override
  public long getRowCount(SessionLocal session) {
    Table accessTable = AccessTableResolver.requireTable(databaseId, tableName);

    return accessTable.getRowCount();
  }

  @Override
  public long getRowCountApproximation(SessionLocal session) {
    Table accessTable = AccessTableResolver.requireTable(databaseId, tableName);

    return accessTable.getRowCount();
  }

  @Override
  public void add(SessionLocal session, Row row) {
    throw readOnly();
  }

  @Override
  public void remove(SessionLocal session, Row row) {
    throw readOnly();
  }

  @Override
  public void truncate(SessionLocal session) {
    throw readOnly();
  }

  @Override
  public void remove(SessionLocal session) {
  }

  @Override
  public void close(SessionLocal session) {
  }

  @Override
  public boolean needRebuild() {
    return false;
  }

  private DbException readOnly() {
    return DbException.getUnsupportedException("Mdbora is read-only");
  }
}
