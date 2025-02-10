package com.sellect.server.tmp;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.tmp.controller.response.TmpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/")
public class TmpController {

    @PostMapping("/success")
    public ApiResponse<TmpResponse> registerProduct(Long userId) {

        TmpResponse tmpResponse = new TmpResponse(1L);

        return ApiResponse.OK(tmpResponse);
    }

    @GetMapping("/error")
    public ApiResponse<TmpResponse> registerProduct2() {

        TmpResponse tmpResponse = new TmpResponse(1L);

        CommonException member = new CommonException(BError.NOT_REGISTERED, "Member");

        if (true) {
            throw member;
        }
        return ApiResponse.OK(tmpResponse);
    }
}
