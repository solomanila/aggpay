package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.dto.FundFlowRequest;
import com.letsvpn.admin.dto.FundFlowVO;
import com.letsvpn.admin.dto.MerchantOptionVO;
import com.letsvpn.admin.service.FundFlowService;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/fund-flow")
@RequiredArgsConstructor
public class FundFlowController {

    private final FundFlowService fundFlowService;

    /** 商户下拉选项 */
    @GetMapping("/merchants")
    public R<List<MerchantOptionVO>> merchants() {
        return R.success(fundFlowService.listMerchantOptions());
    }

    /** 资金流水分页查询 */
    @GetMapping("/list")
    public R<Page<FundFlowVO>> list(
            @RequestParam(required = false) String dateType,
            @RequestParam(required = false) List<Integer> platformIds,
            @RequestParam(required = false) List<Integer> areaTypes,
            @RequestParam(required = false) String refId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        FundFlowRequest req = new FundFlowRequest();
        req.setDateType(dateType);
        req.setPlatformIds(platformIds);
        req.setAreaTypes(areaTypes);
        req.setRefId(refId);
        req.setPageNum(pageNum);
        req.setPageSize(pageSize);
        return R.success(fundFlowService.queryPage(req));
    }
}
