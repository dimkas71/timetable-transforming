package ua.compservice;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.security.sasl.SaslException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.compservice.util.TimeSheetsAppUtils;

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

					if (workbook.isSheetHidden(workbook.getSheetIndex(activeSheet))) continue;
					
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

				String[][] sheetCells = withTeam ? s.withTeam() : s.cells;
				
				
				for (int r = 0; r < sheetCells.length; r++, nextRow++) {

					XSSFRow row = sheet.getRow(nextRow);
					if (row == null) {
						row = sheet.createRow(nextRow);
					}

					if (sheetCells[r] != null) { // TODO: exclude writing null to the container... can be null
						for (int c = 0; c < sheetCells[r].length; c++) {

							XSSFCell cell = row.getCell(c);
							if (cell == null) {
								cell = row.createCell(c, CellType.STRING);
							}

							cell.setCellValue(sheetCells[r][c]);
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
		
		private static final int NO_VALUE = -1;
		
		private String name;
		private String[][] cells;

		public Sheet(String aName, String[][] cells) {
			this.name = aName;
			this.cells = cells;
		}

		public String[][] withTeam() {
			
			int headerRow = headerRow(); //potentially throws NullPointerException
			
			if (headerRow == NO_VALUE) {
				String message = "Header row does'nt matched in the sheet " + name;
				logger.error(message);
				throw new SheetException(message);
			}
			
			
			int fdColumnIndex = firstDayColumnIndex(); //potentially throws NullPointerException
			
			if (fdColumnIndex == NO_VALUE) {
				String message = "First day column not found in the sheet " + name;
				logger.error(message);
				throw new SheetException(message);
			}
			
			int[] contentRows = getFirstLastContentRows(); //potentially throws NullPointerException
			
			int first = contentRows[0];
			int last = contentRows[1];
			
			if (first == NO_VALUE || last == NO_VALUE) {
				String message = "Personnell numbers aren't matched in the sheet " + name;
				logger.error(message);
				throw new SheetException(message);
			}
				
			String[][] newCells = new String[cells.length][];
			
			for (int r = 0; r < cells.length; r++) {
				if (cells[r] == null) continue;
				if (r == headerRow) {
					newCells[r] = new String[cells[r].length + 1];
					for (int c = 0; c < newCells[r].length; c++) {
						if (c < fdColumnIndex) {
							newCells[r][c] = cells[r][c];
						} else if (c == fdColumnIndex) {
							newCells[r][c] = name;
						} else {
							newCells[r][c] = cells[r][c - 1];
						}
					}
				} else if ( r >= first && r <= last) {
					newCells[r] = new String[cells[r].length + 1];
					for (int c = 0; c < newCells[r].length; c++) {
						if (c < fdColumnIndex) {
							newCells[r][c] = cells[r][c];
						} else if (c == fdColumnIndex) {
							newCells[r][c] = name;
						} else {
							newCells[r][c] = cells[r][c - 1];
						}
					}
				} else {
					newCells[r] = new String[cells[r].length];
					for (int c = 0; c < cells[r].length; c++) {
						newCells[r][c] = cells[r][c];
					}
				}
			}
			return newCells;
		}
		
		private int headerRow() {
			
			Objects.requireNonNull(cells);
			
			int headerRow = NO_VALUE;
			
			for (int r = 0; r < cells.length; r++) {
				
				if (cells[r] == null) continue;
				
				for (int c = 0; c < cells[r].length; c++) {
					
					if (cells[r][c] == null) continue;
					
					if (TimeSheetsAppUtils.matches(cells[r][c])) {
						headerRow = r - 1;
						return headerRow; 
					}
				}
			}
			return headerRow;
		}

		private int firstDayColumnIndex() {
			
			Objects.requireNonNull(cells);
			
			int firstDayColumnIndex = NO_VALUE;
			int headerRow = headerRow();
			
			if (headerRow != NO_VALUE) {
				for (int c = 0; c < cells[headerRow].length; c++) {
					if (cells[headerRow][c] == null) continue;
					if (TimeSheetsAppUtils.hasDigit(cells[headerRow][c])) {
						firstDayColumnIndex = c;
						break;
					}
				}
			}
			
			
			return firstDayColumnIndex;
		}
		
		private int personnelNumberColumnIndex() {
			
			Objects.requireNonNull(cells);
			
			int pnColumnIndex = NO_VALUE;
			
			for (int r = 0; r < cells.length; r++) {
				
				if (cells[r] == null) continue;
				
				for (int c = 0; c < cells[r].length; c++) {
					if (cells[r][c] == null) continue;
					if (TimeSheetsAppUtils.matches(cells[r][c])) {
						pnColumnIndex = c;
						return pnColumnIndex; 
					}
				}
			}
			return pnColumnIndex;
			
		}
		
		private int[] getFirstLastContentRows() {
			
			Objects.requireNonNull(cells);
			
			int first = NO_VALUE;
			int last = NO_VALUE;
			
			int pnColumnIndex = personnelNumberColumnIndex();
			boolean isFirst = false;
			if (pnColumnIndex != NO_VALUE) {
				for (int r = 0; r < cells.length; r++) {
					if (cells[r] == null) continue;
					
					if ((cells[r].length <= pnColumnIndex ) || (cells[r][pnColumnIndex] == null)) continue;
					
					boolean pnMatched = TimeSheetsAppUtils.matches(cells[r][pnColumnIndex]);
					
					if (!isFirst && pnMatched) {
						first = r;
						isFirst = true;
						
					} else if (pnMatched) {
						last = r;
					}
				}
			}
			return new int[] {first, last};
		}
	
	}

	static class SheetException extends RuntimeException {
		
		public SheetException() {
			super();
		}
		
		public SheetException(String message) {
			super(message);
		}
		
	}
	
}
