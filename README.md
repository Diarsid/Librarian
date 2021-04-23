# Librarian project

## Goal

## Non-goal

### Required Maven artefacts
This project require dependencies
```
<dependency>
    <groupId>diarsid</groupId>
    <artifactId>support</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>diarsid</groupId>
    <artifactId>jdbc</artifactId>
    <version>1.0</version>
</dependency>
```
They could be found at: https://github.com/Diarsid/Jdbc and https://github.com/Diarsid/Support

### Tests

`src/test` folder contains `resources/datasets/*` fileds that are imported into `resources/database/h2/*.mv.db` H2 database file.
If database-file is not present or is new and tables not found, first tests run will create database file and execute data import from those datasets.

Some tests are executed in in-memory database, and some are executed through database tcp-server using database file with imported data. Tests that use tcp-server can start it, if it is not started. 
Also, tcp-server can be started and stopped manually via
`/test/java/diarsid.tests.db.h2.H2TestDataBase$TcpServer$Start.main()`
`/test/java/diarsid.tests.db.h2.H2TestDataBase$TcpServer$Shutdown.main()`
If tcp-server is started manually, tests will run using it. Having separate tcp-server, independent of test runs is useful for connecting with some database viewer like DBeaver. 
Database url with running tcp-server is `jdbc:h2:tcp://localhost:53489/${project.basedir}/src/test/resources/database/h2/search;DB_CLOSE_DELAY=-1`
