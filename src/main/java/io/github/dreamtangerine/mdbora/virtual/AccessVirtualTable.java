/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.virtual;

import io.github.spannm.jackcess.Index;
import io.github.spannm.jackcess.Table;
import java.util.ArrayList;
import org.h2.command.ddl.CreateTableData;
import org.h2.engine.NullsDistinct;
import org.h2.engine.SessionLocal;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.table.*;

final class AccessVirtualTable extends TableBase {

  private final Table accessTable;
  private final ArrayList<org.h2.index.Index> indexes = new ArrayList<>();
  private final AccessScanIndex scan;

  AccessVirtualTable(CreateTableData data, Table accessTable) {
    super(data);

    this.accessTable = accessTable;
    this.scan = new AccessScanIndex(this, accessTable);

    indexes.add(scan);

    registerAccessIndexes(accessTable);
  }

  private void registerAccessIndexes(Table accessTable) {
    int indexId = 1;

    for (Index sourceIndex : accessTable.getIndexes()) {
      boolean indexCreated = registerAccessIndex(indexId, sourceIndex, accessTable);

      if (indexCreated) {
        indexId++;
      }
    }
  }

  private boolean registerAccessIndex(int indexId, Index sourceIndex, Table accessTable) {
    IndexColumn[] indexColumns = createIndexColumns(sourceIndex);
    boolean indexCreated = indexColumns.length > 0;

    if (indexCreated) {
      IndexType indexType = createIndexType(sourceIndex, indexColumns.length);
      AccessJackcessIndex index = new AccessJackcessIndex(this, indexId, sourceIndex.getName(), indexColumns, indexType, accessTable, sourceIndex);

      indexes.add(index);
    }

    return indexCreated;
  }

  private IndexColumn[] createIndexColumns(Index sourceIndex) {
    ArrayList<IndexColumn> indexColumnList = new ArrayList<>();

    for (Index.Column sourceColumn : sourceIndex.getColumns()) {
      IndexColumn indexColumn = new IndexColumn(sourceColumn.getName(), 0);

      indexColumnList.add(indexColumn);
    }

    IndexColumn[] indexColumns = indexColumnList.toArray(new IndexColumn[0]);

    if (indexColumns.length > 0) {
      IndexColumn.mapColumns(indexColumns, this);
    }

    return indexColumns;
  }

  private static IndexType createIndexType(Index sourceIndex, int columnCount) {
    IndexType indexType;

    if (sourceIndex.isUnique()) {
      indexType = IndexType.createUnique(false, false, columnCount, NullsDistinct.DISTINCT);
    } else {
      indexType = IndexType.createNonUnique(false);
    }

    return indexType;
  }

  @Override
  public org.h2.index.Index getScanIndex(SessionLocal s) {
    return scan;
  }

  @Override
  public ArrayList<org.h2.index.Index> getIndexes() {
    return indexes;
  }

  @Override
  public TableType getTableType() {
    return TableType.EXTERNAL_TABLE_ENGINE;
  }

  @Override
  public boolean isDeterministic() {
    return true;
  }

  @Override
  public boolean canGetRowCount(SessionLocal s) {
    return true;
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
  public long getMaxDataModificationId() {
    return 0;
  }

  @Override
  public boolean isInsertable() {
    return false;
  }

  @Override
  public void checkSupportAlter() {
    throw readOnly();
  }

  @Override
  public void addRow(SessionLocal s, Row row) {
    throw readOnly();
  }

  @Override
  public void removeRow(SessionLocal s, Row row) {
    throw readOnly();
  }

  @Override
  public long truncate(SessionLocal s) {
    throw readOnly();
  }

  public long getDiskSpaceUsed() {
    return 0;
  }

  @Override
  public boolean canDrop() {
    return true;
  }

  @Override
  public void close(SessionLocal s) {
  }

  @Override
  public org.h2.index.Index addIndex(SessionLocal session, String name, int id, IndexColumn[] columns, int uniqueColumnCount, IndexType type, boolean create, String comment) {
    throw readOnly();
  }

  private DbException readOnly() {
    return DbException.getUnsupportedException("Mdbora is read only");
  }
}
