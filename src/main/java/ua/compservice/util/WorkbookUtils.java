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
import java.util.stream.Collectors;

public final class WorkbookUtils {

    public static final String TABLE_NUMBER_SEARCH_TEXT = "Таб. №";

    public static final String NO_VALUE = "";

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


        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            XSSFSheet currentSheet = workbook.createSheet("timetable");

            int currentRow = 0;

            for (Path path : filesFrom) {
                List<ImmutableCell> content = from(path);

                int tableNumberColumn = content.stream()
                        .filter(c -> c.getValue().contains(TABLE_NUMBER_SEARCH_TEXT))
                        .map(c -> c.getRow())
                        .findFirst()
                        .orElse(-1);

                if (tableNumberColumn != -1) {

                    int maxRow = content.stream()
                            .map(c -> c.getRow())
                            .max((r1, r2) -> (r1 > r2) ? 1 : (r1 < r2) ? -1 : 0)
                            .orElse(-1);

                    final int from = content.stream()
                            .filter(c -> Utils.matches(c.getValue()) && (c.getColumn() == tableNumberColumn))
                            .map(c -> c.getRow())
                            .findFirst()
                            .orElse(-1);

                    final int currentRow1 = currentRow;
                    content.stream()
                            .filter(c -> c.getRow() >= from)
                            .forEach(c -> {

                                int rowNum = currentRow1 + c.getRow();

                                XSSFRow row = currentSheet.getRow(rowNum);
                                if (row == null) {
                                    row = currentSheet.createRow(rowNum);
                                }

                                XSSFCell cell = row.getCell(c.getColumn());
                                if (cell == null) {
                                    cell = row.createCell(c.getColumn(), CellType.STRING);
                                }

                                cell.setCellValue(c.getValue());
                            });

                    currentRow += maxRow + 1;

                }

            }

            try (FileOutputStream fos = new FileOutputStream(to.toFile())) {
                workbook.write(fos);
            }

            workbook.close();

        } catch (IOException e) {

        }






    }
}


