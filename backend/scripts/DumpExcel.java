import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;

/** One-off dev utility — run manually with POI on classpath, not part of the Spring Boot app. */
public class DumpExcel {
  public static void main(String[] a) throws Exception {
    try (var in = new FileInputStream(a[0]); var wb = WorkbookFactory.create(in)) {
      var sheet = wb.getSheetAt(0);
      var fmt = new DataFormatter();
      for (int r = 0; r <= Math.min(15, sheet.getLastRowNum()); r++) {
        var row = sheet.getRow(r);
        if (row == null) continue;
        System.out.print("R" + (r + 1) + ":");
        for (int c = 0; c < 5; c++) {
          var cell = row.getCell(c);
          System.out.print(" [" + c + "]=" + (cell == null ? "" : fmt.formatCellValue(cell)));
        }
        System.out.println();
      }
      System.out.println("--- last shops ---");
      for (int r = Math.max(0, sheet.getLastRowNum() - 5); r <= sheet.getLastRowNum(); r++) {
        var row = sheet.getRow(r);
        if (row == null) continue;
        System.out.print("R" + (r + 1) + ":");
        for (int c = 0; c < 5; c++) {
          var cell = row.getCell(c);
          System.out.print(" [" + c + "]=" + (cell == null ? "" : fmt.formatCellValue(cell)));
        }
        System.out.println();
      }
    }
  }
}
