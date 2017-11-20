package ua.compservice.util;

import ua.compservice.model.Cell;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AllTimeTableVariationsGenerator {
    public static void main(String[] args) {

        Path currrentDir = Paths.get(System.getProperty("user.dir").toString()).resolve("src/files");

        try {
            Stream<Path> filesStream = Files.list(currrentDir);

            Set<String> strings = filesStream.map(p -> TimeSheetsAppUtils.from(p))
                    .flatMap(listCells -> onlyForTimeTable(listCells).stream())
                    .flatMap(cell -> Arrays.stream(cell.getValue().split("\n")))
                    .collect(Collectors.toSet());


            Map<String, Integer> values = strings.stream()
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toMap(s -> s, s -> TimeSheetsAppUtils.toWorkingHours(s)));


            Path testDir = Paths.get(System.getProperty("user.dir")).resolve("src/test/resources");


            if (Files.exists(testDir)) {
                Path testCSV = testDir.resolve("timesheet.csv");


                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testCSV.toFile())));


                for (Map.Entry<String, Integer> entry : values.entrySet()) {
                    writer.write("\"" + entry.getKey() + "\"" + "," + "\"" + String.valueOf(entry.getValue()) + "\"" + "\n");

                }


                writer.close();

            } else {
                System.out.println("test dir " + testDir.toString() + " doesn't exist");
            }





        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static List<Cell> onlyForTimeTable(List<Cell> cells) {

        int headerPosition = cells.stream()
                .filter(c -> c.getValue().contains(TimeSheetsAppUtils.PERSONNEL_NUMBER_SEARCH_TEXT))
                .map(c -> c.getRow())
                .findFirst()
                .orElse(-1);


        List<Cell> headerCells = cells.stream()
                .filter(c -> c.getRow() == headerPosition)
                .collect(Collectors.toList());


        List<Integer> markedDayColumns = headerCells.stream()
                .filter(c -> c.getValue().chars().allMatch(Character::isDigit))
                .map(c -> c.getColumn())
                .collect(Collectors.toList());


        return cells.stream()
                .filter(c -> (c.getRow() > headerPosition) && markedDayColumns.contains(c.getColumn()) && !c.getValue().isEmpty())
                .collect(Collectors.toList());
    }

}
