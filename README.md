# Mdbora JDBC

Mdbora is a pure Java JDBC driver for Microsoft Access MDB and ACCDB files. This initial prototype is read-only and exposes Access data through native H2 external tables backed directly by Jackcess. It does not import all rows into H2.

## Status

Prototype `0.1.0-SNAPSHOT`:

- JDK 11+
- Read-only JDBC connection
- Automatic JDBC driver discovery
- Virtual H2 tables
- Sequential `ACCESS_SCAN`
- Access indexes exposed to H2
- Exact indexed lookups for complete keys
- GUID normalization between JDBC and Access
- Prepared statements delegated to H2
- MDB and ACCDB files supported when Jackcess can open them

Pending work includes range scans, partial compound-index keys, complete metadata validation, concurrent-connection tests, saved Access queries, and additional Access data types.

## Build

```bash
mvn clean test
mvn clean package
```

## JDBC URL

Windows:

```text
jdbc:mdbora:C:/data/database.mdb
```

Linux:

```text
jdbc:mdbora:/home/user/data/database.mdb
```

## Example

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

try (Connection connection = DriverManager.getConnection(
        "jdbc:mdbora:C:/data/database.mdb")) {

    try (PreparedStatement statement = connection.prepareStatement(
            "SELECT * FROM \"PATIENT_HISTORY\" WHERE \"GUID\" = ?")) {

        statement.setString(1, "0000ADF7-3266-4C06-8DDC-D8D2D851676F");

        try (ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                System.out.println(rows.getString("GUID"));
            }
        }
    }
}
```

## Run the included example

List public tables:

```bash
mvn exec:java -Dexec.args="C:/data/database.mdb"
```

Run a query:

```bash
mvn exec:java -Dexec.args='C:/data/database.mdb SELECT * FROM "Patient"'
```

For complex SQL, using a small Java test with `PreparedStatement` is recommended instead of passing SQL through shell arguments.

## Index tracing

Enable diagnostic index output with:

```text
-Dmdbora.traceIndexes=true
```

## Read-only behavior

`Connection.isReadOnly()` returns `true`. Mdbora rejects attempts to disable read-only mode and its Access-backed virtual tables reject insert, update, delete, truncate, schema alteration, and index creation operations.

## Project identity

```text
GroupId:    io.github.dreamtangerine
ArtifactId: mdbora-jdbc
Driver:     io.github.dreamtangerine.mdbora.jdbc.MdboraDriver
URL prefix: jdbc:mdbora:
```

## Third-party components

Mdbora JDBC is built on the following open-source components:

- [H2 Database Engine](https://h2database.com/)  
  Used as the internal SQL engine and query optimizer. H2 is dual-licensed
  under the Mozilla Public License 2.0 or the Eclipse Public License 1.0.

- [Jackcess](https://github.com/spannm/jackcess)  
  Used to read Microsoft Access MDB and ACCDB files. Jackcess is licensed
  under the Apache License 2.0.

Additional transitive dependencies may be included by Maven. Their respective
copyright notices and license terms remain applicable.

See THIRD-PARTY-NOTICES.txt for additional
information.

## License

Mdbora JDBC is licensed under the Mozilla Public License 2.0.

You may use Mdbora in open-source and proprietary applications. When you
distribute modifications to MPL-covered source files, those modified files
must remain available under the MPL 2.0.

See the LICENSE file for the complete license text.