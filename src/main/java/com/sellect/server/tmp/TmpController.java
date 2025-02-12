package com.sellect.server.tmp;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.tmp.controller.response.TmpRequest;
import com.sellect.server.tmp.controller.response.TmpResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/")
public class TmpController {

  @PostMapping("/success")
  public ApiResponse<TmpResponse> registerProduct(Long userId) {

    TmpResponse tmpResponse = new TmpResponse(1L);

    return ApiResponse.ok(tmpResponse);
  }

  @GetMapping("/error")
  public ApiResponse<TmpResponse> registerProduct2() {

    TmpResponse tmpResponse = new TmpResponse(1L);
    throw new CommonException(BError.NOT_VALID, "token");
  }

  @PostMapping("/field-error")
  public ApiResponse<TmpResponse> registerProduct2(Long userId,
      @Valid @RequestBody TmpRequest request) {

    TmpRequest tmpRequest = new TmpRequest(request.name(), request.name2(), request.name3());
    TmpResponse tmpResponse = new TmpResponse(1L);

    return ApiResponse.ok(tmpResponse);
  }

}
