package com.sellect.server.order.Infrastructure.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Optional;

@Builder
public record KakaoPayApproveResponse(
    String aid,
    String tid,
    String cid,
    String partnerOrderId,
    String partnerUserId,
    String paymentMethodType,
    String itemName,
    int quantity,
    Amount amount,
    LocalDateTime createdAt,
    LocalDateTime approvedAt,
    Optional<CardInfo> cardInfo
) {

    public record Amount(
        int total,
        int taxFree,
        int vat,
        int point,
        int discount,
        int greenDeposit
    ) {

    }

    public record CardInfo(
        String interestFreeInstall,
        String bin,
        String cardType,
        String cardMid,
        String approvedId,
        String installMonth,
        String installmentType,
        String kakaopayPurchaseCorp,
        String kakaopayPurchaseCorpCode,
        String kakaopayIssuerCorp,
        String kakaopayIssuerCorpCode
    ) {

    }
}

