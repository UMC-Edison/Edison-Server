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
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "LOGIN4003", "유효하지 않은 토큰입니다."),
    ACCESSTOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "LOGIN4004", "access토큰이 만료되었습니다. 재발급해 주세요."),
    REFRESHTOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "LOGIN4005", "토큰이 만료되었습니다. 다시 로그인해 주세요."),
    ACCESS_TOKEN_VALID(HttpStatus.BAD_REQUEST, "LOGIN4006", "Access Token이 아직 유효합니다."),

    // 멤버 관련 에러
    MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEMBER4001", "사용자가 없습니다."),
    NICKNAME_NOT_EXIST(HttpStatus.BAD_REQUEST, "MEMBER4002", "닉네임은 필수 입니다."),
    NICKNAME_NOT_CHANGED(HttpStatus.BAD_REQUEST, "MEMBER4003", "닉네임이 변경되지 않았습니다."),
    PROFILE_NOT_CHANGED(HttpStatus.BAD_REQUEST, "MEMBER4004", "프로필이 변경되지 않았습니다."),
    NICKNAME_ALREADY_SET(HttpStatus.BAD_REQUEST, "MEMBER4005", "닉네임은 최초 1회만 설정 가능합니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "MEMBER4006", "20자이내의 닉네임을 설정해주세요."),

    // 아이덴티티 테스트 관련 에러
    IDENTITY_ALREADY_SET(HttpStatus.BAD_REQUEST, "IDENTITY4001", "아이덴티티 키워드는 최초 1회만 설정 가능합니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "IDENTITY4002", "존재하지 않는 카테고리입니다."),
    INVALID_IDENTITY_MAPPING(HttpStatus.BAD_REQUEST, "IDENTITY4003", "카테고리와 키워드가 일치하지 않습니다."),
    NO_CHANGES_IN_KEYWORDS(HttpStatus.BAD_REQUEST, "IDENTITY4004", "선택된 키워드에 변경사항이 없습니다."),
    NOT_EXISTS_KEYWORD(HttpStatus.BAD_REQUEST, "IDENTITY4005", "존재하지 않는 키워드입니다."),

    // 버블 관련 애러
    BUBBLE_NOT_FOUND(HttpStatus.BAD_REQUEST, "BUBBLE4001", "버블을 찾을 수 없습니다."),
    LINKEDBUBBLE_NOT_FOUND(HttpStatus.BAD_REQUEST, "BUBBLE4002", "링크버블을 찾을 수 없습니다."),
    LABELS_TOO_MANY(HttpStatus.BAD_REQUEST, "BUBBLE_LABEL4001", "라벨 개수는 최대 3개까지 가능합니다."),

    // 라벨 관련 에러
    LABELS_NOT_FOUND(HttpStatus.BAD_REQUEST, "LABEL4001", "라벨을 찾을 수 없습니다."),
    LABEL_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "LABEL4002", "라벨 이름은 최대 20자까지 가능합니다."),
    INVALID_COLOR(HttpStatus.BAD_REQUEST, "LABEL4003", "유효하지 않은 라벨 색상값입니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "LABEL4004", "유효하지 않은 시간 형식입니다."),

    // 아트보드 관련 에러
    KEYWORD_IS_NOT_VALID(HttpStatus.BAD_REQUEST, "LETTER4001", "입력값이 존재하지 않습니다."),
    RESULT_NOT_FOUND(HttpStatus.BAD_REQUEST, "LETTER4002", "검색 결과가 존재하지 않습니다."),
    READTIME_VALIDATION(HttpStatus.BAD_REQUEST, "LETTER4003", "readTime field 관련 오류"),
    TITLE_VALIDATION(HttpStatus.BAD_REQUEST, "LETTER4004", "title field 관련 오류"),
    WRITER_VALIDATION(HttpStatus.BAD_REQUEST, "LETTER4005", "writer field 관련 오류"),
    CONTENT_VALIDATION(HttpStatus.BAD_REQUEST, "LETTER4006", "content field 관련 오류"),
    TAG_VALIDATION(HttpStatus.BAD_REQUEST, "LETTER4007", "tag field 관련 오류"),
    CATEGORY_VALIDATION(HttpStatus.BAD_REQUEST, "LETTER4008", "category field 관련 오류"),
    KEYWORD_IS_EMPTY(HttpStatus.BAD_REQUEST, "LETTER4009", "keyword field 관련 오류"),
    ARTLETTER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "LETTER4010", "아트레터 ID를 입력해 주세요."),
    LETTERS_NOT_FOUND(HttpStatus.BAD_REQUEST, "LETTER4011", "아트레터를 찾을 수 없습니다."),

    // 검색 관련 에러
    INVALID_KEYWORD(HttpStatus.BAD_REQUEST, "SEARCH4001", "검색어는 공백일 수 없습니다."),

    // 스페이스 관련 에러
    NO_BUBBLES_FOUND(HttpStatus.BAD_REQUEST,"SPACE4001", "작성된 버블이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public String getMessage(String message) {
        return Optional.ofNullable(message)
                .filter(Predicate.not(String::isBlank))
                .orElse(this.getMessage());
    }
}