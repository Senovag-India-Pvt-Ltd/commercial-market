package com.sericulture.commercialmarket.model.api.marketauction;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class Weighment {

    private int crateNumber;
    float grossWeight;
    float netWeight;
}
