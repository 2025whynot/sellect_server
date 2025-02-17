package com.sellect.server.payment.mapper;

import com.sellect.server.payment.controller.response.PaymentHistoryResponse;
import com.sellect.server.payment.domain.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(target = "id", expression = "java(payment.getId())")
    @Mapping(target = "orderId", expression = "java(payment.getOrderId())")
    @Mapping(target = "pid", expression = "java(payment.getPid())")
    @Mapping(target = "status", expression = "java(String.valueOf(payment.getStatus()))")
    @Mapping(target = "price", expression = "java(String.valueOf(payment.getPrice()))")
    @Mapping(target = "createdAt", expression = "java(payment.getCreatedAt().toString())")
    PaymentHistoryResponse toPaymentHistoryResponse(Payment payment);
}
