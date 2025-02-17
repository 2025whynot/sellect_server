package com.sellect.server.payment.domain.controller.response;

import lombok.Builder;


@Builder
public record KakaoPayReadyResponse(
    String tid,
    Boolean tms_result,
    String created_at,
    String next_redirect_pc_url,
    String next_redirect_mobile_url,
    String next_redirect_app_url,
    String android_app_scheme,
    String ios_app_scheme
) {

}