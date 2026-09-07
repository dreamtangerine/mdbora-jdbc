/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.virtual;

import io.github.dreamtangerine.mdbora.access.AccessTableResolver;
import io.github.spannm.jackcess.CursorBuilder;
import io.github.spannm.jackcess.IndexCursor;
import io.github.spannm.jackcess.Table;
import java.io.IOException;
import java.util.Iterator;
import io.github.dreamtangerine.mdbora.codec.AccessValueCodec;
import java.util.Arrays;
import org.h2.command.query.AllColumnsForPlan;
import org.h2.engine.SessionLocal;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.index.IndexCondition;
import org.h2.index.IndexType;

import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.IndexColumn;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueNull;

final class AccessJackcessIndex extends Index {

  private static final boolean TRACE = Boolean.getBoolean("mdbora.traceIndexes");

  private final String databaseId;
  private final String tableName;
  private final String accessIndexName;

  AccessJackcessIndex(AccessVirtualTable table, int id, String name, IndexColumn[] columns, IndexType type, String databaseId, String tableName, String accessIndexName, boolean unique) {
    super(table, id, name, columns, unique ? columns.length : 0, type);

    this.databaseId = databaseId;
    this.tableName = tableName;
    this.accessIndexName = accessIndexName;
  }

  @Override
  public Cursor find(SessionLocal session, SearchRow first, SearchRow last, boolean reverse) {
    io.github.spannm.jackcess.Index accessIndex = AccessTableResolver.requireIndex(databaseId, tableName, accessIndexName);

    Cursor result;

    try {
      IndexCursor indexCursor = CursorBuilder.createCursor(accessIndex);
      Object[] exactKey = exactKey(session, first, last, accessIndex);

      if (exactKey != null) {
        trace("table=%s index=%s mode=exact-key key=%s reverse=%s", tableName, accessIndexName, formatKey(exactKey), reverse);

        Iterator<io.github.spannm.jackcess.Row> rows = indexCursor.newEntryIterable(exactKey).iterator();
        Table accessTable = accessIndex.getTable();

        result = new AccessCursor(accessTable, rows);
      } else {
        trace("table=%s index=%s mode=full-table-scan reverse=%s", tableName, accessIndexName, reverse);

        Table accessTable = accessIndex.getTable();

        result = new AccessCursor(accessTable, accessTable.iterator());
      }
    } catch (IOException exception) {
      throw DbException.convert(exception);
    }

    return result;
  }

  private Object[] exactKey(SessionLocal session, SearchRow first, SearchRow last, io.github.spannm.jackcess.Index accessIndex) {
    Object[] result = null;

    if (first != null && last != null) {
      Object[] values = new Object[indexColumns.length];
      boolean exact = true;

      for (int i = 0; i < indexColumns.length && exact; i++) {
        int columnId = indexColumns[i].column.getColumnId();
        Value firstValue = first.getValue(columnId);
        Value lastValue = last.getValue(columnId);
        boolean invalidValue = firstValue == null || lastValue == null || firstValue == ValueNull.INSTANCE || lastValue == ValueNull.INSTANCE;

        if (invalidValue) {
          exact = false;
        } else if (session.compare(firstValue, lastValue) != 0) {
          exact = false;
        } else {
          String columnName = accessIndex.getColumns().get(i).getName();
          io.github.spannm.jackcess.Column accessColumn = accessIndex.getTable().getColumn(columnName);

          values[i] = AccessValueCodec.toJackcessIndex(accessColumn, firstValue);
        }
      }

      if (exact) {
        result = values;
      }
    }

    return result;
  }

  @Override
  public double getCost(SessionLocal session, int[] masks, TableFilter[] filters, int filter, SortOrder sort, AllColumnsForPlan all, boolean select) {
    Table accessTable = AccessTableResolver.requireTable(databaseId, tableName);
    long rowCount = Math.max(1L, accessTable.getRowCount());
    IndexConditions conditions = null;

    double cost;

    if (masks == null) {
      cost = fullIndexScanCost(rowCount);
    } else {
      conditions = analyzeIndexConditions(masks);

      if (conditions.isExactKey(indexColumns.length)) {
        cost = selectiveIndexCost(rowCount, conditions);
      } else {
        cost = fullIndexScanCost(rowCount);
      }
    }

    traceCost(rowCount, conditions, cost);

    return cost;
  }

  private IndexConditions analyzeIndexConditions(int[] masks) {
    int equalities = 0;
    boolean range = false;
    boolean analyzingPrefix = true;

    for (int i = 0; i < indexColumns.length && analyzingPrefix; i++) {
      IndexColumn indexColumn = indexColumns[i];
      int columnId = indexColumn.column.getColumnId();
      int mask = masks[columnId];
      boolean equality = (mask & IndexCondition.EQUALITY) != 0;
      boolean rangeCondition = (mask & IndexCondition.RANGE) != 0;

      if (equality) {
        equalities++;
      } else {
        if (rangeCondition) {
          range = true;
        }

        analyzingPrefix = false;
      }
    }

    return new IndexConditions(equalities, range);
  }

  private static double selectiveIndexCost(long rowCount, IndexConditions conditions) {
    double cost = rowCount;

    for (int i = 0; i < conditions.equalities(); i++) {
      cost /= 20.0d;
    }

    if (conditions.hasRange()) {
      cost /= 4.0d;
    }

    cost = Math.max(2.0d, cost);

    return cost;
  }

  private static double fullIndexScanCost(long rowCount) {
    return rowCount * 10.0d;
  }

  private static final class IndexConditions {

    private final int equalities;
    private final boolean range;

    private IndexConditions(int equalities, boolean range) {
      this.equalities = equalities;
      this.range = range;
    }

    private int equalities() {
      return equalities;
    }

    private boolean hasRange() {
      return range;
    }

    private boolean isExactKey(int indexColumnCount) {
      return equalities == indexColumnCount && !range;
    }
  }

  @Override
  public long getRowCount(SessionLocal s) {
    Table accessTable = AccessTableResolver.requireTable(databaseId, tableName);

    return accessTable.getRowCount();
  }

  @Override
  public long getRowCountApproximation(SessionLocal s) {
    Table accessTable = AccessTableResolver.requireTable(databaseId, tableName);

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

  /**
   * Writes an index diagnostic message when index tracing is enabled.
   *
   * @param message diagnostic message
   * @param arguments message format arguments
   */
  private static void trace(String message, Object... arguments) {

    if (TRACE) {
      System.err.printf(java.util.Locale.ROOT, "[mdbora-index] " + message + "%n", arguments);
    }
  }

  /**
   * Formats an index key for diagnostic output.
   *
   * @param key index key values
   * @return formatted key
   */
  private static String formatKey(Object[] key) {
    return Arrays.toString(key);
  }

  /**
   * Writes diagnostic information about an index cost calculation.
   *
   * @param rowCount approximate number of rows in the table
   * @param conditions analyzed index conditions, or {@code null}
   * @param cost calculated H2 index cost
   */
  private void traceCost(long rowCount, IndexConditions conditions, double cost) {
    if (TRACE) {
      int equalities = 0;
      boolean range = false;
      boolean exactKey = false;

      if (conditions != null) {
        equalities = conditions.equalities();

        range = conditions.hasRange();

        exactKey = conditions.isExactKey(indexColumns.length);
      }

      trace("table=%s index=%s phase=cost rows=%d equalities=%d columns=%d range=%s exactKey=%s cost=%.3f", tableName, accessIndexName, rowCount, equalities, indexColumns.length, range, exactKey, cost);
    }
  }
}
