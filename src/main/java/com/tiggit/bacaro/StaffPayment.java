package com.tiggit.bacaro;

import java.util.ArrayList;
import java.util.List;

public class StaffPayment {
	
	private String firstName;
	
	private String lastName;
	
	private String hourRate;
	
	private List<DateEarning> earnings = new ArrayList<>();
	
	private Double netPay;
	
	private Double pensionEmployee;
	
	private Double pensionEmployer;
	
	private int columnAddress;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getHourRate() {
		return hourRate;
	}

	public void setHourRate(String hourRate) {
		this.hourRate = hourRate;
	}

	public List<DateEarning> getEarnings() {
		return earnings;
	}

	public void setEarnings(List<DateEarning> earnings) {
		this.earnings = earnings;
	}

	public Double getNetPay() {
		return netPay;
	}

	public void setNetPay(Double netPay) {
		this.netPay = netPay;
	}

	public Double getPensionEmployee() {
		return pensionEmployee;
	}

	public void setPensionEmployee(Double pensionEmployee) {
		this.pensionEmployee = pensionEmployee;
	}

	public Double getPensionEmployer() {
		return pensionEmployer;
	}

	public void setPensionEmployer(Double pensionEmployer) {
		this.pensionEmployer = pensionEmployer;
	}

	public int getColumnAddress() {
		return columnAddress;
	}

	public void setColumnAddress(int columnAddress) {
		this.columnAddress = columnAddress;
	}
	
	
	
	

}
