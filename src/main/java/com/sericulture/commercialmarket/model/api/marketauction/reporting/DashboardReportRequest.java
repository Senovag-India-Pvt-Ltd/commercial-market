package com.sericulture.commercialmarket.model.api.marketauction.reporting;

import com.sericulture.commercialmarket.model.api.RequestBody;
import lombok.*;

import java.time.LocalDate;
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class DashboardReportRequest extends RequestBody {

    private LocalDate dashboardReportDate;
}
