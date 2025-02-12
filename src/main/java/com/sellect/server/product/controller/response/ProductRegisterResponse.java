package com.sellect.server.product.controller.response;

import com.sellect.server.product.domain.Product;
import java.util.List;

public record ProductRegisterResponse(
    List<ProductRegisterSuccessResponse> successProducts,
    List<ProductRegisterFailureResponse> failedProducts
) {

  public static ProductRegisterResponse from(
      List<Product> successProducts,
      List<ProductRegisterFailureResponse> failedProducts
  ) {
    return new ProductRegisterResponse(
        ProductRegisterSuccessResponse.fromList(successProducts), // 성공 상품 리스트 반환
        failedProducts
    );
  }
}