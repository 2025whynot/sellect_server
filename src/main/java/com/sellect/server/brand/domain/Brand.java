package com.sellect.server.brand.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Brand {

  private Long id;
  private String name;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deleteAt;
}
