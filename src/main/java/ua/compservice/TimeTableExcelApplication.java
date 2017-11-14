package ua.compservice;

import ua.compservice.util.WorkbookUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TimeTableExcelApplication {

    public static void main(String[] args) {
        Path currentDir = Paths.get(System.getProperty("user.dir").toString()).resolve("src/files");

        Path file1 = currentDir.resolve(Paths.get("11.xlsx"));

        Path file2 = currentDir.resolve(Paths.get("46.xlsx"));

        Path to = currentDir.resolve(Paths.get("common.xlsx"));

        WorkbookUtils.merge(to, file1, file2);



    }
}
