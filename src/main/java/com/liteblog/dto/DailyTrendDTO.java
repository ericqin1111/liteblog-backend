package com.liteblog.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyTrendDTO {

    private LocalDate date;

    private Long pv;
}
