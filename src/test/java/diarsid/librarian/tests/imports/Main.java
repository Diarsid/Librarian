package diarsid.librarian.tests.imports;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.librarian.tests.CoreTestSetup;

import static java.util.Collections.sort;
import static java.util.stream.Collectors.toList;

import static diarsid.librarian.tests.CoreTestSetupStaticSingleton.server;

public class Main {

    public static void main(String[] args) throws Exception {
        CoreTestSetup server = server();

        Jdbc jdbc = server.jdbc;

        List<String> storedBooks = jdbc.doInTransaction(tx -> {
            return tx.doQueryAndStream(
                    ColumnGetter.stringOf("string_origin"),
                            "SELECT e.string_origin FROM entries e WHERE e.user_uuid = '994f2f50-af7b-4128-95aa-223d7300d515'")
                    .collect(toList());
        });

        List<String> fileBooks = new ArrayList<>();
        Consumer<String> lines = line -> {
            int authorsIndex = line.indexOf(", authors:");
            String authorsLine = line.substring(authorsIndex + ", authors:".length()).strip().trim();
            String book = line.substring(0, authorsIndex);
            fileBooks.add(book);
        };

        Files.readAllLines(Paths.get("./src/test/resources/datasets/books"))
                .forEach(lines);

        sort(fileBooks);
        sort(storedBooks);

        storedBooks.removeAll(fileBooks);

        int a = 5;
    }
}
