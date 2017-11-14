package ua.compservice.util;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ua.compservice.model.ImmutableCell;
import ua.compservice.model.ImmutableRow;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class WorkbookUtils {

    public static final String FIO_SEARCH_TEXT = "Фамилия И. О.";
    public static final String TABLE_NUMBER_SEARCH_TEXT = "Таб. №";
    public static final String CURRENT_POSITION_SEARCH_TEXT = "Должность";

    public static final String NO_VALUE = "";

    private static final int ROW_NUM_NO_VALUE = -1;

    public static List<ImmutableCell> from(Path source) {

        List<ImmutableCell> content = new ArrayList<>();

        try {
            try(XSSFWorkbook workbook = new XSSFWorkbook(source.toFile())) {

                XSSFSheet activeSheet = workbook.getSheetAt(0);


                Iterator<Row> rowIterator = activeSheet.iterator();

                while (rowIterator.hasNext()) {
                    Row currentRow = rowIterator.next();

                    Iterator<Cell> cellIterator = currentRow.cellIterator();

                    while (cellIterator.hasNext()) {
                        Cell currentCell = cellIterator.next();

                        String aValue = "";

                        CellType cellType = currentCell.getCellTypeEnum();
                        if (cellType == CellType.NUMERIC) {
                            aValue = String.valueOf(new Integer((int) currentCell.getNumericCellValue()));
                        } else if (cellType == CellType.STRING) {
                            aValue = currentCell.getStringCellValue();
                        } else {
                            aValue = NO_VALUE;
                        }


                        content.add(
                                new ImmutableCell(
                                        currentRow.getRowNum(),
                                        currentCell.getColumnIndex(),
                                        aValue
                                )
                        );

                    }

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }

        return content;

    }

    public static void merge(Path to, Path... filesFrom) {

        //TODO: fix problem at defining the

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            XSSFSheet currentSheet = workbook.createSheet("timetable");

            int currentRow = 0;
            boolean isHeaderOutputed = false;

            /*
                1. first of all the header from the first path should be outputed
                2. then it ouputs all the rows from the rowHeader + 1
            */

            for (Path path : filesFrom) {
                List<ImmutableCell> content = from(path);

                int from = content.stream()
                        .filter(c -> c.getValue().contains(TABLE_NUMBER_SEARCH_TEXT))
                        .map(c -> c.getRow())
                        .findFirst()
                        .orElse(ROW_NUM_NO_VALUE);

                if (from == ROW_NUM_NO_VALUE) {
                    //TODO: log this case
                    continue;
                } else {
                    //Main handler is at this point....
                    if (!isHeaderOutputed) {
                        List<ImmutableCell> headerCells = content.stream()
                                .filter(c -> c.getRow() == from)
                                .collect(Collectors.toList());

                        writeTo(currentSheet, headerCells);

                        isHeaderOutputed = true;
                    }

                    List<ImmutableCell> otherCells = content.stream()
                            .filter(c -> c.getRow() > from)
                            .collect(Collectors.toList());

                    writeTo(currentSheet, otherCells);

                }
            }

            try (FileOutputStream fos = new FileOutputStream(to.toFile())) {
                workbook.write(fos);
            }

            workbook.close();

        } catch (IOException e) {

        }

    }

    private static void writeTo(XSSFSheet currentSheet, List<ImmutableCell> otherCells) {

        Objects.requireNonNull(currentSheet);
        Objects.requireNonNull(otherCells);

        int nextRow = currentSheet.getLastRowNum() + 1;

        otherCells.stream()
                .forEach(currentCell -> {

                    int rowNum = nextRow + currentCell.getRow();

                    XSSFRow row = currentSheet.getRow(rowNum);
                    if (row == null) {
                        row = currentSheet.createRow(rowNum);
                    }

                    XSSFCell cell = row.getCell(currentCell.getColumn());
                    if (cell == null) {
                        cell = row.createCell(currentCell.getColumn(), CellType.STRING);
                    }

                    cell.setCellValue(currentCell.getValue());
                });
    }
}


