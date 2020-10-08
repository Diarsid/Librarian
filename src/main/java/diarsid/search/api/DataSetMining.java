package diarsid.search.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class DataSetMining {

    public static void main(String[] args) throws Exception {
        Path d = Paths.get("D:/");
        Files.walk(d, 4)
                .filter(path -> Files.isDirectory(path))
//                .map(path -> path.getFileName())
//                .filter(Objects::nonNull)
                .forEach(path -> {
                    try {
                        System.out.println(path.toString());
                    }
                    catch (Exception e) {

                    }
                });
    }
}
