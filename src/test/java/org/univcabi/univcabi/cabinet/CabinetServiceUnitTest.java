package org.univcabi.univcabi.cabinet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.service.CabinetFallbackService;
import org.univcabi.univcabi.cabinet.service.CabinetRedisService;
import org.univcabi.univcabi.cabinet.service.CabinetService;
import org.univcabi.univcabi.cabinet.service.ReservationQueueManager;
import org.univcabi.univcabi.cabinet.vo.CabinetDetailVo;
import org.univcabi.univcabi.cabinet.vo.CabinetRentVo;
import org.univcabi.univcabi.cabinet.vo.CabinetReturnVo;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.ServiceException;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.univcabi.univcabi.cabinet.service.CabinetService.CABINET_OPERATION_RESULT_KEY;

@ExtendWith(MockitoExtension.class)
public class CabinetServiceUnitTest {

    @Mock
    private CabinetRepository cabinetRepository;

    @Mock
    private UserRepository userRepository;  // 추가

    @Mock
    private AuthnRepository authnRepository;  // 추가

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private CabinetRedisService cabinetRedisService;

    @Mock
    private ReservationQueueManager queueManager;

    @Mock
    private ThreadPoolTaskExecutor cabinetTaskExecutor; // 추가

    @Mock
    private CabinetFallbackService cabinetFallbackService; // 추가

    @InjectMocks
    private CabinetService cabinetService;

    private Cabinet testCabinet;
    private Building testBuilding;
    private User testUser;

    private static final String CABINET_RELEASE_TIME_KEY = "cabinet:release_time:";
    private static final String CABINET_RESERVATION_QUEUE_KEY = "cabinet:reservation_queue:";
    private static final String CABINET_AVAILABLE_KEY = "cabinet:available:";

