package com.sellect.server.tmp.controller.response;

import org.hibernate.validator.constraints.Length;;
public record TmpRequest(

        @Length(max = 10)
        String name,
        @Length(max = 10)
        String name2,
        @Length(max = 10)
        String name3


) {
}
