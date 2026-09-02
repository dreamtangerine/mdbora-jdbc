/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.testsupport;

import io.github.spannm.jackcess.ColumnBuilder;
import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.DatabaseBuilder;
import io.github.spannm.jackcess.IndexBuilder;
import io.github.spannm.jackcess.Table;
import io.github.spannm.jackcess.TableBuilder;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Generates a deterministic and sufficiently large MDB database for Mdbora tests.
 */
public final class LargeAccessFixtureGenerator {

  private static final long RANDOM_SEED = 0x4D44424F52414CL;
  private static final int DEFAULT_CUSTOMERS = 100_000;
  private static final int DEFAULT_ORDERS = 500_000;
  private static final int DEFAULT_ITEMS = 1_000_000;
  private static final int BATCH_LOG_SIZE = 50_000;

  private LargeAccessFixtureGenerator() {
  }

  /**
   * Generates an MDB fixture using command-line arguments.
   *
   * <p>
   * The first argument is the output file. The remaining optional arguments specify the number of customers, orders and order items.</p>
   *
   * @param args output file, customer count, order count and item count
   * @throws Exception if the fixture cannot be generated
   */
  public static void main(String[] args) throws Exception {

    Path output = args.length >= 1
            ? Path.of(args[0])
            : Path.of("target", "test-data", "mdbora-large-fixture.mdb");

    int customerCount = intArgument(args, 1, DEFAULT_CUSTOMERS);
    int orderCount = intArgument(args, 2, DEFAULT_ORDERS);
    int itemCount = intArgument(args, 3, DEFAULT_ITEMS);

    generate(output, customerCount, orderCount, itemCount);
  }

  /**
   * Generates a deterministic MDB fixture at the specified path.
   *
   * <p>
   * If a file already exists at the destination, it is deleted before the new fixture is created.</p>
   *
   * @param output destination MDB file
   * @param customerCount number of customer rows to generate
   * @param orderCount number of order rows to generate
   * @param itemCount number of order item rows to generate
   * @return absolute and normalized path of the generated MDB file
   * @throws Exception if the database cannot be created or populated
   */
  public static Path generate(Path output, int customerCount, int orderCount, int itemCount) throws Exception {
    validateArguments(output, customerCount, orderCount, itemCount);

    Path absoluteOutput = output.toAbsolutePath().normalize();
    Path parentDirectory = absoluteOutput.getParent();

    if (parentDirectory != null) {
      Files.createDirectories(parentDirectory);
    }

    Files.deleteIfExists(absoluteOutput);

    long startedAt = System.nanoTime();

    System.out.printf(Locale.ROOT, "Generating %s: customers=%,d orders=%,d items=%,d%n", absoluteOutput, customerCount, orderCount, itemCount);

    DatabaseBuilder builder = new DatabaseBuilder(absoluteOutput.toFile());

    builder.withFileFormat(Database.FileFormat.V2000);
    builder.withAutoSync(false);

    try (Database database = builder.create()) {
      Table customerTable = createCustomerTable(database);
      Table orderTable = createOrderTable(database);
      Table itemTable = createOrderItemTable(database);
      Table statusTable = createLookupTable(database);
      Table blobTable = createBlobTable(database);

      populateLookup(statusTable);
      populateCustomers(customerTable, customerCount);
      populateOrders(orderTable, customerCount, orderCount);
      populateOrderItems(itemTable, orderCount, itemCount);
      populateBlobs(blobTable);
      
      database.flush();
    }

    long fileSize = Files.size(absoluteOutput);
    double elapsedSeconds = (System.nanoTime() - startedAt) / 1_000_000_000.0d;

    System.out.printf(Locale.ROOT, "Generated %,d bytes in %.3f s: %s%n", fileSize, elapsedSeconds, absoluteOutput);

    return absoluteOutput;
  }

  /**
   * Validates the fixture generation arguments.
   *
   * @param output destination MDB file
   * @param customerCount number of customer rows
   * @param orderCount number of order rows
   * @param itemCount number of order item rows
   * @throws IllegalArgumentException if an argument is invalid
   */
  private static void validateArguments(Path output, int customerCount, int orderCount, int itemCount) {

    if (output == null) {
      throw new IllegalArgumentException("The output path cannot be null");
    }

    if (customerCount <= 0) {
      throw new IllegalArgumentException("The customer count must be greater than zero: " + customerCount);
    }

    if (orderCount <= 0) {
      throw new IllegalArgumentException("The order count must be greater than zero: " + orderCount);
    }

    if (itemCount < 0) {
      throw new IllegalArgumentException("The item count cannot be negative: " + itemCount);
    }
  }

