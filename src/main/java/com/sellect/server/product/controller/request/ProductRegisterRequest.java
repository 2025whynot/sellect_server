package com.sellect.server.product.controller.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRegisterRequest(

    @NotNull(message = "카테고리 ID는 필수 입력값입니다.")
    Long categoryId,

    @NotNull(message = "브랜드 ID는 필수 입력값입니다.")
    Long brandId,

    // todo : 가격은 BigDecimal로 막을 수 없음- -> 그래서 String타입으로 받음.
    @NotNull(message = "가격은 필수 입력값입니다.")
    @Positive(message = "가격은 1원 이상이어야 합니다.")
    @Pattern(regexp = "^[0-9]+$", message = "가격은 정수만 입력 가능합니다.") // 소수점 입력 방지
    String price, // BigDecimal 대신 String으로 받아서 정수 여부 검증

    @NotBlank(message = "상품명은 필수 입력값입니다.")
    @Size(min = 10, message = "상품명은 최소 10글자 이상이어야 합니다.")
    String name,

    @NotNull(message = "재고(stock)는 필수 입력값입니다.")
    @Min(value = 1, message = "재고(stock)는 1 이상이어야 합니다.")
    Integer stock

) {

    // 가격 변환 메서드
    @JsonIgnore
    public BigDecimal getPriceAsBigDecimal() {
        return new BigDecimal(price);
    }
}