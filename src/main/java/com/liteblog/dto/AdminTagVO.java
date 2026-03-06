package com.liteblog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTagVO {
    private Long id;
    private String name;
    private Long articleCount;
}
