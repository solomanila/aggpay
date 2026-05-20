package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.service.BillService;
import com.letsvpn.common.core.dto.BillOrderDetailDTO;
import com.letsvpn.common.core.dto.BillPageDTO;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/admin/bill")
@RequiredArgsConstructor
@Slf4j
public class BillController {

    private final BillService billService;

    @GetMapping("/page")
    public R<Page<BillPageDTO>> page(
            @RequestParam(required = false) Long    id,
            @RequestParam(required = false) String  account,
            @RequestParam(required = false) String  channelTitle,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1")  long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return R.success(billService.pageQuery(id, account, channelTitle, status, pageNum, pageSize));
    }

    @GetMapping("/download")
    public void download(
            @RequestParam Long billId,
            HttpServletResponse response) throws IOException {
        List<BillOrderDetailDTO> details = billService.getOrderDetails(billId);

        String filename = URLEncoder.encode("bill_" + billId + ".xlsx", StandardCharsets.UTF_8.name());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("sheet1");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Order No");
            header.createCell(1).setCellValue("Merchant No");
            header.createCell(2).setCellValue("Amount");
            header.createCell(3).setCellValue("Status");
            header.createCell(4).setCellValue("CreatedTime");
            header.createCell(5).setCellValue("PayTime");

            int rowIdx = 1;
            for (BillOrderDetailDTO d : details) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(d.getOrderId()      != null ? d.getOrderId()      : "");
                row.createCell(1).setCellValue(d.getOtherOrderId() != null ? d.getOtherOrderId() : "");
                Cell amtCell = row.createCell(2);
                amtCell.setCellValue(d.getRealAmount() != null ? d.getRealAmount().doubleValue() : 0.0);
                row.createCell(3).setCellValue(Integer.valueOf(1).equals(d.getStatus()) ? "Success" : "Fail");
                row.createCell(4).setCellValue(fmtDate(d.getCreateTime()));
                row.createCell(5).setCellValue(fmtDate(d.getPayTime()));
            }

            wb.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("Failed to generate bill excel for billId={}", billId, e);
            response.reset();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":500,\"msg\":\"导出失败\"}");
        }
    }

    private String fmtDate(Date d) {
        if (d == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }
}
