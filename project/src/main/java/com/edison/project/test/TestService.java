package com.edison.project.test;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
import org.springframework.stereotype.Service;

@Service
public class TestService {
    public String testData() {
        return "테스트 Success";
    }

    public void testError() {
        throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
    }
}