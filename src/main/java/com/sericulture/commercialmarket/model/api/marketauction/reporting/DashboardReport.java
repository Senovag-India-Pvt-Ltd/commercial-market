package com.sericulture.commercialmarket.model.api.marketauction.reporting;

import com.sericulture.commercialmarket.model.api.ResponseBody;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class DashboardReport extends ResponseBody {
    private String auctionStarted;
    private String acceptanceStarted;
    private String marketName;
    List<DashboardReportInfo> dashboardReportInfoList;
}
