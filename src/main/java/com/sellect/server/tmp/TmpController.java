package com.sellect.server.tmp;

import com.sellect.server.common.exception.ApiResponse;
import com.sellect.server.common.exception.GlobalException;
import com.sellect.server.common.exception.ResponseStatus;
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

        return ApiResponse.OK(tmpResponse);
    }

    @GetMapping("/error")
    public ApiResponse<TmpResponse> registerProduct2() {

        TmpResponse tmpResponse = new TmpResponse(1L);

        GlobalException member = new GlobalException(ResponseStatus.TOKEN_NOT_VALID);

        if (true) {
            throw member;
        }
        // service 에서 사용 예시
//        Option option = optionRepository.findById(cartOptionModifyDto.getOptionId())
//                .orElseThrow(() -> new GlobalException(ResponseStatus.NO_EXIST_OPTION));
        return ApiResponse.OK(tmpResponse);
    }

    @PostMapping("/field-error")
    public ApiResponse<TmpResponse> registerProduct2(Long userId, @Valid @RequestBody TmpRequest request) {

        TmpRequest tmpRequest = new TmpRequest(request.name(), request.name2(), request.name3());
        TmpResponse tmpResponse = new TmpResponse(1L);

        return ApiResponse.OK(tmpResponse);
    }

}
