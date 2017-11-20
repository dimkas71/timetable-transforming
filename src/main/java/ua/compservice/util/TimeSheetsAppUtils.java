package ua.compservice.util;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.formula.functions.Rows;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.compservice.model.Cell;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TimeSheetsAppUtils {

    //Logging support
    private static final Logger logger = LoggerFactory.getLogger(TimeSheetsAppUtils.class);

    //region Constants
    public static final int NO_VALUE = -1;

    public static final String FIO_SEARCH_TEXT = "Фамилия И. О.";
    public static final String PERSONNEL_NUMBER_SEARCH_TEXT = "Таб. №";
    public static final String CURRENT_POSITION_SEARCH_TEXT = "Должность";

    public static final String EMPTY_STRING = "";

    public static final String TIME_REGEX_EXPRESSION
            = "(?<hours>\\d{1})\\/(?<workshift>\\d{1})\\((?<hoursminutes>\\d{1}:\\d{2})\\)";

    public static final String DIGIT_FIND_REGEX_EXPRESSION = "\\d{1,}";
    //endregion

    //region Patterns
    private static final Pattern DIGIT_FOUNDER_PATTERN = Pattern.compile(DIGIT_FIND_REGEX_EXPRESSION);
    private static final Pattern TIME_FOUNDER_PATTERN = Pattern.compile(TIME_REGEX_EXPRESSION);
    //endregion


    public static List<Cell> from(Path source) {

        List<Cell> content = new ArrayList<>();

        try {
            try(XSSFWorkbook workbook = new XSSFWorkbook(source.toFile())) {

                XSSFSheet activeSheet = workbook.getSheetAt(0);


                Iterator<Row> rowIterator = activeSheet.iterator();

                while (rowIterator.hasNext()) {
                    Row currentRow = rowIterator.next();

                    Iterator<org.apache.poi.ss.usermodel.Cell> cellIterator = currentRow.cellIterator();

                    while (cellIterator.hasNext()) {
                        org.apache.poi.ss.usermodel.Cell currentCell = cellIterator.next();

                        String aValue = "";

                        CellType cellType = currentCell.getCellTypeEnum();
                        if (cellType == CellType.NUMERIC) {
                            aValue = String.valueOf(new Integer((int) currentCell.getNumericCellValue()));
                        } else if (cellType == CellType.STRING) {
                            aValue = currentCell.getStringCellValue();
                        } else {
                            aValue = EMPTY_STRING;
                        }


                        content.add(
                                new Cell(
                                        currentRow.getRowNum(),
                                        currentCell.getColumnIndex(),
                                        aValue
                                )
                        );

                    }

                }

            }
        } catch (IOException e) {
            logger.error("{}", e);
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            logger.error("{}", e);
            e.printStackTrace();
        }

        return content;

    }

    public static void merge(Path to, Path... filesFrom) {


        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            XSSFSheet currentSheet = workbook.createSheet("timetable");

            int currentRow = 0;
            boolean isHeaderOutputed = false;

            /*
                1. first of all the header from the first path should be outputed
                2. then it ouputs all the rows from the rowHeader + 1
            */

            for (Path path : filesFrom) {
                List<Cell> content = from(path);

                int from = content.stream()
                        .filter(c -> c.getValue().contains(PERSONNEL_NUMBER_SEARCH_TEXT))
                        .map(c -> c.getRow())
                        .findFirst()
                        .orElse(NO_VALUE);

                if (from == NO_VALUE) {
                    logger.debug("In the file {} a header with text {} doesn't found", path.toFile().getAbsolutePath(), PERSONNEL_NUMBER_SEARCH_TEXT);
                    continue;
                } else {
                    //Main handler is at this point....
                    if (!isHeaderOutputed) {
                        List<Cell> headerCells = content.stream()
                                .filter(c -> c.getRow() == from)
                                .collect(Collectors.toList());

                        writeTo(currentSheet, headerCells, from + 1);

                        isHeaderOutputed = true;
                    }

                    List<Cell> otherCells = content.stream()
                            .filter(c -> c.getRow() > from)
                            .collect(Collectors.toList());

                    writeTo(currentSheet, otherCells, from + 1);

                }
            }

            try (FileOutputStream fos = new FileOutputStream(to.toFile())) {
                workbook.write(fos);
            }

            workbook.close();

        } catch (IOException e) {
            logger.error("Exception has been raised {}", e);
        }

    }

    public static boolean hasDigit(String aString) {
        if (aString == null || aString.isEmpty())
            return false;
        else
            return DIGIT_FOUNDER_PATTERN.matcher(aString).find();
    }

    public static boolean matches(String aPersonnelNumber) {

        final String regex = "\\d{2}\\/\\d{4}$";

        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(aPersonnelNumber);

        return matcher.find();


    }

    public static int toWorkingHours(String aString) {

        final float SECONDS_IN_MINUTE = 60.0f;
        final double PRESISION = 0.5;

        int hoursForWorkShift = 0;
        int minutesForWorkShift = 0;

        Matcher m = TIME_FOUNDER_PATTERN.matcher(aString);

        while (m.find()) {

            try {
                String hm = m.group("hoursminutes");

                hoursForWorkShift = Integer.parseInt(hm.split(":")[0]);
                minutesForWorkShift = Integer.parseInt(hm.split(":")[1]);

            } catch (NumberFormatException e) {
                //TODO:LOG this situation somehow
            } catch (IllegalStateException e) {
                //TODO:Log this situation somehow
            }


        }

        float part = minutesForWorkShift / SECONDS_IN_MINUTE;

        return hoursForWorkShift + (part < PRESISION ? 0 : 1);

    }

    public static int toWorkShift(String from) {

        int workShift = 1;

        Matcher m = TIME_FOUNDER_PATTERN.matcher(from);

        while (m.find()) {

            try {
                workShift = Integer.parseInt(m.group("workshift"));
            } catch (NumberFormatException e) {

            } catch (IllegalStateException e) {
            }
        }

        return workShift;

    }

    private static void writeTo(XSSFSheet currentSheet, List<Cell> otherCells, int shiftFrom) {

        Objects.requireNonNull(currentSheet);
        Objects.requireNonNull(otherCells);

        int nextRow = currentSheet.getLastRowNum() + 1 - shiftFrom;

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

    public static List<Cell> extractHeader(List<Cell> allCells) {

        //TODO: write test for this function with using Mockito framework

        int headerRow = allCells.stream()
                .filter(c -> c.getValue().contains(PERSONNEL_NUMBER_SEARCH_TEXT))
                .map(c -> c.getRow())
                .findFirst()
                .orElse(NO_VALUE);


        return allCells.stream()
                .filter(c -> c.getRow() == headerRow)
                .collect(Collectors.toList());

    }

    public static List<Cell> extractLeftExceptOf(List<Cell> cells, List<Cell> header) {

        int headerRow = header.stream()
                .mapToInt(c -> c.getRow())
                .findFirst()
                .orElse(NO_VALUE);

        int firstDayColumn = header.stream()
                .filter(c -> TimeSheetsAppUtils.hasDigit(c.getValue()))
                .mapToInt(c -> c.getColumn())
                .sorted()
                .findFirst()
                .orElse(-1);


        return cells.stream()
                .filter(c -> (c.getRow() > headerRow) && (c.getColumn() < firstDayColumn))
                .collect(Collectors.toList());



    }

    public static void save(Path to, List<Cell> all, String sheetName) {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            XSSFSheet sheet = workbook.createSheet(sheetName);

            all.stream()
                    .forEach(c -> {
                        XSSFRow row = sheet.getRow(c.getRow());

                        if (row == null) {
                            row = sheet.createRow(c.getRow());
                        }
                        XSSFCell cell = row.createCell(c.getColumn());
                        cell.setCellValue(c.getValue());
                    });

            FileOutputStream fos = new FileOutputStream(to.toFile());
            workbook.write(fos);
            fos.close();
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            e.printStackTrace();
        }
    }


}


