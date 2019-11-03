package com.tiggit.bacaro;

import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Row;

public class SheetMap {
	
	public SheetMap() {
		dateRows = new ArrayList<Row>();
	}
	
	private Row hourlyRateRow;
	
	private Row firstNameRow;
	
	private Row lastNameRow;
	
	private ArrayList<Row> dateRows;
	
	private Row pensionEmployeeRow;
	
	private Row persionEmployerRow;
	
	private Row netPayRow;

	public Row getHourlyRateRow() {
		return hourlyRateRow;
	}

	public void setHourlyRateRow(Row hourlyRateRow) {
		this.hourlyRateRow = hourlyRateRow;
	}

	public Row getFirstNameRow() {
		return firstNameRow;
	}

	public void setFirstNameRow(Row firstNameRow) {
		this.firstNameRow = firstNameRow;
	}

	public Row getLastNameRow() {
		return lastNameRow;
	}

	public void setLastNameRow(Row lastNameRow) {
		this.lastNameRow = lastNameRow;
	}

	public ArrayList<Row> getDateRows() {
		return dateRows;
	}

	public void setDateRows(ArrayList<Row> dateRows) {
		this.dateRows = dateRows;
	}

	public Row getPensionEmployeeRow() {
		return pensionEmployeeRow;
	}

	public void setPensionEmployeeRow(Row pensionEmployeeRow) {
		this.pensionEmployeeRow = pensionEmployeeRow;
	}

	public Row getPersionEmployerRow() {
		return persionEmployerRow;
	}

	public void setPersionEmployerRow(Row persionEmployerRow) {
		this.persionEmployerRow = persionEmployerRow;
	}

	public Row getNetPayRow() {
		return netPayRow;
	}

	public void setNetPayRow(Row netPayRow) {
		this.netPayRow = netPayRow;
	}
	
	

}
