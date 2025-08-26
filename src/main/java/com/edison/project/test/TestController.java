package com.edison.project.test;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.test.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/data")
    public ResponseEntity<ApiResponse> success() {
        return ApiResponse.onSuccess(SuccessStatus._OK, "성공!");
    }

    @GetMapping("/error")
    public ResponseEntity<ApiResponse> error() {
        throw new GeneralException("일반적인 에러 발생", new RuntimeException("에러 발생"));
    }
}