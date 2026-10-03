package org.univcabi.univcabi.support;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * N+1 여부를 SQL 파싱이 아닌 Hibernate Statistics로 직접 측정하기 위한 테스트 헬퍼.
 * count()는 실행된 JDBC PreparedStatement 개수를 반환하므로, 데이터 건수(N)를 늘려도
 * 값이 고정되어 있는지를 보고 N+1 여부를 판단할 수 있다.
 */
public final class HibernateQueryCounter {

    private final Statistics statistics;

    public HibernateQueryCounter(EntityManager entityManager) {
        this.statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    public void reset() {
        statistics.clear();
    }

    public long count() {
        return statistics.getPrepareStatementCount();
    }
}
