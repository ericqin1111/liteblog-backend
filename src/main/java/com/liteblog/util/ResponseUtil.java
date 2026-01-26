package com.liteblog.util;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ResponseUtil<T> {
    private Integer code;
    private String message;
    private T data;

    public ResponseUtil() {
    }

    public ResponseUtil(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseUtil<T> success() {
        return new ResponseUtil<>(200, "操作成功", null);
    }

    public static <T> ResponseUtil<T> success(T data) {
        return new ResponseUtil<>(200, "操作成功", data);
    }

    public static <T> ResponseUtil<T> success(String message, T data) {
        return new ResponseUtil<>(200, message, data);
    }

    public static <T> ResponseUtil<T> error(String message) {
        return new ResponseUtil<>(500, message, null);
    }

    public static <T> ResponseUtil<T> error(Integer code, String message) {
        return new ResponseUtil<>(code, message, null);
    }

    /**
     * 构建分页响应
     */
    public static <T> ResponseUtil<Map<String, Object>> page(long total, long current, long size, T records) {
        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("current", current);
        data.put("size", size);
        data.put("records", records);
        return new ResponseUtil<>(200, "查询成功", data);
    }
}
