package com.sellect.server.product.controller.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record ProductModifyRequest(

    @Positive(message = "가격은 1원 이상이어야 합니다.")
    @Pattern(regexp = "^[0-9]+$", message = "가격은 정수만 입력 가능합니다.") // 소수점 입력 방지
    String price, // BigDecimal 대신 String으로 받아서 정수 여부 검증

    @Size(min = 10, message = "상품명은 최소 10글자 이상이어야 합니다.")
    String name,

    @Min(value = 1, message = "재고(stock)는 1 이상이어야 합니다.")
    Integer stock

) {

  @JsonIgnore
  public BigDecimal getPriceAsBigDecimal() {
    return price != null ? new BigDecimal(price) : null;
  }
}