  private static Table createCustomerTable(Database database) throws Exception {
    return new TableBuilder("CUSTOMER")
            .addColumn(new ColumnBuilder("ID", DataType.LONG))
            .addColumn(new ColumnBuilder("GUID", DataType.GUID))
            .addColumn(new ColumnBuilder("CODE", DataType.TEXT).withLength(24))
            .addColumn(new ColumnBuilder("NAME", DataType.TEXT).withLength(100))
            .addColumn(new ColumnBuilder("EMAIL", DataType.TEXT).withLength(120))
            .addColumn(new ColumnBuilder("ACTIVE", DataType.BOOLEAN))
            .addColumn(new ColumnBuilder("CREATED_AT", DataType.SHORT_DATE_TIME))
            .addColumn(new ColumnBuilder("CREDIT_LIMIT", DataType.MONEY))
            .addColumn(new ColumnBuilder("NOTES", DataType.MEMO))
            .addIndex(new IndexBuilder("PK_CUSTOMER").withColumns("ID").withPrimaryKey())
            .addIndex(new IndexBuilder("UX_CUSTOMER_GUID").withColumns("GUID").withUnique())
            .addIndex(new IndexBuilder("UX_CUSTOMER_CODE").withColumns("CODE").withUnique())
            .addIndex(new IndexBuilder("IX_CUSTOMER_NAME").withColumns("NAME"))
            .toTable(database);
  }

  private static Table createOrderTable(Database database) throws Exception {
    return new TableBuilder("CUSTOMER_ORDER")
            .addColumn(new ColumnBuilder("ID", DataType.LONG))
            .addColumn(new ColumnBuilder("GUID", DataType.GUID))
            .addColumn(new ColumnBuilder("CUSTOMER_ID", DataType.LONG))
            .addColumn(new ColumnBuilder("ORDER_DATE", DataType.SHORT_DATE_TIME))
            .addColumn(new ColumnBuilder("STATUS", DataType.BYTE))
            .addColumn(new ColumnBuilder("TOTAL", DataType.MONEY))
            .addColumn(new ColumnBuilder("REFERENCE", DataType.TEXT).withLength(40))
            .addColumn(new ColumnBuilder("CANCELLED_AT", DataType.SHORT_DATE_TIME))
            .addIndex(new IndexBuilder("PK_CUSTOMER_ORDER").withColumns("ID").withPrimaryKey())
            .addIndex(new IndexBuilder("UX_CUSTOMER_ORDER_GUID").withColumns("GUID").withUnique())
            .addIndex(new IndexBuilder("IX_ORDER_CUSTOMER_DATE").withColumns("CUSTOMER_ID", "ORDER_DATE"))
            .addIndex(new IndexBuilder("IX_ORDER_STATUS_DATE").withColumns("STATUS", "ORDER_DATE"))
            .toTable(database);
  }

  private static Table createOrderItemTable(Database database) throws Exception {
    return new TableBuilder("ORDER_ITEM")
            .addColumn(new ColumnBuilder("ORDER_ID", DataType.LONG))
            .addColumn(new ColumnBuilder("LINE_NO", DataType.INT))
            .addColumn(new ColumnBuilder("PRODUCT_CODE", DataType.TEXT).withLength(32))
            .addColumn(new ColumnBuilder("DESCRIPTION", DataType.TEXT).withLength(160))
            .addColumn(new ColumnBuilder("QUANTITY", DataType.INT))
            .addColumn(new ColumnBuilder("UNIT_PRICE", DataType.MONEY))
            .addColumn(new ColumnBuilder("DISCOUNT", DataType.DOUBLE))
            .addIndex(new IndexBuilder("PK_ORDER_ITEM").withColumns("ORDER_ID", "LINE_NO").withPrimaryKey())
            .addIndex(new IndexBuilder("IX_ITEM_PRODUCT").withColumns("PRODUCT_CODE"))
            .toTable(database);
  }

  private static Table createLookupTable(Database database) throws Exception {
    return new TableBuilder("ORDER_STATUS")
            .addColumn(new ColumnBuilder("ID", DataType.BYTE))
            .addColumn(new ColumnBuilder("NAME", DataType.TEXT).withLength(30))
            .addIndex(new IndexBuilder("PK_ORDER_STATUS").withColumns("ID").withPrimaryKey())
            .toTable(database);
  }

