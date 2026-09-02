/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.virtual;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.Table;
import java.util.Iterator;
import java.util.List;
import io.github.dreamtangerine.mdbora.codec.AccessValueCodec;
import org.h2.index.Cursor;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.value.Value;

final class AccessCursor implements Cursor {

  private final List<? extends Column> columns;
  private final Iterator<io.github.spannm.jackcess.Row> rows;
  private Row current;
  private long key;

  AccessCursor(Table table, Iterator<io.github.spannm.jackcess.Row> rows) {
    this.columns = List.copyOf(table.getColumns());
    this.rows = rows;
  }

  @Override
  public Row get() {
    return current;
  }

  @Override
  public SearchRow getSearchRow() {
    return current;
  }

  @Override
  public boolean next() {
    if (!rows.hasNext()) {
      current = null;
    } else {
      io.github.spannm.jackcess.Row source = rows.next();
      Value[] values = new Value[columns.size()];
      
      for (int i = 0; i < columns.size(); i++) {
        Column column = columns.get(i);
      
        values[i] = AccessValueCodec.toH2(column, source.get(column.getName()));
      }
      
      current = Row.get(values, 1);
      current.setKey(++key);
    }

    return null != current;
  }

  @Override
  public boolean previous() {
    return false;
  }
}
