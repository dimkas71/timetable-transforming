package ua.compservice;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SheetContainer {

	private final static Logger logger = LoggerFactory.getLogger(SheetContainer.class);
	private final static String EMPTY_STRING = "";

	private String name;
	private List<Sheet> sheets;

	private SheetContainer() {

	}

	public static SheetContainer createFrom(Path p) {

		SheetContainer container = new SheetContainer();

		if (!Files.exists(p)) {
			String message = String.format("File %s doesn't exist", p.toString());
			logger.error("{}", message);
			throw new TimeSheetsException(message);
		}

		List<Sheet> sheets = new ArrayList<>();

		try {
			try (XSSFWorkbook workbook = new XSSFWorkbook(p.toFile())) {

				Iterator<org.apache.poi.ss.usermodel.Sheet> sheetIterator = workbook.sheetIterator();

				while (sheetIterator.hasNext()) {

					XSSFSheet activeSheet = (XSSFSheet) sheetIterator.next();

					String sheetName = activeSheet.getSheetName();

					String[][] sheetCells = new String[activeSheet.getLastRowNum() + 1][];
					Iterator<Row> rowIterator = activeSheet.iterator();

					while (rowIterator.hasNext()) {
						Row currentRow = rowIterator.next();

						logger.debug("Sheet: {};Row num:{}", sheetName, currentRow.getRowNum());

						sheetCells[currentRow.getRowNum()] = new String[currentRow.getLastCellNum() + 1];

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
							sheetCells[currentRow.getRowNum()][currentCell.getColumnIndex()] = aValue;
						} // cell iterator

					} // row iterator

					sheets.add(new Sheet(sheetName, sheetCells));

				} // sheet iterator

			}
		} catch (IOException e) {
			logger.error("{}", e);
			e.printStackTrace();
		} catch (InvalidFormatException e) {
			logger.error("{}", e);
			e.printStackTrace();
		}

		container.name = p.toString();
		container.sheets = sheets;

		return container;
	}

	public void write(Path to, boolean withTeam) {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {

			XSSFSheet sheet = workbook.createSheet("merged-sheets");

			int nextRow = 0;
			for (Sheet s : sheets) {

				for (int r = 0; r < s.cells.length; r++, nextRow++) {

					XSSFRow row = sheet.getRow(nextRow);
					if (row == null) {
						row = sheet.createRow(nextRow);
					}

					if (s.cells[r] != null) { // TODO: exclude writing null to the container... can be null
						for (int c = 0; c < s.cells[r].length; c++) {

							XSSFCell cell = row.getCell(c);
							if (cell == null) {
								cell = row.createCell(c, CellType.STRING);
							}

							cell.setCellValue(s.cells[r][c]);
						}
					}
				}

			}

			try (FileOutputStream fos = new FileOutputStream(to.toFile())) {
				workbook.write(fos);
			}

		} catch (IOException e) {
			logger.error("{}", e.getMessage());
			e.printStackTrace();
		}
	}

	static class Sheet {

		private String name;
		private String[][] cells;

		public Sheet(String aName, String[][] cells) {
			this.name = aName;
			this.cells = cells;
		}

	}

}