    @BeforeEach
    void setUp() {
        // ValueOperations 및 ZSetOperations 설정 - lenient() 추가
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // 테스트 데이터 준비 - 빌더 패턴 적용
        testBuilding = Building.builder()
                .id(1L)
                .name(BuildingName.가온관)
                .floor(3)
                .section("A")
                .build();

        testUser = User.builder()
                .id(1L)
                .name("홍길동")
                .isVisible(true)
                .build();

        LocalDateTime now = LocalDateTime.now();
        testCabinet = Cabinet.builder()
                .id(1L)
                .cabinetNumber("A101")
                .status(CabinetStatus.AVAILABLE)
                .buildingId(testBuilding)
                .userId(null) // 초기에는 사용자가 없음
                .createdAt(now)
                .updatedAt(now)
                .build();

        // cabinetTaskExecutor mock 설정 (중요!)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // 추가: CompletableFuture 비동기 작업을 동기적으로 실행하도록 설정
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run(); // 비동기 작업을 즉시 실행
            return null;
        }).when(cabinetTaskExecutor).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("반납 후 다음날 13시 이전에는 대여가 불가능해야 함")
    void rentCabinet_BeforeReleaseTime_ShouldFail() throws Exception {
        // given
        Long cabinetId = 1L;
        String studentNumber = "20200101";
        CabinetRentVo rentVo = new CabinetRentVo(cabinetId, studentNumber);

        // isBeforeReleaseTime 메서드가 true를 반환하도록 설정
        when(cabinetRedisService.isBeforeReleaseTime(cabinetId)).thenReturn(true);

        // CompletableFuture.failedFuture 값을 설정
        ServiceException expectedException = new ServiceException(ExceptionStatus.CABINET_RENT_FAILED);

        // when
        CompletableFuture<CabinetDetailVo> future = cabinetService.rentCabinet(rentVo);

        // then
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            future.get(); // CompletableFuture에서 결과 가져오기 시도
        });

        // 실제 원인 예외 확인
        assertTrue(exception.getCause() instanceof ServiceException);
        ServiceException serviceException = (ServiceException) exception.getCause();
        assertEquals(ExceptionStatus.CABINET_RENT_FAILED, serviceException.getStatus());
    }

    @Test
    @DisplayName("Redis에 대여 가능 시간 정보가 없으면 대여가 가능해야 함")
    void rentCabinet_NoReleaseTime_ShouldSucceed() throws Exception {
        // given
        Long cabinetId = 1L;
        String studentNumber = "20200101";
        CabinetRentVo rentVo = new CabinetRentVo(cabinetId, studentNumber);

        // Redis 상태 모킹
        lenient().when(cabinetRedisService.canRentCabinet(cabinetId)).thenReturn(true);
        lenient().when(cabinetRedisService.isProcessingRent(cabinetId)).thenReturn(false);

        // 큐 및 락 모킹
        lenient().when(queueManager.addToQueueAndAcquireLock(cabinetId, studentNumber)).thenReturn(true);

        // DB 조회 모킹
        lenient().when(cabinetRepository.findById(cabinetId)).thenReturn(Optional.of(testCabinet));

        // 폴백 서비스 모킹 추가
        lenient().when(cabinetFallbackService.shouldUseFallback()).thenReturn(true);

        // 대여 처리 성공 모킹
        Cabinet updatedCabinet = Cabinet.builder()
                .id(testCabinet.getId())
                .cabinetNumber(testCabinet.getCabinetNumber())
                .status(CabinetStatus.USING)
                .buildingId(testBuilding)
                .userId(testUser)
                .createdAt(testCabinet.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cabinetFallbackService.processRentRequest(any())).thenReturn(Optional.of(updatedCabinet));

        // authnRepository 모킹
        Authn authn = Authn.builder()
                .user(testUser)
                .studentNumber(studentNumber)
                .build();
        lenient().when(authnRepository.findByStudentNumber(studentNumber)).thenReturn(Optional.of(authn));

        // 결과 설정 모킹
        lenient().doNothing().when(cabinetRedisService).setOperationResult(eq(cabinetId), eq(studentNumber), eq(true), isNull());

        // 리스너 설정 모킹 - 리스너 호출 시뮬레이션
        lenient().doAnswer(invocation -> {
            MessageListener listener = invocation.getArgument(1);

            // 결과 설정
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("success", true);

            when(redisTemplate.opsForValue().get(CABINET_OPERATION_RESULT_KEY + cabinetId + ":" + studentNumber))
                    .thenReturn(resultMap);

            // findById 두 번째 호출 모킹
            when(cabinetRepository.findById(cabinetId)).thenReturn(Optional.of(updatedCabinet));

            // 리스너 호출 시뮬레이션
            Message message = mock(Message.class);
            when(message.getBody()).thenReturn((cabinetId + ":" + studentNumber).getBytes(StandardCharsets.UTF_8));
            listener.onMessage(message, null);

            return null;
        }).when(cabinetRedisService).registerListener(eq(cabinetId + ":" + studentNumber), any(MessageListener.class));

        // when
        CompletableFuture<CabinetDetailVo> future = cabinetService.rentCabinet(rentVo);

        // then
        CabinetDetailVo result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(testBuilding.getFloor(), result.floor());
        assertEquals(testBuilding.getName().toString(), result.building().toString());
        assertEquals(updatedCabinet.getCabinetNumber(), result.cabinetNumber());
        assertTrue(result.isMine());
    }

    @Test
    @DisplayName("대여 가능 시간 이후에는 첫 요청자만 대여 성공해야 함")
    void rentCabinet_AfterReleaseTime_FirstRequesterOnly() throws Exception {
        // given
        Long cabinetId = 1L;
        String studentNumber = "20200101";
        CabinetRentVo rentVo = new CabinetRentVo(cabinetId, studentNumber);

        // Redis에서 캐비닛 상태 확인
        when(redisTemplate.opsForValue().get(CabinetService.CABINET_STATUS_KEY + cabinetId))
                .thenReturn(CabinetStatus.AVAILABLE.toString());

        // DB 확인 단계: 캐비닛이 존재하고 상태가 AVAILABLE인 경우
        Cabinet cabinet = mock(Cabinet.class);
        when(cabinet.getStatus()).thenReturn(CabinetStatus.AVAILABLE);
        Optional<Cabinet> cabinetOpt = Optional.of(cabinet);
        when(cabinetRepository.findById(cabinetId)).thenReturn(cabinetOpt);

        // 대여 가능 시간 체크는 통과하도록 설정
        when(cabinetRedisService.isBeforeReleaseTime(cabinetId)).thenReturn(false);
        when(cabinetRedisService.isProcessingRent(cabinetId)).thenReturn(false);

        // 중요: 대기열 체크에서 실패를 반환하도록 설정 - 이 단계에서 실패해야 함
        when(queueManager.addToQueueAndAcquireLock(cabinetId, studentNumber)).thenReturn(false);

        // when
        CompletableFuture<CabinetDetailVo> future = cabinetService.rentCabinet(rentVo);

        // then
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            future.get(5, TimeUnit.SECONDS);
        });

        // 실제 원인 예외 확인
        assertTrue(exception.getCause() instanceof ServiceException);
        ServiceException serviceException = (ServiceException) exception.getCause();
        assertEquals(ExceptionStatus.CABINET_RENT_FAILED, serviceException.getStatus());

        // 데이터베이스에서 대여 처리되지 않았는지 확인
        verify(cabinetRepository, never()).save(any(Cabinet.class));
    }

    @Test
    @DisplayName("캐비닛 반납 시 Redis에 정보가 업데이트 되어야 함")
    void returnCabinet_ShouldSetReleaseTime() {
        // given
        Long cabinetId = 1L;
        String studentNumber = "20200101";
        CabinetReturnVo returnVo = new CabinetReturnVo(cabinetId, studentNumber);

        // 반납 처리된 캐비닛 설정 (빌더 패턴 사용)
        Cabinet returnedCabinet = Cabinet.builder()
                .id(testCabinet.getId())
                .cabinetNumber(testCabinet.getCabinetNumber())
                .status(CabinetStatus.AVAILABLE)
                .buildingId(testBuilding)
                .userId(testUser)
                .createdAt(testCabinet.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        lenient().when(cabinetRepository.returnCabinetByCabinetId(cabinetId, studentNumber))
                .thenReturn(Optional.of(testCabinet));

        // Redis 서비스 모킹
        lenient().doNothing().when(cabinetRedisService).setTemporaryStatus(eq(cabinetId), eq(CabinetStatus.AVAILABLE));
        lenient().doNothing().when(cabinetRedisService).setReleaseTime(eq(cabinetId));
        lenient().doNothing().when(queueManager).releaseProcessingLock(cabinetId);
        lenient().doNothing().when(queueManager).releaseReturnLock(cabinetId);

        // Redis 템플릿 모킹
        // Redis get 호출시 null 반환하도록 설정 - 두 번째 setTemporaryStatus 호출을 트리거함
        lenient().when(redisTemplate.opsForValue().get(anyString())).thenReturn(null);
        lenient().doReturn(1L).when(redisTemplate).delete(anyList());

        // when
        CabinetDetailVo result = cabinetService.returnCabinet(returnVo);

        // then
        assertNotNull(result);
        assertFalse(result.isMine());

        // Redis 상태 업데이트 확인 - 두 번 호출되는 것을 기대함
        verify(cabinetRedisService, times(2)).setTemporaryStatus(eq(cabinetId), eq(CabinetStatus.AVAILABLE));
        verify(cabinetRedisService).setReleaseTime(cabinetId);
        verify(queueManager).releaseProcessingLock(cabinetId);
        verify(queueManager).releaseReturnLock(cabinetId);
        verify(redisTemplate).delete(anyList());
    }

    @Test
    @DisplayName("동시에 여러 사용자가 Redis에 대기열에 추가되는 경우 테스트")
    void rentCabinet_MultipleUsersInQueue_ShouldProcessCorrectly() throws Exception {
        // given
        Long cabinetId = 1L;
        String studentNumber = "20200101";
        CabinetRentVo rentVo = new CabinetRentVo(cabinetId, studentNumber);

        // Redis 상태 모킹
        lenient().when(cabinetRedisService.canRentCabinet(cabinetId)).thenReturn(true);
        lenient().when(cabinetRedisService.isProcessingRent(cabinetId)).thenReturn(false);

        // 큐 및 락 모킹
        lenient().when(queueManager.addToQueueAndAcquireLock(cabinetId, studentNumber)).thenReturn(true);

        // DB 조회 모킹
        lenient().when(cabinetRepository.findById(cabinetId)).thenReturn(Optional.of(testCabinet));

        // authnRepository 모킹
        Authn authn = Authn.builder()
                .user(testUser)
                .studentNumber(studentNumber)
                .build();
        lenient().when(authnRepository.findByStudentNumber(studentNumber)).thenReturn(Optional.of(authn));

        // 비동기 처리 성공 결과 시뮬레이션
        lenient().doAnswer(invocation -> {
            // 리스너 호출을 시뮬레이션
            MessageListener listener = invocation.getArgument(1);

            // 가짜 메시지 생성
            byte[] body = (cabinetId + ":" + studentNumber).getBytes();
            Message message = mock(Message.class);
            when(message.getBody()).thenReturn(body);

            // 성공 결과 생성
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("success", true);

            // 결과를 Redis에 설정 (모킹)
            when(redisTemplate.opsForValue().get(CABINET_OPERATION_RESULT_KEY + cabinetId + ":" + studentNumber))
                    .thenReturn(resultMap);

            // 리스너 호출
            listener.onMessage(message, null);

            return null;
        }).when(cabinetRedisService).registerListener(eq(cabinetId + ":" + studentNumber), any(MessageListener.class));

        // 대여 상태로 변경된 캐비닛
        Cabinet updatedCabinet = Cabinet.builder()
                .id(testCabinet.getId())
                .cabinetNumber(testCabinet.getCabinetNumber())
                .status(CabinetStatus.USING)
                .buildingId(testBuilding)
                .userId(testUser)
                .createdAt(testCabinet.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        // findById 두 번째 호출 모킹 (성공 케이스)
        lenient().when(cabinetRepository.findById(cabinetId))
                .thenReturn(Optional.of(testCabinet))  // 첫 번째 호출
                .thenReturn(Optional.of(updatedCabinet)); // 두 번째 호출

        // when
        CompletableFuture<CabinetDetailVo> future = cabinetService.rentCabinet(rentVo);

        // CompletableFuture에서 실제 결과 가져오기
        CabinetDetailVo result = future.get(5, TimeUnit.SECONDS);

        // then
        assertNotNull(result);
        assertTrue(result.isMine());

        // Redis 상태 설정 확인
        verify(cabinetRedisService).setTemporaryStatus(eq(cabinetId), eq(CabinetStatus.USING));
    }

    @Test
    @DisplayName("동시 대여 요청 처리 중 DB 저장 실패 시 예외 처리 테스트")
    void rentCabinet_DatabaseFailure_ShouldHandleException() throws Exception {
        // given
        Long cabinetId = 1L;
        String studentNumber = "20200101";
        CabinetRentVo rentVo = new CabinetRentVo(cabinetId, studentNumber);

        // 대여 가능 시간 체크 우회
        lenient().when(cabinetRedisService.isBeforeReleaseTime(cabinetId)).thenReturn(false);
        lenient().when(cabinetRedisService.canRentCabinet(cabinetId)).thenReturn(true);
        lenient().when(cabinetRedisService.isProcessingRent(cabinetId)).thenReturn(false);

        // 큐 처리 설정
        lenient().when(queueManager.addToQueueAndAcquireLock(cabinetId, studentNumber)).thenReturn(true);

        // 캐비닛 조회 설정
        lenient().when(cabinetRepository.findById(cabinetId)).thenReturn(Optional.of(testCabinet));

        // 폴백 서비스 설정
        lenient().when(cabinetFallbackService.shouldUseFallback()).thenReturn(true);

        // 데이터베이스 저장 실패 시뮬레이션
        RuntimeException dbException = new RuntimeException("캐비닛 저장 실패");
        lenient().when(cabinetFallbackService.processRentRequest(any())).thenThrow(dbException);

        // CompletableFuture.runAsync를 즉시 실행하도록 설정
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run(); // 비동기 작업을 동기적으로 실행
            return CompletableFuture.completedFuture(null);
        }).when(cabinetTaskExecutor).execute(any(Runnable.class));

        // 오류 결과 설정
        lenient().doAnswer(inv -> {
            // 에러 결과 설정 시뮬레이션
            return null;
        }).when(cabinetRedisService).setOperationResult(eq(cabinetId), eq(studentNumber), eq(false), anyString());

        // when
        CompletableFuture<CabinetDetailVo> future = cabinetService.rentCabinet(rentVo);

        // then
        // CompletableFuture에서 예외 확인
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            future.get(5, TimeUnit.SECONDS); // 타임아웃 설정
        });

        // 예외 원인 확인
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof ServiceException);

        // 실패 처리 확인
        verify(cabinetRedisService).setOperationResult(eq(cabinetId), eq(studentNumber), eq(false), anyString());
    }
}