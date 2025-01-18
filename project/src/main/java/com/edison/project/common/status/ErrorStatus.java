package com.edison.project.common.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public enum ErrorStatus {
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    _NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "페이지를 찾을 수 없습니다."),

    // 입력값 검증 관련 에러
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALID401", "입력값이 올바르지 않습니다."),

    // 로그인 관련 에러
    LOGIN_CANCELLED(HttpStatus.BAD_REQUEST, "LOGIN4001", "로그인이 취소되었습니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "LOGIN4002", "로그인이 필요합니다."),

    // 멤버 관려 에러
    MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEMBER4001", "사용자가 없습니다."),
    NICKNAME_NOT_EXIST(HttpStatus.BAD_REQUEST, "MEMBER4002", "닉네임은 필수 입니다."),

    // 버블 관련 애러
    BUBBLE_NOT_FOUND(HttpStatus.BAD_REQUEST, "BUBBLE4001", "버블을 찾을 수 없습니다."),

    LABELS_TOO_MANY(HttpStatus.BAD_REQUEST, "BUBBLE_LABEL4001", "라벨 개수는 최대 3개까지 가능합니다."),

    // 라벨 관련 에러
    LABELS_NOT_FOUND(HttpStatus.BAD_REQUEST, "LABEL4001", "라벨을 찾을 수 없습니다."),
    LABEL_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "LABEL4002", "라벨 이름은 최대 20자까지 가능합니다."),
    INVALID_COLOR(HttpStatus.BAD_REQUEST, "LABEL4003", "유효하지 않은 라벨 색상입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public String getMessage(String message) {
        return Optional.ofNullable(message)
                .filter(Predicate.not(String::isBlank))
                .orElse(this.getMessage());
    }
}