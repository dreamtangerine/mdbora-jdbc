# Mdbora JDBC 0.1.0-alpha.2

This alpha release improves memory usage, linked-table handling, JDBC metadata,
and compatibility with database exploration tools such as NetBeans.

## Added

* Added the `includeLinkedTables` connection property.
* Added lazy Access table and index resolution.
* Added JDBC primary-key metadata.
* Added JDBC metadata for unique and non-unique indexes.
* Added support for simple and compound keys in JDBC metadata.
* Added JDBC imported-key metadata for Access relationships.
* Added JDBC exported-key metadata for Access relationships.
* Added JDBC cross-reference metadata between parent and foreign tables.
* Added index diagnostic tracing through `-Dmdbora.traceIndexes=true`.

## Changed

* Linked tables are excluded by default.
* Mdbora no longer retains permanent references to Jackcess table and index
objects after an operation completes.
* Primary-key, index, and relationship metadata is generated only when a JDBC
client requests it.
* H2 is opened directly as an internal implementation component instead of
relying on global JDBC driver discovery.
* The shaded JAR exposes Mdbora as its public JDBC driver. JDBC tools that scan
all bundled classes may still detect the internal H2 driver class.
* Index costs now distinguish exact complete-key searches from operations that
require a full table scan.

## Fixed

* Fixed connection failures caused by missing linked Access databases when
linked tables are disabled.
* Fixed internal H2 driver resolution in isolated JDBC environments such as
NetBeans.
* Reduced retained memory for databases containing many tables and indexes.
* Improved operation with constrained JVM heap sizes.
* Improved the visibility of primary keys, indexes, and relationships in JDBC
database exploration tools.

## Default linked-table behavior

```text
includeLinkedTables=false
```

With the default value, local tables remain available even when linked table
targets are missing or inaccessible.

To include linked tables:

```java
Properties properties = new Properties();

properties.setProperty(
        MdboraProperty.INCLUDE\_LINKED\_TABLES,
        "true");
```

When enabled, opening or querying linked tables may fail if an external target
cannot be resolved.

## JDBC metadata

This release exposes metadata for:

* primary keys;
* unique and non-unique indexes;
* compound indexes;
* imported foreign keys;
* exported foreign keys;
* cross-table relationships.

Metadata is resolved lazily. Access relationships are reported only when they
are declared in the source database. Mdbora does not infer or create missing
relationships.

Mdbora currently uses the following JDBC namespace model:

```text
Catalog: null
Schema:  PUBLIC
```

## NetBeans

When registering the shaded JAR in NetBeans, use:

```text
Driver name:  Mdbora JDBC
Driver class: io.github.dreamtangerine.mdbora.jdbc.MdboraDriver
```

Some NetBeans versions may also detect `org.h2.Driver` because H2 is included
inside the shaded JAR. Select the Mdbora driver class explicitly.

## Known limitations

* Read-only access only.
* No insert, update, delete, or schema modification support.
* Range index scans are not fully implemented.
* Partial compound-key searches are not fully implemented.
* Saved Access queries are not exposed.
* Linked external sources other than supported Access files may not be usable.
* This remains an alpha release intended for testing and evaluation.

## Requirements

* Java 11 or later

## Maven dependency

```xml
<dependency>
  <groupId>io.github.dreamtangerine</groupId>
  <artifactId>mdbora-jdbc</artifactId>
  <version>0.1.0-alpha.2</version>
</dependency>
```
