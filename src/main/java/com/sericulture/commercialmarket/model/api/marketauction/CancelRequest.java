package com.sericulture.commercialmarket.model.api.marketauction;

import com.sericulture.commercialmarket.model.api.RequestBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CancelRequest extends RequestBody {

    @Schema(name = "cancellationReason", example = "do not want to participate", required = true)
    private int cancellationReason;

    private LocalDate auctionDate;
}