  private static Table createBlobTable(Database database) throws Exception {
    return new TableBuilder("DOCUMENT_SAMPLE")
            .addColumn(new ColumnBuilder("ID", DataType.LONG))
            .addColumn(new ColumnBuilder("CUSTOMER_GUID", DataType.GUID))
            .addColumn(new ColumnBuilder("FILE_NAME", DataType.TEXT).withLength(120))
            .addColumn(new ColumnBuilder("CONTENT", DataType.OLE))
            .addIndex(new IndexBuilder("PK_DOCUMENT_SAMPLE").withColumns("ID").withPrimaryKey())
            .addIndex(new IndexBuilder("IX_DOCUMENT_CUSTOMER").withColumns("CUSTOMER_GUID"))
            .toTable(database);
  }

  private static void populateLookup(Table table) throws Exception {
    table.addRow((byte) 0, "NEW");
    table.addRow((byte) 1, "CONFIRMED");
    table.addRow((byte) 2, "SHIPPED");
    table.addRow((byte) 3, "CANCELLED");
  }

  private static void populateCustomers(Table table, int count) throws Exception {
    Random random = new Random(RANDOM_SEED);
    Date base = Date.from(Instant.parse("2020-01-01T00:00:00Z"));

    for (int i = 1; i <= count; i++) {
      String guid = accessGuid(uuidFor("CUSTOMER", i));
      String notes = i % 25 == 0 ? "Synthetic customer with deterministic notes " + i : null;

      table.addRow(
              i,
              guid,
              String.format(Locale.ROOT, "C%010d", i),
              "Customer " + i,
              "customer" + i + "@example.test",
              i % 13 != 0,
              new Date(base.getTime() + i * 60_000L),
              BigDecimal.valueOf(1_000_00L + random.nextInt(9_000_00), 2),
              notes);

      logProgress("CUSTOMER", i, count);
    }
  }

  private static void populateOrders(Table table, int customerCount, int count) throws Exception {
    Random random = new Random(RANDOM_SEED ^ 0x0A0D3L);
    long base = Instant.parse("2021-01-01T00:00:00Z").toEpochMilli();

    for (int i = 1; i <= count; i++) {
      int customerId = 1 + random.nextInt(customerCount);
      byte status = (byte) random.nextInt(4);
      Date cancelledAt = status == 3 ? new Date(base + i * 120_000L + 3_600_000L) : null;

      table.addRow(
              i,
              accessGuid(uuidFor("ORDER", i)),
              customerId,
              new Date(base + i * 120_000L),
              status,
              BigDecimal.valueOf(100 + random.nextInt(99_900), 2),
              String.format(Locale.ROOT, "ORD-%012d", i),
              cancelledAt);

      logProgress("CUSTOMER_ORDER", i, count);
    }
  }

  private static void populateOrderItems(Table table, int orderCount, int count) throws Exception {
    Random random = new Random(RANDOM_SEED ^ 0x17EAL);
    int[] lineNumbers = new int[orderCount + 1];

    for (int i = 1; i <= count; i++) {
      int orderId = 1 + ((i - 1) % orderCount);
      int lineNo = ++lineNumbers[orderId];
      int product = 1 + random.nextInt(20_000);

      table.addRow(
              orderId,
              (short) lineNo,
              String.format(Locale.ROOT, "P%08d", product),
              "Synthetic product " + product,
              (short) (1 + random.nextInt(10)),
              BigDecimal.valueOf(100 + random.nextInt(50_000), 2),
              random.nextInt(30) / 100.0d);

      logProgress("ORDER_ITEM", i, count);
    }
  }

  private static void populateBlobs(Table table) throws Exception {
    Random random = new Random(RANDOM_SEED ^ 0xB10BL);

    for (int i = 1; i <= 100; i++) {
      byte[] content = new byte[4_096 + i * 31];

      random.nextBytes(content);

      table.addRow(
              i,
              accessGuid(uuidFor("CUSTOMER", i)),
              "document-" + i + ".bin",
              content);
    }
  }

  private static UUID uuidFor(String namespace, int id) {
    return UUID.nameUUIDFromBytes((namespace + ':' + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private static String accessGuid(UUID uuid) {
    return "{" + uuid.toString().toUpperCase(Locale.ROOT) + "}";
  }

  private static int intArgument(String[] args, int index, int defaultValue) {
    return args.length > index ? Integer.parseInt(args[index]) : defaultValue;
  }

  private static void logProgress(String table, int current, int total) {
    if (current % BATCH_LOG_SIZE == 0 || current == total) {
      System.out.printf(Locale.ROOT, "%s: %,d / %,d%n", table, current, total);
    }
  }
}
