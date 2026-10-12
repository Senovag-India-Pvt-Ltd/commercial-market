package com.sericulture.commercialmarket.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetHighestBidPerLotResponse {

    private int allottedLotId;

    private int highestBidAmount;
}
