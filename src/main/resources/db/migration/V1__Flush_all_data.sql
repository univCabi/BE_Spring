-- 외래 키 제약 조건 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 테이블 초기화 (역순으로)
TRUNCATE TABLE cabinets;
TRUNCATE TABLE cabinet_positions;
TRUNCATE TABLE cabinet_histories;
TRUNCATE TABLE authns;
TRUNCATE TABLE users;
TRUNCATE TABLE buildings;

-- 외래 키 제약 조건 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;