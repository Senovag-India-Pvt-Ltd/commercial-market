package com.sericulture.commercialmarket.model.enums;

import lombok.Getter;

@Getter
public enum PAYMENTMODE {

    CASH("cash"),
    ONLINE("online");

    private String label ;

    PAYMENTMODE(String label) {
        this.label = label;
    }
}