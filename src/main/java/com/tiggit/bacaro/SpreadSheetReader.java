package com.tiggit.bacaro;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class SpreadSheetReader {
	
	
	private static final String hourlyRateMatch = "Rate CI$";
	
	private static final String firstNameMatch = "DATE";
	
	private static final String pensionEmployeeMatch = "Pension Employee";
	
	private static final String pensionEmployerMatch = "Pension Employer";
	
	private static final String netPayMatch = "NET PAY";
	
	
	public SpreadSheetReader() {
		
	}
	
	
	
	FormulaEvaluator evaluator;
	
	public List<StaffPayment> read(InputStream fis) {
		
		
		ArrayList<StaffPayment> payments = new ArrayList<>();
		
		try {
			
			byte[] buffer = IOUtils.toByteArray(fis);
			
			ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
			
			XSSFWorkbook workbook = new XSSFWorkbook(bis);
			
			evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			
			int sheetCount = workbook.getNumberOfSheets();
			System.out.println("There are "+sheetCount+" sheets");
			
			for (int i=0; i<sheetCount; i++) {
				XSSFSheet sheet = workbook.getSheetAt(i);
				System.out.println("Sheet "+i+" title "+sheet.getSheetName());
				// maps.add(processSheet(sheet));
				
				MapReader mapReader = new MapReader();
				payments.addAll(mapReader.process(processSheet(sheet), evaluator));
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Payments has "+payments.size()+" entries");
		
		return payments;
	}
	
	private SheetMap processSheet(XSSFSheet sheet) {
		// start by looking for rows that have a date at the start
		// and also look for magic rows
		
		SheetMap map = new SheetMap();
		
		Iterator<Row> rowIterator = sheet.rowIterator();
		while(rowIterator.hasNext()) {
			Row row = rowIterator.next();
			Cell cell = row.getCell(0);
			
			switch (cell.getCellType()) {
			case _NONE : {
				break;
			}
			case NUMERIC : {
				
				processNumericRow(row, map);
				break;
			}
			case STRING : {
				processStringRow(row, map);
				break;
			}
			case FORMULA : {
				processFormulaRow(row, map);
				break;
			}
			default: {
				
			}
			}
			
		}
		
		System.out.println("Sheet map created");
		
		return map;
	}
	
	
	// this is where we apply business specific logic to build the sheet map
	private void processFormulaRow(Row row, SheetMap map) {
		
		Cell cell = row.getCell(0);
		try {
			
			CellValue value = evaluator.evaluate(cell); // update the cell for later!
			switch (value.getCellType()) {
			case NUMERIC: {
				
				double d = value.getNumberValue();
				// if this is a recent date value, then attempt to convert it to a date and assume it is 
				// a date row
				if (d > 43000) {
					Date startDate = DateUtil.getJavaDate(d);	// not sure what this is for
					map.getDateRows().add(row);
				}
				// processNumericRow(row, map); // we have converted the formula to numeric yee hah!
				break;
			}
			default: {
				// don't do anything we don't want this row
			}
			}
		} catch (Exception e) {
			System.out.println("Exception procssing formula " + e);
		}
		
	}
	
	private void processNumericRow(Row row, SheetMap map) {
		
		Cell cell = row.getCell(0);
		double d = cell.getNumericCellValue();
		// if this is a recent date value, then attempt to convert it to a date and assume it is 
		// a date row
		if (d > 43000) {
			Date startDate = DateUtil.getJavaDate(d);	// not sure what this is for
			map.getDateRows().add(row);
		}
		
	}
	
	private void processStringRow(Row row, SheetMap map) {
		
		Cell cell = row.getCell(0);
		String value = cell.getStringCellValue();
		if (map.getFirstNameRow() != null && map.getLastNameRow() == null) {
			// this is the row after the firstname has been set
			map.setLastNameRow(row);
		}
		switch(value) {
		
		case hourlyRateMatch: {
			map.setHourlyRateRow(row);
			break;
		}
		case firstNameMatch: {
			map.setFirstNameRow(row);
			break;
		}
		case pensionEmployeeMatch: {
			map.setPensionEmployeeRow(row);
			break;
		}
		case pensionEmployerMatch: {
			map.setPersionEmployerRow(row);
			break;
		}
		case netPayMatch: {
			map.setNetPayRow(row);
			break;
		}
		default : {
			// stuff we don't want
		}
		
		}
		
	}

}
