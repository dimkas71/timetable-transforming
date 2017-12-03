package ua.compservice.util;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.compservice.TimeSheetsException;
import ua.compservice.model.Cell;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TimeSheetsAppUtils {

    //Logging support
    private static final Logger logger = LoggerFactory.getLogger(TimeSheetsAppUtils.class);

    //region Constants
    static final int NO_VALUE = -1;

    static final String FIO_SEARCH_TEXT = "Фамилия И. О.";
    static final String PERSONNEL_NUMBER_SEARCH_TEXT = "Таб. №";
    static final String CURRENT_POSITION_SEARCH_TEXT = "Должность";
    static final String TEAM_SEARCH_TEXT = "бригада";

    static final String EMPTY_STRING = "";

    static final String TIME_REGEX_EXPRESSION
            = "(?<hours>\\d{1})\\/(?<workshift>\\d{1})\\((?<hoursminutes>\\d{1}:\\d{2})\\)";

    static final String DIGIT_FIND_REGEX_EXPRESSION = "\\d{1,}";
    //endregion

    //region Patterns
    static final Pattern DIGIT_FOUNDER_PATTERN = Pattern.compile(DIGIT_FIND_REGEX_EXPRESSION);
    static final Pattern TIME_FOUNDER_PATTERN = Pattern.compile(TIME_REGEX_EXPRESSION);
    static final String TIME_SHEET_WORD = "Табель";
    static final String NO_TEAM_TEXT = "NO TEAM";
    //endregion



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
        final double PRECISION = 0.5;

        int hoursForWorkShift = 0;
        int minutesForWorkShift = 0;

        Matcher m = TIME_FOUNDER_PATTERN.matcher(aString);

        while (m.find()) {

            try {
                String hm = m.group("hoursminutes");

                hoursForWorkShift = Integer.parseInt(hm.split(":")[0]);
                minutesForWorkShift = Integer.parseInt(hm.split(":")[1]);

            } catch (NumberFormatException e) {
                logger.error("For cell's value {} Integer.parseInt() throws NumberFormatException  {} ", aString, e.getMessage());

            } catch (IllegalStateException e) {
                logger.error("For cell's value {} Integer.parseInt() throws IllegalStateException {} ", aString, e.getMessage());
            }
        }

        float part = minutesForWorkShift / SECONDS_IN_MINUTE;

        return hoursForWorkShift + (part < PRECISION ? 0 : 1);

    }

    public static int toWorkShift(String from) {

        int workShift = 1;

        Matcher m = TIME_FOUNDER_PATTERN.matcher(from);

        while (m.find()) {

            try {
                workShift = Integer.parseInt(m.group("workshift"));
            } catch (NumberFormatException e) {
                logger.error("Working shift parsing error for cell's value {} Integer.parseInt() throws NumberFormatException  {} ", from, e.getMessage());
            } catch (IllegalStateException e) {
                logger.error("Working shift parsing error for cell's value {} Integer.parseInt() throws IllegalStateException  {} ", from, e.getMessage());
            }
        }

        return workShift;

    }

    public static List<Cell> extractHeader(List<Cell> allCells) {

        int headerRow = findHeader(allCells);

        return allCells.stream()
                .filter(c -> c.getRow() == headerRow)
                .collect(Collectors.toList());

    }

    public static List<Cell> extractLeftExceptOf(List<Cell> cells, List<Cell> header) {

        int headerRow = findHeader(header);

        int firstDayColumn = findFirstDayColumn(header, headerRow);

        return cells.stream()
                .filter(c -> (c.getRow() > headerRow) && (c.getColumn() < firstDayColumn))
                .collect(Collectors.toList());



    }

    public static void merge(Path to, boolean shouldInsertTeam, Path... sources) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            XSSFSheet sheet = workbook.createSheet("timesheet");

            List<Cell> cells = shouldInsertTeam ? withTeamFrom(sources): from(sources);
            writeTo(sheet, cells, 0);

            try(FileOutputStream fos = new FileOutputStream(to.toFile())) {
                workbook.write(fos);
            }

        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void createTimeSheet(Path to, Path source) {

        List<Cell> cells = from(source, 0);

        List<Cell> header = extractHeader(cells);

        List<Cell> left = extractLeftExceptOf(cells, header);


        //all others should be converted into time sheet cells

        List<Cell> others = cells.stream()
                .filter(c -> !header.contains(c) && !left.contains(c))
                .map(TimeSheetsAppUtils::newCell)
                .collect(Collectors.toList());

        List<Cell> all = Stream.of(header, left, others)
                .flatMap(l -> l.stream())
                .collect(Collectors.toList());

        save(to, all);

    }

    public static void checkDoubles(Path source) {

        List<Cell> cells = from(source, 0);

        int personnelColumn = cells.stream()
                .filter(c -> c.getValue().contains(PERSONNEL_NUMBER_SEARCH_TEXT))
                .map(c -> c.getColumn())
                .findFirst()
                .orElse(NO_VALUE);

        Map<String, List<Cell>> doubles = cells.stream()
                .filter(c -> (c.getColumn() == personnelColumn) && !c.getValue().isEmpty())
                .collect(Collectors.groupingBy(Cell::getValue));


        doubles.entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> {

                    final List<Integer> rows = e.getValue().stream()
                            .map(c -> c.getRow() + 1)
                            .collect(Collectors.toList());

                    final String personnalNumber = e.getKey();
                    logger.debug("For the PN {} there are rows with doubled values. List rows -> {}",
                            personnalNumber, rows);
                    System.out.println(String.format("For the PN %s, there are rows with doubles. List rows %s", personnalNumber, rows));
                });
    }

    public static void checkPersonnelNumber(Path source) {

        List<Cell> cells = TimeSheetsAppUtils.from(source, 0);

        int personnelColumn = cells.stream()
                .filter(c -> c.getValue().contains(PERSONNEL_NUMBER_SEARCH_TEXT))
                .map(c -> c.getColumn())
                .findFirst()
                .orElse(NO_VALUE);

        cells.stream()
                .filter(c -> (c.getColumn() == personnelColumn) && !c.getValue().isEmpty() && !c.getValue().contains(PERSONNEL_NUMBER_SEARCH_TEXT))
                .filter(c -> !matches(c.getValue()))
                .forEach(c -> {
                    int row = c.getRow() + 1;
                    final String personnelNumber = c.getValue();
                    logger.info("Row: {}, the personal number {} isn't correct", row, personnelNumber);
                    System.out.println(String.format("Row: %d, the personal number %s isn't correct", row, personnelNumber));
                });


    }

    public static void createNormHours(Path source, Path dest) {

        List<Cell> cells = from(source, 0);


        final int rowHeader = cells.stream()
                .filter(c -> c.getValue().contains(PERSONNEL_NUMBER_SEARCH_TEXT))
                .mapToInt(c -> c.getRow())
                .findFirst()
                .orElse(NO_VALUE);

        final int firstColumn = cells.stream()
                .filter(c -> c.getRow() == rowHeader && hasDigit(c.getValue()))
                .mapToInt(c -> c.getColumn())
                .sorted()
                .findFirst()
                .orElse(NO_VALUE);

        Map<Integer, Map<Integer, Integer>> collectedNormHours = cells.stream()
                .filter(c -> c.getRow() > rowHeader && c.getColumn() >= firstColumn && !c.getValue().isEmpty())
                .collect(Collectors.groupingBy(
                        Cell::getRow,
                        Collectors.groupingBy(
                                c -> toWorkShift(c.getValue()),
                                Collectors.mapping(
                                        c -> toWorkingHours(c.getValue()),
                                        Collectors.summingInt(i -> i)))));


        List<Cell> header = createNewHeader(cells);

        List<Cell> left = extractLeftExceptOf(cells, header);

        //TODO: rewrite the code below using stream api style...
        List<Cell> hours = new ArrayList<>();

        for (Map.Entry<Integer, Map<Integer, Integer>> outerEntry : collectedNormHours.entrySet()) {
            int row = outerEntry.getKey();

            for (Map.Entry<Integer, Integer> innerEntry : outerEntry.getValue().entrySet()) {
                int shift = innerEntry.getKey();
                int shiftHours = innerEntry.getValue();

                hours.add(
                        new Cell(row, firstColumn + shift - 1, String.valueOf(shiftHours))
                );
            }
        }

        save(dest, Stream.of(header, left, hours).flatMap(l -> l.stream()).collect(Collectors.toList()));


    }

    static List<Cell> from(Path source, final int rowShift) {

        if (!Files.exists(source)) {
            String message = String.format("File %s doesn't exist", source.toString());
            logger.error("{}", message);
            throw new TimeSheetsException(message);
        }


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
                                        currentRow.getRowNum() + rowShift,
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

    static List<Cell> from(Path... sources) {

        if (sources.length == 0) return new ArrayList<>();

        List<Cell> collected = new ArrayList<>();

        //1.collect all cells from variable sources to a list
        Arrays.stream(sources)
                .forEach(p -> {
                    //
                    int lastRow = collected.stream()
                        .map(Cell::getRow)
                        .sorted(Comparator.reverseOrder())
                        .findFirst()
                        .orElse(0);

                    collected.addAll(from(p, lastRow));
                });

        //2. extract the first header from the collected list
        List<Cell> packedCells = packedFrom(collected);


        return packedCells;
    }

    static int findHeader(List<Cell> cells) {
        return cells.stream()
                .filter(c -> c.getValue().contains(TimeSheetsAppUtils.PERSONNEL_NUMBER_SEARCH_TEXT))
                .map(Cell::getRow)
                .findFirst()
                .orElse(NO_VALUE);

    }




    /**
     *
     * @param collected
     * @return a packed list of cells, without doubled headers and empty rows...
     */
    private static List<Cell> packedFrom(List<Cell> collected) {

        List<Cell> header = extractHeader(collected);


        //skip rows contains header and TIME_SHEET_WORD
        List<Integer> rowsToSkip = collected.stream()
                .filter(c -> c.getValue().contains(PERSONNEL_NUMBER_SEARCH_TEXT) || c.getValue().contains(TIME_SHEET_WORD))
                .mapToInt(Cell::getRow)
                .collect(ArrayList<Integer>::new, ArrayList<Integer>::add, ArrayList<Integer>::addAll);


        int fioColumn = collected.stream()
                .filter(c -> c.getValue().contains(FIO_SEARCH_TEXT))
                .map(Cell::getColumn)
                .findFirst()
                .orElse(NO_VALUE);

        rowsToSkip.addAll(collected.stream()
                .filter(c -> c.getColumn() == fioColumn && c.getValue().contains(TEAM_SEARCH_TEXT))
                .map(Cell::getRow)
                .collect(Collectors.toList()));



        //3. all without headers
        List<Cell> withoutAnyHeaders = collected.stream()
                .filter(c -> rowsToSkip.indexOf(c.getRow()) == -1)
                .collect(Collectors.toList());

        //4. merge header and without header in the one big list of cells

        List<Cell> temporaryCollection = Stream.concat(header.stream(), withoutAnyHeaders.stream())
                .collect(Collectors.toList());

        //5. pack cells to another list without other headers and empty rows...

        Map<Integer, List<Cell>> groupedByRow = temporaryCollection.stream()
                .collect(Collectors.groupingBy(Cell::getRow));

        int newRowNumber = 0;

        Map<Integer, Integer> rowsMapper = new HashMap<>();

        for (Integer oldRowNumber : groupedByRow.keySet()) {
            rowsMapper.put(oldRowNumber, newRowNumber++);
        }

        return temporaryCollection.stream()
                .map(c -> new Cell(rowsMapper.get(c.getRow()), c.getColumn(), c.getValue()))
                .collect(Collectors.toList());
    }

    private static void writeTo(XSSFSheet currentSheet, List<Cell> otherCells, int shiftFrom) {

        Objects.requireNonNull(currentSheet);
        Objects.requireNonNull(otherCells);

        //int nextRow = currentSheet.getLastRowNum() + 1 - shiftFrom;
        int nextRow = currentSheet.getLastRowNum() - shiftFrom;

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

    private static void save(Path to, List<Cell> all) {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            final String timesheet = "timesheet";

            XSSFSheet sheet = workbook.createSheet(timesheet);

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

    private static Cell newCell(Cell c) {
        String value = c.getValue();
        if (value.isEmpty())
            return new Cell(c.getRow(), c.getColumn(), value);

        String[] values = value.split("\n");

        int hours = 0;

        for (String v : values) {
            hours += TimeSheetsAppUtils.toWorkingHours(v.trim());
        }

        return new Cell(c.getRow(), c.getColumn(), String.valueOf(hours));
    }

    private static List<Cell> createNewHeader(List<Cell> cells) {

        final String shiftOneString = "Нормо години 1 зміна";
        final String shiftTwoString = "Нормо години 2 зміна";
        final String shiftThreeString = "Нормо години 3 зміна";

        int headerRow = findHeader(cells);

        int firstDayColumn = findFirstDayColumn(cells, headerRow);

        List<Cell> withoutDates = cells.stream()
                .filter(c -> c.getRow() == headerRow && c.getColumn() < firstDayColumn)
                .collect(Collectors.toList());


        List<Cell> withShifts = Arrays.asList(
                new Cell(headerRow, firstDayColumn, shiftOneString),
                new Cell(headerRow, firstDayColumn + 1, shiftTwoString),
                new Cell(headerRow, firstDayColumn + 2, shiftThreeString)
        );

        //union header

        return Stream.of(withoutDates, withShifts)
                .flatMap(list -> list.stream())
                .collect(Collectors.toList());
    }

    private static int findFirstDayColumn(List<Cell> cells, int headerRow) {
        return cells.stream()
                .filter(c -> TimeSheetsAppUtils.hasDigit(c.getValue()) && c.getRow() == headerRow)
                .mapToInt(c -> c.getColumn())
                .sorted()
                .findFirst()
                .orElse(NO_VALUE);
    }

    private static int findPersonnelNumberColumn(List<Cell> cells) {
        return cells.stream()
                .filter(c -> c.getValue().contains(PERSONNEL_NUMBER_SEARCH_TEXT))
                .map(Cell::getColumn)
                .findFirst()
                .orElse(NO_VALUE);
    }

    private static List<Cell> withTeamFrom(Path... sources) {
        if (sources.length == 0) return new ArrayList<>();

        List<Cell> collected = new ArrayList<>();

        //1.collect all cells from variable sources to a list
        Arrays.stream(sources)
                .forEach(p -> {
                    //
                    int lastRow = collected.stream()
                            .map(Cell::getRow)
                            .sorted(Comparator.reverseOrder())
                            .findFirst()
                            .orElse(0);

                    collected.addAll(from(p, lastRow));
                });

        Map<String, Integer> teamMap = collected.stream()
                .filter(c -> c.getValue().contains(TEAM_SEARCH_TEXT))
                .collect(Collectors.toMap(Cell::getValue, Cell::getRow));


        int headerRow = findHeader(collected);
        int personnelNumberColumn = findPersonnelNumberColumn(collected);
        int firstDayColumn = findFirstDayColumn(collected, headerRow);

        Map<Integer, String> rowsToTeam = collected.stream()
                .filter(c -> c.getColumn() == personnelNumberColumn && !c.getValue().isEmpty() && c.getRow() > headerRow)
                .map(Cell::getRow)
                .collect(Collectors.toMap(Function.identity(), (Integer row) -> teamFromRow(row, teamMap)));


        //at the moment we prepared to add a team column to the collected list of cells....
        //1.
        Cell headerTeamCell = new Cell(headerRow, firstDayColumn, TEAM_SEARCH_TEXT);

        List<Cell> insertedCells = new ArrayList<>();

        rowsToTeam.entrySet()
                .stream()
                .forEach(e -> {
                    insertedCells.add(new Cell(e.getKey(), firstDayColumn, e.getValue()));
                });

        List<Cell> header = extractHeader(collected);

        //unpack header on the left and the right side, right side should be shifted to the one position right...
        //and pack them again in a new header...


        List<Cell> repackedHeader = Stream.of(
                    header.stream()
                        .filter(c -> c.getColumn() < firstDayColumn) //left
                        .collect(Collectors.toList()),
                    header.stream()
                        .filter(c -> c.getColumn() >= firstDayColumn) //right
                        .map(c -> new Cell(c.getRow(), c.getColumn() + 1, c.getValue()))
                        .collect(Collectors.toList())
                    ).flatMap(ll -> ll.stream())
                     .collect(Collectors.toList());




        List<Cell> leftSideCells = extractLeftExceptOf(collected, header);

        //for the right side and below header cells it needs to shift the column -> column + 1

        List<Cell> rightSideAndBelowCells = collected.stream()
                .filter(c -> !repackedHeader.contains(c) && !leftSideCells.contains(c) && !c.getValue().isEmpty())
                .map(c -> new Cell(c.getRow(), c.getColumn() + 1, c.getValue()))
                .collect(Collectors.toList());

        //and then collect header, headerTeamCell, insertedCells, leftSideCells and rigthSideAndBelowCells to
        //the one collection of cells

        List<Cell> recollected = Stream.of(
                            repackedHeader,
                            Arrays.asList(headerTeamCell),
                            leftSideCells,
                            insertedCells,
                            rightSideAndBelowCells
                    ).flatMap(ll -> ll.stream()).collect(Collectors.toList());


        //2. extract the first header from the collected list
        List<Cell> packedCells = packedFrom(recollected);


        return packedCells;
    }

    private static String teamFromRow(int row, Map<String, Integer> teamMap) {
        return teamMap.entrySet()
            .stream()
            .filter(e -> e.getValue() <= row)
            .sorted(Comparator.comparing(Map.Entry<String,Integer>::getValue).reversed())
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(NO_TEAM_TEXT);
    }

}


