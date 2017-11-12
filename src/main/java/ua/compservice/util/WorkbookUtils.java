package ua.compservice.util;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ua.compservice.model.ImmutableCell;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class WorkbookUtils {

    public static final String NO_VALUE = "UNDEFINED VALUE";

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
}
