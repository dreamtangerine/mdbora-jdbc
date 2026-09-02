/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.virtual;

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

  private final Table accessTable;

  AccessScanIndex(AccessVirtualTable table, Table accessTable) {
    super(table, 0, "ACCESS_SCAN", IndexColumn.wrap(table.getColumns()), 0, IndexType.createScan(false));
    
    this.accessTable = accessTable;
  }

  @Override
  public Cursor find(SessionLocal s, SearchRow first, SearchRow last, boolean reverse) {
    return new AccessCursor(accessTable, accessTable.iterator());
  }

  @Override
  public double getCost(SessionLocal s, int[] masks, TableFilter[] filters, int filter, SortOrder sort, AllColumnsForPlan all, boolean select) {
    return 10d + accessTable.getRowCount();
  }

  @Override
  public long getRowCount(SessionLocal s) {
    return accessTable.getRowCount();
  }

  @Override
  public long getRowCountApproximation(SessionLocal s) {
    return accessTable.getRowCount();
  }

  @Override
  public void add(SessionLocal s, Row row) {
    throw readOnly();
  }

  @Override
  public void remove(SessionLocal s, Row row) {
    throw readOnly();
  }

  @Override
  public void truncate(SessionLocal s) {
    throw readOnly();
  }

  @Override
  public void remove(SessionLocal s) {
  }

  @Override
  public void close(SessionLocal s) {
  }

  @Override
  public boolean needRebuild() {
    return false;
  }

  public long getDiskSpaceUsed() {
    return 0;
  }

  private DbException readOnly() {
    return DbException.getUnsupportedException("Mdbora is read only");
  }
}
