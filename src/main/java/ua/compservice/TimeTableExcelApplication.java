package ua.compservice;

import ua.compservice.util.WorkbookUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TimeTableExcelApplication {

    private static final String FIO_SEARCH_TEXT = "Фамилия И. О.";
    private static final String TABLE_NUMBER_SEARCH_TEXT = "Таб. №";
    private static final String CURRENT_POSITION_SEARCH_TEXT = "Должность";


    public static void main(String[] args) {
        Path currentDir = Paths.get(System.getProperty("user.dir").toString()).resolve("src/files");

        Path file1 = currentDir.resolve(Paths.get("11.xlsx"));

        Path file2 = currentDir.resolve(Paths.get("46.xlsx"));

        Path to = currentDir.resolve(Paths.get("common.xlsx"));

        WorkbookUtils.merge(to, file1, file2);



    }
}
