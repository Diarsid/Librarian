# Librarian project

## Goal
Main goal of this project is to provide semantical search engine for stored strings data. It is intended for storing and searching of relatively shorts strings like sentences or phrases. It is not intended for storing a huge text bloks like book paragrahs or pages. 

Main advantage and feature of this engine in contrast to common full text search engines is that it can discern words from messy queries. For example, search string like:  
`tolknlorofrng`  
will find a string:  
`The Lord of the Rings by J.R.R. Tolkien`  
Engine will figure out that words snippets tolkn-lor-of-rngs without any separators. 

Such behaviour allows building searches that have following advantages:
 - do not require from users to put any words separations
 - tolerate typos
 - tolerate abbreviations

However, this engine has it's limitations and disadvantages:
 - it has SQL behind it
 - it's built on top of H2 database and uses it's native Java capabilities. It cannot be easily ported to other SQL databases due to it will require creation of stored procedures, which now are created in H2 as pure Java code
 - unfortunately, it's queries although being limited by joins are still making partial table scans, that means they are not fully based on indexes and are relatively slow. It limits the amount of data that can be stored and queried because searching time will increase with growing of amount of stored data.
 
Due to those limitations, it's implied, that this search engine will not be used for very large data sets. But it could be successfully used for small to medium data sets. 

## Non-goal

 - it is not a one more FTS-engine
 - it is not intended for storing blocks of text
 
## Usage

#### Main concepts

##### Entry
Entry is a basical unit of stored data that can be queried. Basical it is represents a single stored string with some metadata - id, saved time, user id etc. It is represented by class `diarsid.librarian.api.model.Entry`. Entries can be created and retrieved by repository-like object of interface `diarsid.librarian.api.Entries`.

For example:
```
User user = ...
Entries entries = ...

String lotrBookName = "The Lord of the Rings by J.R.R. Tolkien";
Entry lotrBookEntry = entries.save(user, lotrBookName);

assertThat(lotrBookEntry.string()).isEqualTo(lotrBookName);
```


##### Entry.Label and Entry.Labeled
Label is a kind of tag. Any entry, any number of entries can be joined with any number of any labels. It allows to create logical grouping of entries. For example:
```
User user = ...
Entries entries = ...
Labels labels = ...
LabeledEntries labeledEntries = ...

List<Entry> tolkienBooks = List.of(
        entries.save(user, "The Lord of the Rings by J.R.R. Tolkien"),
        entries.save(user, "The Hobbit by J.R.R. Tolkien"),
        entries.save(user, "The Silmarillion by J.R.R. Tolkien"));

Entry.Label tolkienBooksLabel = labels.getOrSave(user, "Tolkien books");

List<Entry.Labeled> labeledBooks = labeledEntries.add(tolkienBooks, tolkienBooksLabel);

for ( Entry.Labeled labeledBook : labeledBooks ) {
    assertThat(labeledBook.label()).isEqualTo(tolkienBooksLabel);
}
```


##### Pattern, PatternToEntry, and Entry.Label.Matching
Pattern is a stored query string.   
PatternToEntry is a model of search result where some entry string was found by some pattern.
```
User user = ...
Search search = ...

String pattern = "tolknlorofrng";   // let's search lord of the rings as an example data
PatternToEntry result = search
        .findAllBy(user, pattern)
        .get(0);    // let's assume that we have only 1 search result for simplicity

String expectedEntryString = "The Lord of the Rings by J.R.R. Tolkien";

assertThat(result.entry().string()).isEqualTo(expectedEntryString);
assertThat(result.pattern().string()).isEqualTo(pattern);
```
Pattern is represented by `diarsid.librarian.api.model.Pattern`.
PatternToEntry is represented by `diarsid.librarian.api.model.PatternToEntry`.

It is also possible to include labels into search. Let's assume user want to find only books, but not movies:
```
User user = ...
Entries entries = ...
Labels labels = ...
Search search = ...

Entry.Label tolkienBooksLabel = labels.getOrSave(user, "Tolkien books");

String pattern = "tolknlorofrng"; 
List<PatternToEntry> result = search.findAllBy(user, pattern, tolkienBooksLabel); 
```
If there are other entrie matching to pattern, but they do not labeled by `tolkienBooksLabel`, they will be excluded from results.

Let's assume now that user wants to find any entries by given pattern that has any of several labels, for example books OR movies. In order to do it, Entry.Label.Matching enum can be used:
```
enum Matching implements CommonEnum<Matching> {
    ANY_OF,
    ALL_OF,
    NONE_OF
}
```
as followed:
```
User user = ...
Entries entries = ...
Labels labels = ...
Search search = ...

Entry.Label tolkienBooksLabel = labels.getOrSave(user, "Tolkien books");
Entry.Label moviesLabel = labels.getOrSave(user, "movies");     // let's assume that there are stored entries bound to this label

String pattern = "tolknlorofrng"; 
List<PatternToEntry> result = search.findAllBy(user, pattern, ANY_OF, tolkienBooksLabel, moviesLabel); 
```

Matching options are:
- ANY_OF: include in results entries that are bound to **at least one** of given labels
- ALL_OF: include in results entries that are bound to **all** of given labels 
- NONE_OF: include in results entries that are **NOT** bound to **any** of given labels 
  
#### API
Central class is an interface `diarsid.librarian.api.Core` that contains other repository-like objects to operate 

#### Required Maven artefacts
This project require following dependencies (not present in Maven central, are public in my GitHub account. You need to download them and build locally)
```
<!-- common util components, like pools, collection methods etc. -->
<!-- https://github.com/Diarsid/Support -->
<dependency>
    <groupId>diarsid</groupId>
    <artifactId>support</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- algorithm for strings similarity assessment -->
<!-- https://github.com/Diarsid/Sceptre -->
<dependency>
    <groupId>diarsid</groupId>
    <artifactId>sceptre</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- util for JDBC -->
<!-- https://github.com/Diarsid/Jdbc -->
<dependency>
    <groupId>diarsid</groupId>
    <artifactId>jdbc</artifactId>
    <version>1.1.0</version>
</dependency>

<!-- console app util for tests -->
<!-- https://github.com/Diarsid/Console -->
<dependency>
    <groupId>diarsid</groupId>
    <artifactId>console</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
``` 

## Technical aspects

#### Tests

`src/test` folder contains `resources/datasets/*` fileds that are imported into `resources/database/h2/*.mv.db` H2 database file.
If database-file is not present or is new and tables not found, first tests run will create database file and execute data import from those datasets.

Some tests are executed in in-memory database, and some are executed through database tcp-server using database file with imported data. Tests that use tcp-server can start it, if it is not started.   

Also, tcp-server can be started and stopped manually via  

`/test/java/diarsid.tests.db.h2.H2TestDataBase$TcpServer$Start.main()`  
`/test/java/diarsid.tests.db.h2.H2TestDataBase$TcpServer$Shutdown.main()`  

If tcp-server is started manually, tests will run using it. Having separate tcp-server, independent of test runs is useful for connecting with some database viewer like DBeaver. 
Database url with running tcp-server is `jdbc:h2:tcp://localhost:53487/${project.basedir}/src/test/resources/database/h2/search;DB_CLOSE_DELAY=-1`
