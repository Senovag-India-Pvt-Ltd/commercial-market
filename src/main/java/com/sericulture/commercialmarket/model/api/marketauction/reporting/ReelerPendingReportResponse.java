package com.sericulture.commercialmarket.model.api.marketauction.reporting;

import com.sericulture.commercialmarket.model.api.ResponseBody;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerPendingReportResponse extends ResponseBody {
    private String balance;
    private String creditTotal;
    private String debitTotal;
    private String marketName;
    List<ReelerPendingInfo> reelerPendingInfoList;
}
