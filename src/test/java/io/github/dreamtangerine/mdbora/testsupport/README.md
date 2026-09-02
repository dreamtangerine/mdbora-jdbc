# Large MDB Fixture Generator for Mdbora

This utility generates synthetic and deterministic data. It does not contain any real-world data.

## Tables

- `CUSTOMER`: 100,000 rows by default, including GUID, text, Boolean, date, MONEY, and MEMO values.
- `CUSTOMER_ORDER`: 500,000 rows, single-column and compound indexes, and a nullable date column.
- `ORDER_ITEM`: 1,000,000 rows, with a compound primary key.
- `ORDER_STATUS`: Small reference table.
- `DOCUMENT_SAMPLE`: 100 rows containing GUID and OLE/BLOB values.

## Integration

Copy `LargeAccessFixtureGenerator.java` to:

```text
src/test/java/io/github/dreamtangerine/mdbora/testsupport/
```

The project already includes Jackcess as a dependency, so no additional libraries are required.

## Running the Generator

From the Mdbora project directory:

```bash
mvn test-compile exec:java \
  -Dexec.mainClass=io.github.dreamtangerine.mdbora.testsupport.LargeAccessFixtureGenerator \
  -Dexec.classpathScope=test \
  -Dexec.args="target/test-data/mdbora-large-fixture.mdb"
```

## Custom Fixture Sizes

```bash
mvn test-compile exec:java \
  -Dexec.mainClass=io.github.dreamtangerine.mdbora.testsupport.LargeAccessFixtureGenerator \
  -Dexec.classpathScope=test \
  -Dexec.args="target/test-data/mdbora-large-fixture.mdb 20000 100000 250000"
```

The numeric arguments represent, in order:

1. The number of customers
2. The number of orders
3. The number of order items

## Testing Recommendation

The large MDB fixture should not be committed to version control.

Generate it under `target/test-data`, use it for integration tests, and remove it with:

```bash
mvn clean
```

For fast unit tests, generate a smaller fixture.