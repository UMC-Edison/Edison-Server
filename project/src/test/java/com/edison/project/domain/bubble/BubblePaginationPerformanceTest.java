package com.edison.project.domain.bubble;

import com.edison.project.domain.bubble.repository.BubbleRepository;
import java.sql.PreparedStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StopWatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Tag("ignore")
public class BubblePaginationPerformanceTest {

  @Autowired
  private BubbleRepository bubbleRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private Long testMemberId = 1L;

  @BeforeEach
  void setUp() {
    System.out.println("데이터 세팅 시작");

    jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

    String sql = "INSERT INTO bubble " +
        "(member_id, title, content, is_deleted, is_trashed, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";

    jdbcTemplate.batchUpdate(sql,
        new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
            ps.setLong(1, testMemberId);
            ps.setString(2, "테스트용 버블 제목 " + i);
            ps.setString(3, "테스트용 버블 내용 " + i);
            ps.setBoolean(4, false);
            ps.setBoolean(5, false);
          }
          @Override
          public int getBatchSize() {
            return 100000;
          }
        });

    jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

    System.out.println("10만 건 데이터 세팅");
  }

  @Test
  @DisplayName("오프셋 페이징 vs 커서 페이징 성능 비교")
  void comparePaginationPerformance() {
    int targetPage = 4900; // 4900번째 페이지 조회
    int size = 20;

    // 오프셋 페이징 측정
    StopWatch offsetStopWatch = new StopWatch();
    offsetStopWatch.start();
    PageRequest offsetRequest = PageRequest.of(targetPage, size, Sort.by(Sort.Direction.DESC, "bubbleId"));

    System.out.println("\n=======================================================");
    System.out.println("[오프셋 페이징 쿼리 시작] - 쿼리 2개(데이터+COUNT)");
    System.out.println("=======================================================");

    bubbleRepository.findByMember_MemberIdAndIsTrashedFalse(testMemberId, offsetRequest);
    offsetStopWatch.stop();

    // 커서 페이징 측정
    // 4900 페이지의 첫 번째 항목 커서 값 임의로 계산
    Long cursorId = 2000L;

    StopWatch cursorStopWatch = new StopWatch();
    cursorStopWatch.start();
    PageRequest cursorRequest = PageRequest.of(0, size);

    System.out.println("\n=======================================================");
    System.out.println("[커서 페이징 쿼리 시작] - 쿼리 1개");
    System.out.println("=======================================================");

    bubbleRepository.findByMember_MemberIdAndIsTrashedFalseAndBubbleIdLessThanOrderByBubbleIdDesc(
        testMemberId, cursorId, cursorRequest);
    cursorStopWatch.stop();

    System.out.println("=== 테스트 결과 ===");
    System.out.println("오프셋 페이징 소요 시간: " + offsetStopWatch.getTotalTimeMillis() + " ms");
    System.out.println("커서 페이징 소요 시간: " + cursorStopWatch.getTotalTimeMillis() + " ms");

  }
}
