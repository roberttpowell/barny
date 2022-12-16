package com.tiggit.bacaro;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
            System.out.println("There are " + sheetCount + " sheets");

            for (int i = 0; i < sheetCount; i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                System.out.println("Sheet " + i + " title " + sheet.getSheetName());
                // maps.add(processSheet(sheet));

                // payments.addAll(mapReader.process(processSheet22(sheet), evaluator));
                payments.addAll(processSheet22(sheet, evaluator));
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("Payments has " + payments.size() + " entries");

        return payments;
    }

    private List<StaffPayment> processSheet22(XSSFSheet sheet, FormulaEvaluator evaluator) {

        List<StaffPayment> payments = new ArrayList<>();

        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Cell cell = row.getCell(0);

            // rows that have a text cell at zero are candidates
            // and that have a payout value
            if (cell != null && cell.getCellType() == CellType.STRING) {
                System.out.println("Processing "+cell.getStringCellValue());
                Cell paymentCell = row.getCell(8);
                if (paymentCell.getCellType() == CellType.FORMULA) {
                    // this is a candidate row
                    try {
                        StaffPayment payment = new StaffPayment();
                        payment.setName(row.getCell(0).getStringCellValue());
                        payment.setHourRate(row.getCell(1).getNumericCellValue());
                        payment.setPoints(new Double(row.getCell(2).getNumericCellValue()).intValue());
                        payment.setShiftsWorked(row.getCell(3).getNumericCellValue());
                        payment.setGratsPoints(evaluator.evaluate(row.getCell(4)).getNumberValue());
                        payment.setShare(evaluator.evaluate(row.getCell(5)).getNumberValue());
                        payment.setNetPay(evaluator.evaluate(row.getCell(8)).getNumberValue());
                        payment.setPensionEmployee(evaluator.evaluate(row.getCell(6)).getNumberValue());
                        payment.setPensionEmployer(evaluator.evaluate(row.getCell(7)).getNumberValue());

                        payments.add(payment);
                    } catch (NullPointerException np) {
                        // there could be a null pointer if the cells are empty, we should silently handle this.
                    }

                }
            }

        }

        return payments;

    }

    
}
