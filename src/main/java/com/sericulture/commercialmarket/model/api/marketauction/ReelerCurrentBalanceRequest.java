package com.sericulture.commercialmarket.model.api.marketauction;

import com.sericulture.commercialmarket.model.api.RequestBody;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode

public class ReelerCurrentBalanceRequest extends RequestBody {
    private String reelerLicenceNumber;
    private String mobileNumber;
}
