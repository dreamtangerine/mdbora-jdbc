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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Generates small deterministic Access databases for JDBC metadata tests.
 */
public final class MetadataFixtureGenerator {

  private MetadataFixtureGenerator() {
  }

  /**
   * Generates an Access 2000 MDB fixture.
   *
   * @param output destination MDB file
   * @return absolute path of the generated database
   * @throws Exception if the fixture cannot be generated
   */
  public static Path generateV2000(Path output) throws Exception {
    return generate(output, Database.FileFormat.V2000);
  }

  /**
   * Generates an Access database with the requested file format.
   *
   * @param output destination Access file
   * @param fileFormat Access file format
   * @return absolute path of the generated database
   * @throws Exception if the fixture cannot be generated
   */
  public static Path generate(Path output, Database.FileFormat fileFormat) throws Exception {
    validateArguments(output, fileFormat);

    Path absoluteOutput = output.toAbsolutePath().normalize();
    Path parentDirectory = absoluteOutput.getParent();

    if (parentDirectory != null) {
      Files.createDirectories(parentDirectory);
    }

    Files.deleteIfExists(absoluteOutput);

    DatabaseBuilder builder = new DatabaseBuilder(absoluteOutput.toFile());

    builder.withFileFormat(fileFormat);
    builder.withAutoSync(false);

    try (Database database = builder.create()) {
      Table table = createMetadataTable(database);

      populateMetadataTable(table);

      database.flush();
    }

    return absoluteOutput;
  }

  private static void validateArguments(Path output, Database.FileFormat fileFormat) {
    if (output == null) {
      throw new IllegalArgumentException("The output path cannot be null");
    }

    if (fileFormat == null) {
      throw new IllegalArgumentException("The Access file format cannot be null");
    }
  }

  private static Table createMetadataTable(Database database) throws Exception {
    Table result = new TableBuilder(
            "METADATA_SAMPLE")
            .addColumn(
                    new ColumnBuilder(
                            "ID",
                            DataType.LONG))
            .addColumn(
                    new ColumnBuilder(
                            "GUID",
                            DataType.GUID))
            .addColumn(
                    new ColumnBuilder(
                            "NAME",
                            DataType.TEXT)
                            .withLength(100))
            .addColumn(
                    new ColumnBuilder(
                            "ACTIVE",
                            DataType.BOOLEAN))
            .addColumn(
                    new ColumnBuilder(
                            "CREATED_AT",
                            DataType.SHORT_DATE_TIME))
            .addIndex(
                    new IndexBuilder(
                            "PK_METADATA_SAMPLE")
                            .withColumns("ID")
                            .withPrimaryKey())
            .addIndex(
                    new IndexBuilder(
                            "UX_METADATA_SAMPLE_GUID")
                            .withColumns("GUID")
                            .withUnique())
            .toTable(database);

    return result;
  }

  private static void populateMetadataTable(Table table) throws Exception {
    Date baseDate = Date.from(Instant.parse("2026-01-01T00:00:00Z"));

    for (int index = 1; index <= 10; index++) {
      UUID uuid = UUID.nameUUIDFromBytes(("METADATA_SAMPLE:" + index).getBytes(StandardCharsets.UTF_8));

      String guid = "{" + uuid.toString().toUpperCase(Locale.ROOT) + "}";

      table.addRow(index, guid, "Metadata sample " + index, index % 2 == 0, new Date(baseDate.getTime() + index * 60_000L));
    }
  }
}
