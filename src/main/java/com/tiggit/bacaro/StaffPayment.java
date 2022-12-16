package com.tiggit.bacaro;

import java.util.ArrayList;
import java.util.List;

public class StaffPayment {

    private String name;

    private Double hourRate;

    private Integer points;

    private Double shiftsWorked;

    private Double gratsPoints;

    private Double share;

    private Double netPay;

    private Double pensionEmployee;

    private Double pensionEmployer;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getHourRate() {
        return hourRate;
    }

    public void setHourRate(Double hourRate) {
        this.hourRate = hourRate;
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

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Double getShiftsWorked() {
        return shiftsWorked;
    }

    public void setShiftsWorked(Double shiftsWorked) {
        this.shiftsWorked = shiftsWorked;
    }

    public Double getGratsPoints() {
        return gratsPoints;
    }

    public void setGratsPoints(Double gratsPoints) {
        this.gratsPoints = gratsPoints;
    }

    public Double getShare() {
        return share;
    }

    public void setShare(Double share) {
        this.share = share;
    }

}
