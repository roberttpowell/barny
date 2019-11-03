package com.tiggit.bacaro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;

public class MapReader {
	
	public static final String[] reservedColumns = {"DATE","Kitchen", "Busers", "Servers", "Grats"};
	
	ArrayList<StaffPayment> paymentList = new ArrayList<>();
	
	public MapReader() {
		
	}
	
	public ArrayList<StaffPayment> process(SheetMap map, FormulaEvaluator evaluator) {
		
		processNames(map.getFirstNameRow(), map.getLastNameRow());
		processDatePayments(map.getDateRows(), evaluator);
		processPensions(map.getPensionEmployeeRow(), map.getPersionEmployerRow(), evaluator);
		processNetPay(map.getNetPayRow(), evaluator);
		
		return paymentList;
			
		
	}
	
	private void processNames(Row row, Row lnRow) {
		
		List<String> reservedList = Arrays.asList(reservedColumns);
		
		
		for (int i=row.getFirstCellNum(); i<row.getLastCellNum(); i++) {
			// cells should contain Strings.
			Cell nameCell = row.getCell(i);
			if (nameCell.getCellType() == CellType.STRING) {
				String name = nameCell.getStringCellValue();
				if (!reservedList.contains(name)) {
					
					StaffPayment payment = new StaffPayment();
					payment.setFirstName(name);
					payment.setLastName(lnRow.getCell(i).getStringCellValue());
					payment.setColumnAddress(i);
					
					paymentList.add(payment);
				}
			}
		}
		
	}
	
	private void processDatePayments(ArrayList<Row> rows, FormulaEvaluator evaluator) {
		
		for (StaffPayment payment : paymentList) {
			for (Row row : rows) {
				
				DateEarning earning = new DateEarning();
						
				Cell dateCell = row.getCell(0);
				if (dateCell.getCellType() == CellType.FORMULA) {
					// need to evaluate the formula and populate the date
					CellValue evaluatedCell = evaluator.evaluate(dateCell);
					Date workDate = DateUtil.getJavaDate(evaluatedCell.getNumberValue());
					earning.setEarningDate(workDate);
				}
				if (dateCell.getCellType() == CellType.NUMERIC) {
					// the cell is a hardcoded date
					Date workDate = DateUtil.getJavaDate(dateCell.getNumericCellValue());
					earning.setEarningDate(workDate);
				}
				Cell paymentCell = row.getCell(payment.getColumnAddress());
				if (paymentCell.getCellType() == CellType.FORMULA) {
					CellValue evalCell = evaluator.evaluate(paymentCell);
					earning.setEarningAmount(evalCell.getNumberValue());
				}
				if (paymentCell.getCellType() == CellType.NUMERIC) {
					earning.setEarningAmount(paymentCell.getNumericCellValue());
				}
				
				payment.getEarnings().add(earning);
			}
		}
		
	}
	
	private void processPensions(Row employeeRow, Row employerRow, FormulaEvaluator evaluator) {
		
		for (StaffPayment payment : paymentList) {
			
			Cell employeeCell = employeeRow.getCell(payment.getColumnAddress());
			if (employeeCell.getCellType() == CellType.FORMULA) {
				CellValue evalCell = evaluator.evaluate(employeeCell);
				payment.setPensionEmployee(evalCell.getNumberValue());
			}
			if (employeeCell.getCellType() == CellType.NUMERIC) {
				payment.setPensionEmployee(employeeCell.getNumericCellValue());
			}
			Cell employerCell = employerRow.getCell(payment.getColumnAddress());
			if (employerCell.getCellType() == CellType.FORMULA) {
				CellValue evalCell = evaluator.evaluate(employerCell);
				payment.setPensionEmployer(evalCell.getNumberValue());
			}
			if (employerCell.getCellType() == CellType.NUMERIC) {
				payment.setPensionEmployer(employerCell.getNumericCellValue());
			}
		}
		
		
	}
	
	private void processNetPay(Row payRow, FormulaEvaluator evaluator) {
		
		for (StaffPayment payment : paymentList) {
			Cell paymentCell = payRow.getCell(payment.getColumnAddress());
			if (paymentCell.getCellType() == CellType.FORMULA) {
				CellValue value = evaluator.evaluate(paymentCell);
				payment.setNetPay(value.getNumberValue());
			}
			
		}
		
	}

}
