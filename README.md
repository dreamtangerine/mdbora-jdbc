# Mdbora JDBC

[![License: MPL 2.0](https://img.shields%202.0-blue.svg](LICENSE)
[![Java 11g.shields.io/badge/Java-11%2B-blue.svg](https://openjdk.org/)

Mdbora is a pure Java, read-only JDBC driver for Microsoft Access MDB and
ACCDB database files.

Mdbora exposes Access tables through H2 external tables backed directly by
Jackcess. Access rows are not imported into H2 before a query is executed.

## Status

Mdbora is currently under active development.

Current version:

```text
0.1.0-alpha.3-SNAPSHOT
```

This is an alpha version intended for testing and evaluation. The public API
and internal implementation may change before the first stable release.

## Features

* Pure Java implementation
* Java 11 or later
* Read-only JDBC connections
* MDB and ACCDB support when the file can be opened by Jackcess
* Automatic JDBC driver discovery
* Virtual H2 tables backed directly by Access tables
* Lazy query execution
* Lazy resolution of Jackcess tables and indexes
* Lazy JDBC metadata for primary keys, indexes and relationships
* Sequential Access table scans
* Access indexes exposed to the H2 query optimizer
* Exact indexed searches using complete index keys
* Single-column and compound Access indexes
* JDBC metadata for primary keys and indexes
* JDBC metadata for imported and exported foreign keys
* JDBC cross-reference metadata for Access relationships
* GUID normalization between JDBC and Access
* Prepared statement support through the internal H2 SQL engine
* Configurable internal cache limits
* Configurable in-memory row limits
* Optional support for linked Access tables
* Temporary H2 catalogs removed when a connection is closed
* Low-memory query execution

## Known limitations

* Mdbora is read-only.
* Insert, update, delete and schema modification operations are not supported.
* Range index scans are not fully implemented.
* Partial compound-index searches are not fully implemented.
* Saved Access queries are not currently exposed.
* Some Access-specific data types may require additional validation.
* Some advanced JDBC metadata remains under development.
* Linked tables that use unsupported external data sources cannot be opened.
* Compatibility with third-party JDBC tools is still being tested.

## Requirements

* Java 11 or later
* Maven 3.8 or later for building the project

Mdbora does not require:

* Microsoft Access
* Windows native libraries
* ODBC drivers
* DLL files

## Project identity

```text
GroupId:    io.github.dreamtangerine
ArtifactId: mdbora-jdbc
Driver:     io.github.dreamtangerine.mdbora.jdbc.MdboraDriver
URL prefix: jdbc:mdbora:
```

## Installation

### Maven

After the version has been published to Maven Central, add the following
dependency:

```xml
<dependency>
    <groupId>io.github.dreamtangerine</groupId>
    <artifactId>mdbora-jdbc</artifactId>
    <version>0.1.0-alpha.2</version>
</dependency>
```

Maven resolves H2, Jackcess and their required transitive dependencies
automatically.

### Manual installation

For manual installation, use the shaded JAR:

```text
mdbora-jdbc-0.1.0-alpha.2-all.jar
```

The shaded JAR contains Mdbora and its runtime dependencies.

Add the JAR to the application classpath:

```bash
java \\
  -cp "mdbora-jdbc-0.1.0-alpha.2-all.jar:application.jar" \\
  com.example.Main
```

On Windows, use a semicolon as the classpath separator:

```powershell
java `
  -cp "mdbora-jdbc-0.1.0-alpha.2-all.jar;application.jar" `
  com.example.Main
```

## JDBC URLs

### Linux

```text
jdbc:mdbora:/home/user/data/database.mdb
```

```text
jdbc:mdbora:/home/user/data/database.accdb
```

### Windows

```text
jdbc:mdbora:C:/data/database.mdb
```

```text
jdbc:mdbora:C:/data/database.accdb
```

User names and passwords are not currently required by the Mdbora connection.

## Basic usage

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class MdboraExample {

  private MdboraExample() {
  }

  public static void main(String\[] args) throws Exception {
    String url =
            "jdbc:mdbora:/home/user/data/database.mdb";

    String sql =
            "SELECT \* "
                    + "FROM \\"PATIENT\_HISTORY\\" "
                    + "WHERE \\"GUID\\" = ?";

    try (Connection connection =
            DriverManager.getConnection(url);
         PreparedStatement statement =
            connection.prepareStatement(sql)) {

      statement.setString(
              1,
              "0000ADF7-3266-4C06-8DDC-D8D2D851676F");

      try (ResultSet rows =
              statement.executeQuery()) {

        while (rows.next()) {
          System.out.println(
                  rows.getString("GUID"));
        }
      }
    }
  }
}
```

## Listing tables

The following example uses JDBC metadata to list the tables exposed by the
connection:

```java
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

public final class ListTablesExample {

  private ListTablesExample() {
  }

  public static void main(String\[] args) throws Exception {
    String url = "jdbc:mdbora:/home/user/data/database.mdb";

    try (Connection connection = DriverManager.getConnection(url)) {
      DatabaseMetaData metadata = connection.getMetaData();

      try (ResultSet tables = metadata.getTables(
                      null,
                      "PUBLIC",
                      "%",
                      new String\[]{"TABLE"})) {

        while (tables.next()) {
          System.out.println(tables.getString("TABLE\_NAME"));
        }
      }
    }
  }
}
```

## Connection properties

Mdbora supports the following JDBC connection properties.

### `readOnly`

Default value:

```text
true
```

Indicates whether the connection is read-only.

The current version only accepts:

```text
true
```

Attempts to disable read-only mode are rejected.

### `cacheSize`

Default value:

```text
2048
```

Memory budget for the internal H2 cache, expressed in KiB.

### `maxInMemoryRows`

Default value:

```text
1000
```

Maximum number of rows that supported operations may retain in memory before
using temporary storage.

### `includeLinkedTables`

Default value:

```text
false
```

Indicates whether tables linked to external Microsoft Access database files
should be exposed.

Example:

```java
import io.github.dreamtangerine.mdbora.config.MdboraProperty;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

Properties properties = new Properties();

properties.setProperty(
        MdboraProperty.READ\_ONLY,
        "true");

properties.setProperty(
        MdboraProperty.CACHE\_SIZE,
        "2048");

properties.setProperty(
        MdboraProperty.MAX\_IN\_MEMORY\_ROWS,
        "1000");

properties.setProperty(
        MdboraProperty.INCLUDE\_LINKED\_TABLES,
        "false");

try (Connection connection =
        DriverManager.getConnection(
                "jdbc:mdbora:/path/to/database.mdb",
                properties)) {

  // Execute queries through the connection.
}
```

Only valid Boolean values should be used for Boolean properties:

```text
true
false
```

## Linked tables

Mdbora can expose tables linked to other Microsoft Access database files.

Linked tables are disabled by default because their external targets may be:

* missing;
* inaccessible;
* located on another computer;
* located on an unavailable network drive;
* represented by a Windows path that is not available in the current
environment;
* protected by operating-system permissions;
* based on an unsupported external data source.

The default behavior is equivalent to:

```text
includeLinkedTables=false
```

With this default configuration:

* local tables are exposed;
* linked tables are ignored;
* linked tables are not opened;
* missing linked databases do not prevent the main database from opening.

This allows applications to query the local content of an Access database
even when that database contains broken or unavailable linked-table
references.

To enable linked tables:

```java
import io.github.dreamtangerine.mdbora.config.MdboraProperty;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

Properties properties = new Properties();

properties.setProperty(
        MdboraProperty.INCLUDE\_LINKED\_TABLES,
        Boolean.TRUE.toString());

try (Connection connection =
        DriverManager.getConnection(
                "jdbc:mdbora:/path/to/database.mdb",
                properties)) {

  // Local and linked tables may now be queried.
}
```

The linked-table behavior is:

### `includeLinkedTables=false`

* Local tables are exposed.
* Linked tables are ignored.
* Missing or inaccessible linked databases do not prevent the connection
from opening.

### `includeLinkedTables=true`

* Local and linked tables are exposed.
* Mdbora attempts to resolve and open linked tables.
* Opening the connection may fail if a linked database is missing,
inaccessible or unsupported.

Linked tables backed by external systems other than Access may require
additional software or connection support that Mdbora does not currently
provide.

## Lazy table resolution

Mdbora registers the virtual table definitions required by H2, but does not
retain strong references to every Jackcess table and index for the lifetime of
the connection.

Access tables and indexes are resolved when query execution or detailed JDBC
metadata requires them. When an operation finishes, Mdbora does not retain
references to the corresponding Jackcess table, column or index objects.

This approach allows Jackcess to reuse its internal table cache while making
unused table objects eligible for garbage collection when no strong
references remain.

This behavior is particularly useful for server applications that open Access
databases containing many tables but query only a small subset of them.

## Read-only behavior

Mdbora currently supports read-only access only.

The following method returns `true`:

```java
connection.isReadOnly();
```

Mdbora rejects attempts to disable read-only mode:

```java
connection.setReadOnly(false);
```

Access-backed virtual tables reject:

* insert operations;
* update operations;
* delete operations;
* truncate operations;
* table alterations;
* index creation;
* other schema modifications.

## Index support

Mdbora exposes Access indexes to the H2 query optimizer.

The current implementation supports:

* sequential table scans;
* single-column indexes;
* compound indexes;
* exact searches using complete index keys;
* unique indexes;
* primary-key indexes.

An exact search can use an Access index:

```sql
SELECT \*
FROM "CUSTOMER"
WHERE "ID" = ?
```

A complete compound key can also use an Access index:

```sql
SELECT \*
FROM "ORDER\_ITEM"
WHERE "ORDER\_ID" = ?
  AND "LINE\_NO" = ?
```

Range scans and partial compound-key searches are still being expanded.

## Index diagnostics

Index diagnostic output can be enabled with the following JVM property:

```text
-Dmdbora.traceIndexes=true
```

Example:

```bash
java \\
  -Dmdbora.traceIndexes=true \\
  -cp "mdbora-jdbc-0.1.0-alpha.2-all.jar:application.jar" \\
  com.example.Main
```

## JDBC metadata

Mdbora exposes JDBC metadata for the driver and the underlying Access
database.

Current metadata includes:

* driver name and version;
* database product and Access file format;
* JDBC URL;
* read-only and transaction capabilities;
* table names and column definitions;
* primary keys;
* unique and non-unique indexes;
* simple and compound indexes;
* imported foreign keys;
* exported foreign keys;
* cross-table relationships.

Primary-key, index and relationship metadata is resolved lazily when requested
by a JDBC client.

Only relationships declared in the Access database are exposed. Mdbora does
not infer or create relationships.

Mdbora currently exposes no JDBC catalog and uses `PUBLIC` as its schema:

```text
Catalog: null
Schema:  PUBLIC
```

## Building

Run the unit tests:

```bash
mvn clean test
```

Build the regular artifacts:

```bash
mvn clean package
```

The build generates:

```text
target/mdbora-jdbc-0.1.0-alpha.2.jar
target/mdbora-jdbc-0.1.0-alpha.2-sources.jar
target/mdbora-jdbc-0.1.0-alpha.2-javadoc.jar
```

Run the complete verification lifecycle:

```bash
mvn clean verify
```

Install the current version in the local Maven repository:

```bash
mvn clean install
```

The artifact is then available under:

```text
\~/.m2/repository/io/github/dreamtangerine/mdbora-jdbc/
```

## Building the shaded JAR

Generate a single JAR containing Mdbora and its runtime dependencies:

```bash
mvn clean package -Pbundle
```

The additional output is:

```text
target/mdbora-jdbc-0.1.0-alpha.2-all.jar
```

The regular JAR is intended for Maven and Gradle consumers.

The shaded JAR is intended for:

* manual installation;
* JDBC tools;
* IDE database explorers;
* environments where distributing a single JAR is more convenient.

H2 remains an internal implementation component of Mdbora. Some JDBC tools
may still detect the H2 driver class when they scan every class contained in
the shaded JAR. When configuring such a tool, select or enter the Mdbora
driver class explicitly:

```text
io.github.dreamtangerine.mdbora.jdbc.MdboraDriver
```

## Using Mdbora in NetBeans

Generate the shaded JAR:

```bash
mvn clean package -Pbundle
```

In NetBeans:

1. Open the **Services** window.
2. Expand **Databases**.
3. Right-click **Drivers**.
4. Select **New Driver**.
5. Add the shaded Mdbora JAR.
6. Enter the driver class manually if it is not selected automatically.
7. Use `Mdbora JDBC` as the driver name.

Driver class:

```text
io.github.dreamtangerine.mdbora.jdbc.MdboraDriver
```

Example URL:

```text
jdbc:mdbora:/home/user/data/database.mdb
```

Leave the user name and password fields empty.

Some NetBeans versions may detect `org.h2.Driver` when scanning the shaded
JAR because H2 is included as an internal dependency. Select the Mdbora driver
class manually in that case.

After the connection is created, NetBeans can use the JDBC metadata exposed by
Mdbora to display:

* tables and columns;
* primary keys;
* unique and non-unique indexes;
* declared foreign-key relationships.

## Running the included example

Compile the project:

```bash
mvn clean package
```

Run the included command-line example:

```bash
mvn exec:java \\
  -Dexec.mainClass=io.github.dreamtangerine.mdbora.jdbc.DriverExample \\
  -Dexec.args="/path/to/database.mdb"
```

Run a query:

```bash
mvn exec:java \\
  -Dexec.mainClass=io.github.dreamtangerine.mdbora.jdbc.DriverExample \\
  -Dexec.args='/path/to/database.mdb SELECT \* FROM "CUSTOMER"'
```

For complex SQL statements, using a Java program with `PreparedStatement` is
recommended instead of passing SQL through shell arguments.

## Synthetic test fixtures

The test suite contains utilities that can generate deterministic synthetic
MDB databases.

The generated databases:

* contain no real-world data;
* contain no customer data;
* can include large tables;
* include GUID, text, Boolean, date, MONEY, MEMO and OLE/BLOB values;
* include single-column and compound indexes;
* can be used for functional, integration and performance testing.

Example:

```bash
mvn test-compile exec:java \\
  -Dexec.mainClass=io.github.dreamtangerine.mdbora.testsupport.LargeAccessFixtureGenerator \\
  -Dexec.classpathScope=test \\
  -Dexec.args="target/test-data/mdbora-large-fixture.mdb"
```

Custom sizes:

```bash
mvn test-compile exec:java \\
  -Dexec.mainClass=io.github.dreamtangerine.mdbora.testsupport.LargeAccessFixtureGenerator \\
  -Dexec.classpathScope=test \\
  -Dexec.args="target/test-data/mdbora-large-fixture.mdb 20000 100000 250000"
```

The numeric arguments represent, in order:

1. number of customers;
2. number of orders;
3. number of order items.

Large generated MDB fixtures should not be committed to version control.
Generate them under:

```text
target/test-data/
```

and remove them with:

```bash
mvn clean
```

## Third-party components

Mdbora JDBC is built on the following open-source components.

### H2 Database Engine

Project website:

https://h2database.com/

Source repository:

https://github.com/h2database/h2database

H2 is used as the internal SQL engine and query optimizer.

H2 is dual-licensed under:

* Mozilla Public License 2.0;
* Eclipse Public License 1.0.

Mdbora uses H2 under the Mozilla Public License 2.0 option.

### Jackcess

Source repository:

https://github.com/spannm/jackcess

Project documentation:

https://jackcess.sourceforge.io/

Jackcess is used to read Microsoft Access MDB and ACCDB database files.

Jackcess is licensed under the Apache License 2.0.

### Transitive dependencies

H2, Jackcess and their dependencies may include additional open-source
components. Those components remain subject to their respective copyright
notices and license terms.

When Mdbora is consumed through Maven, dependencies are resolved as separate
artifacts.

When distributing the shaded JAR, all applicable notices and license files
from the packaged dependencies must be preserved.

See [`THIRD-PARTY-NOTICES.txt`](THIRD-PARTY-NOTICES.txt) for additional
information.

## Contributing

Contributions, issue reports and compatibility test results are welcome.

When reporting an issue, include:

* Mdbora version;
* Java version;
* operating system;
* Access file format;
* JDBC URL with sensitive paths removed;
* SQL statement when applicable;
* complete exception and cause;
* whether linked tables are present;
* whether `includeLinkedTables` is enabled.

Do not attach databases containing:

* real customer data;
* personal information;
* medical information;
* credentials;
* confidential business data.

Synthetic and anonymized fixtures are preferred.

## License

Mdbora JDBC is licensed under the Mozilla Public License 2.0.

Mdbora may be used in open-source and proprietary applications. When
modifications to MPL-covered source files are distributed, those modified
files must remain available under the Mozilla Public License 2.0.

See the [`LICENSE`](LICENSE) file for the complete license text.