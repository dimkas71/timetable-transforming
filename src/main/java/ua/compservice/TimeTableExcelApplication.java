package ua.compservice;

import ua.compservice.model.ImmutableCell;
import ua.compservice.util.WorkbookUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class TimeTableExcelApplication {

    private static final String FIO_SEARCH_TEXT = "Фамилия И. О.";
    private static final String TABLE_NUMBER_SEARCH_TEXT = "Таб. №";
    private static final String CURRENT_POSITION_SEARCH_TEXT = "Должность";


    public static void main(String[] args) {
        Path source = Paths.get(System.getProperty("user.dir").toString() + "/src/files/11.xlsx");

        if (Files.exists(source)) {
            List<ImmutableCell> content = WorkbookUtils.from(source);


            //
            Integer fioRow = content.stream()
                    .filter(c -> c.getValue().contains(FIO_SEARCH_TEXT))
                    .map(c -> c.getRow())
                    .findFirst()
                    .orElse(-1);

            System.out.println(fioRow);

            Integer tableNumberRow = content.stream()
                    .filter(c -> c.getValue().contains(TABLE_NUMBER_SEARCH_TEXT))
                    .map(c -> c.getRow())
                    .findFirst()
                    .orElse(-1);

            Integer currentPositionRow = content.stream()
                    .filter(c -> c.getValue().contains(CURRENT_POSITION_SEARCH_TEXT))
                    .map(c -> c.getRow())
                    .findFirst()
                    .orElse(-1);


            Integer firstDayColumn =
                    content.stream()
                        .filter(cell -> cell.getRow() == tableNumberRow && cell.getValue().equals("1"))
                        .map(c -> c.getColumn())
                        .findFirst()
                        .orElse(-1);

            System.out.println(firstDayColumn);


            List<ImmutableCell> header = content.stream()
                    .filter(c -> c.getRow() == fioRow)
                    .collect(Collectors.toList());








            System.out.println(header);

        }

    }
}
