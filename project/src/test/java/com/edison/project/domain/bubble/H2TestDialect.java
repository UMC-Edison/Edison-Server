package com.edison.project.domain.bubble;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;

// 테스트 환경의 H2 DB를 위한 전용 방언(Dialect) 설정
public class H2TestDialect extends H2Dialect {

  @Override
  public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    super.contributeTypes(typeContributions, serviceRegistry);

    // 핵심: H2가 모르는 VECTOR 타입이 들어오면, 무조건 "varbinary" (이진 데이터) 타입으로 속여서 테이블을 만들라고 지시합니다.
    typeContributions.getTypeConfiguration().getDdlTypeRegistry()
        .addDescriptor(new DdlTypeImpl(SqlTypes.VECTOR, "varbinary", this));
  }
}
