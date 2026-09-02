/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.codec;

import io.github.spannm.jackcess.Column;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.h2.util.DateTimeUtils;
import org.h2.value.*;

/**
 * Converts values between Jackcess objects and H2 values used by Mdbora
 * virtual tables and indexes.
 */
public final class AccessValueCodec {

  private AccessValueCodec() {
  }

  /**
   * Converts a value read from an Access row into an H2 value.
   *
   * @param column source Access column
   * @param value value read by Jackcess, possibly {@code null}
   * @return the corresponding H2 value
   */
  public static Value toH2(Column column, Object value) {
    Value result;

    if (value == null) {
      result = ValueNull.INSTANCE;

    } else if ("GUID".equals(column.getType().name())) {
      String jdbcGuid = AccessGuidCodec.toJdbc(value.toString());

      result = ValueVarchar.get(jdbcGuid);
    } else if (value instanceof Boolean) {
      result = ValueBoolean.get((Boolean) value);
    } else if (value instanceof Byte) {
      result = ValueTinyint.get((Byte) value);
    } else if (value instanceof Short) {
      result = ValueSmallint.get((Short) value);
    } else if (value instanceof Integer) {
      result = ValueInteger.get((Integer) value);
    } else if (value instanceof Long) {
      result = ValueBigint.get((Long) value);
    } else if (value instanceof Float) {
      result = ValueReal.get((Float) value);
    } else if (value instanceof Double) {
      result = ValueDouble.get((Double) value);
    } else if (value instanceof BigDecimal) {
      result = ValueNumeric.get((BigDecimal) value);
    } else if (value instanceof byte[]) {
      result = ValueVarbinary.getNoCopy((byte[]) value);
    } else if (value instanceof LocalDateTime) {
      result = timestamp((LocalDateTime) value);
    } else if (value instanceof Date) {
      Date date = (Date) value;

      Instant instant = Instant.ofEpochMilli(date.getTime());
      LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

      result = timestamp(localDateTime);
    } else {
      result = ValueVarchar.get(value.toString());
    }

    return result;
  }

  /**
   * Converts an H2 search value into the representation expected by a
   * Jackcess index key.
   *
   * @param column indexed Access column
   * @param value H2 search value
   * @return value suitable for a Jackcess index lookup
   */
  public static Object toJackcessIndex(Column column, Value value) {

    String accessType = column.getType().name();
    Object result;

    switch (accessType) {
      case "GUID":
        String jdbcGuid = value.getString();
        result = AccessGuidCodec.toJackcess(jdbcGuid);
        break;
      case "BOOLEAN":
        result = value.getBoolean();
        break;
      case "BYTE":
        result = value.getByte();
        break;
      case "INT":
        result = value.getShort();
        break;
      case "LONG":
        result = value.getInt();
        break;
      case "BIG_INT":
        result = value.getLong();
        break;
      case "FLOAT":
        result = value.getFloat();
        break;
      case "DOUBLE":
        result = value.getDouble();
        break;
      case "NUMERIC":
      case "MONEY":
        result = value.getBigDecimal();
        break;
      case "BINARY":
      case "OLE":
        result = value.getBytesNoCopy();
        break;
      default:
        result = value.getString();
        break;
    }

    return result;
  }

  private static ValueTimestamp timestamp(LocalDateTime value) {
    long date = DateTimeUtils.dateValue(value.getYear(), value.getMonthValue(), value.getDayOfMonth());

    return ValueTimestamp.fromDateValueAndNanos(date, value.toLocalTime().toNanoOfDay());
  }
}
