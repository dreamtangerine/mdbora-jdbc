/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.virtual;

import io.github.dreamtangerine.mdbora.access.AccessTableResolver;
import io.github.spannm.jackcess.Index;
import io.github.spannm.jackcess.TableDefinition;
import java.util.ArrayList;
import org.h2.command.ddl.CreateTableData;
import org.h2.engine.NullsDistinct;
import org.h2.engine.SessionLocal;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.table.*;

final class AccessVirtualTable extends TableBase {

  private final ArrayList<org.h2.index.Index> indexes = new ArrayList<>();
  private final AccessScanIndex scan;

  private final String databaseId;
  private final String tableName;

  AccessVirtualTable(CreateTableData data, String databaseId, String tableName, TableDefinition definition) {
    super(data);

    this.databaseId = databaseId;
    this.tableName = tableName;

    this.scan = new AccessScanIndex(this, databaseId, tableName);

    indexes.add(scan);

    registerAccessIndexes(definition);
  }

  private void registerAccessIndexes(TableDefinition definition) {

    int indexId = 1;

    for (io.github.spannm.jackcess.Index sourceIndex : definition.getIndexes()) {
      if (registerAccessIndex(indexId, sourceIndex)) {
        indexId++;
      }
    }
  }

  private boolean registerAccessIndex(int indexId, io.github.spannm.jackcess.Index sourceIndex) {
    IndexColumn[] indexColumns = createIndexColumns(sourceIndex);
    boolean created = indexColumns.length > 0;

    if (created) {
      IndexType indexType = createIndexType(sourceIndex, indexColumns.length);
      boolean unique = sourceIndex.isPrimaryKey() || sourceIndex.isUnique();

      AccessJackcessIndex index = new AccessJackcessIndex(this, indexId, sourceIndex.getName(), indexColumns, indexType, databaseId, tableName, sourceIndex.getName(), unique);

      indexes.add(index);
    }

    return created;
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
    
    return AccessTableResolver.requireTable(databaseId, tableName).getRowCount();
  }

  @Override
  public long getRowCountApproximation(SessionLocal s) {
    return AccessTableResolver.requireTable(databaseId, tableName).getRowCount();
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
