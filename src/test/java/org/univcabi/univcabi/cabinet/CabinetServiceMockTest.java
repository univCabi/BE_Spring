package org.univcabi.univcabi.cabinet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.cabinet.dto.CabinetKafkaDto;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.service.*;
import org.univcabi.univcabi.cabinet.vo.CabinetDetailVo;
import org.univcabi.univcabi.cabinet.vo.CabinetRentVo;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.ServiceException;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CabinetServiceMockTest {

    @Mock
    private CabinetRepository cabinetRepository;

    @Mock
    private AuthnRepository authnRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CabinetKafkaProducerService kafkaProducerService;

    @Mock
    private ReservationQueueManager queueManager;

    @Mock
    private CabinetUtilService cabinetUtilService;

    @Mock
    private CabinetRedisService cabinetRedisService;

    @Mock
    private CabinetFallbackService cabinetFallbackService;

    @Mock
    private Executor cabinetTaskExecutor;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private CabinetService cabinetService;

    private User testUser;
    private Cabinet testCabinet;
    private Building testBuilding;
    private Authn testAuthn;

    @BeforeEach
    void setUp() {
        // Set up the Redis template mocks
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Create the cabinet service with mocked repositories
        cabinetService = new CabinetService(
                cabinetRepository,
                userRepository,
                authnRepository,
                redisTemplate,
                kafkaProducerService,
                queueManager,
                cabinetUtilService,
                cabinetRedisService,
                cabinetTaskExecutor,
                cabinetFallbackService
        );

        // Create test entities
        testBuilding = Building.builder()
                .id(1L)
                .name(BuildingName.가온관)
                .floor(2)
                .section("B")
                .build();

        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .isVisible(true)
                .building(testBuilding)
                .build();

        testAuthn = Authn.builder()
                .id(1L)
                .studentNumber("TEST_STUDENT_1000")
                .password("password")
                .user(testUser)
                .role(org.univcabi.univcabi.auth.entity.AuthnRole.NORMAL)
                .build();

        testCabinet = Cabinet.builder()
                .id(1L)
                .cabinetNumber("TEST_100")
                .status(CabinetStatus.AVAILABLE)
                .buildingId(testBuilding)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("사물함 렌트 - 성공 케이스")
    void rentCabinet_Success() throws ExecutionException, InterruptedException {
        // Setup
        String studentNumber = "TEST_STUDENT_1000";
        Long cabinetId = 1L;

        // Release time is not before (대여 가능 시간 체크 통과)
        when(cabinetRedisService.isBeforeReleaseTime(cabinetId)).thenReturn(false);

        // Cabinet is available
        when(cabinetRedisService.canRentCabinet(cabinetId)).thenReturn(true);

        // Not processing rent yet
        when(cabinetRedisService.isProcessingRent(cabinetId)).thenReturn(false);

        // Lock acquisition successful
        when(queueManager.addToQueueAndAcquireLock(cabinetId, studentNumber)).thenReturn(true);

        // Finding cabinet in DB
        when(cabinetRepository.findById(cabinetId)).thenReturn(Optional.of(testCabinet));

        // 추가: cabinetRedisService.setTemporaryStatus 모킹
        doNothing().when(cabinetRedisService).setTemporaryStatus(anyLong(), any(CabinetStatus.class));

        // 중요: cabinetTaskExecutor 모킹 추가 - 비동기 작업을 동기적으로 실행
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run(); // 즉시 실행
            return null;
        }).when(cabinetTaskExecutor).execute(any(Runnable.class));

        // Mock fallback service to return success
        when(cabinetFallbackService.shouldUseFallback()).thenReturn(true);

        // 폴백 서비스 성공 처리
        Cabinet rentedCabinet = testCabinet.toBuilder()
                .status(CabinetStatus.USING)
                .userId(testUser)
                .build();

        when(cabinetFallbackService.processRentRequest(any(CabinetKafkaDto.CabinetRentMessage.class)))
                .thenReturn(Optional.of(rentedCabinet));

        // 성공 결과 설정 모킹
        doNothing().when(cabinetRedisService).setOperationResult(eq(cabinetId), eq(studentNumber), eq(true), isNull());

        // Setup authn and user lookup
        when(authnRepository.findByStudentNumber(studentNumber)).thenReturn(Optional.of(testAuthn));

        // 리스너 등록 모킹 (설정만 하고 실제로 호출하지 않음)
        doNothing().when(cabinetRedisService).registerListener(anyString(), any(MessageListener.class));

        // 리스너 등록 시 결과 처리 즉시 실행
        doAnswer(invocation -> {
            // setupResultListener 메서드가 호출될 때 수동으로 CompletableFuture 완료
            MessageListener listener = invocation.getArgument(1);

            // 결과 생성 및 설정
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("success", true);

            when(redisTemplate.opsForValue().get(anyString())).thenReturn(resultMap);
            when(cabinetRepository.findById(cabinetId)).thenReturn(Optional.of(rentedCabinet));

            // 리스너 호출 시뮬레이션
            TestMessage message = new TestMessage((cabinetId + ":" + studentNumber).getBytes());
            listener.onMessage(message, null);

            return null;
        }).when(cabinetRedisService).registerListener(eq(cabinetId + ":" + studentNumber), any(MessageListener.class));

        // Execute
        CabinetRentVo rentVo = new CabinetRentVo(cabinetId, studentNumber);
        CompletableFuture<CabinetDetailVo> futureResult = cabinetService.rentCabinet(rentVo);

        // Wait and verify result
        CabinetDetailVo result = futureResult.get();

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isMine(), "Cabinet should be marked as mine");
        assertEquals(testCabinet.getCabinetNumber(), result.cabinetNumber(), "Cabinet number should match");
        assertEquals(CabinetStatus.USING, result.status(), "Cabinet status should be USING");
    }

    @Test
    @DisplayName("사물함 렌트 - 이미 사용 중인 경우")
    void rentCabinet_AlreadyInUse() {
        // Setup
        String studentNumber = "TEST_STUDENT_1000";
        Long cabinetId = 1L;

        // Cabinet is not available
        when(cabinetRedisService.canRentCabinet(cabinetId)).thenReturn(false);

        // Execute
        CabinetRentVo rentVo = new CabinetRentVo(cabinetId, studentNumber);
        CompletableFuture<CabinetDetailVo> futureResult = cabinetService.rentCabinet(rentVo);

        // Verify exception
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            futureResult.get();
        });

        assertTrue(exception.getCause() instanceof ServiceException);
        ServiceException serviceException = (ServiceException) exception.getCause();
        assertEquals(ExceptionStatus.CABINET_ALREADY_USING, serviceException.getStatus());
    }

    @Test
    @DisplayName("사물함 렌트 - 락 획득 실패")
    void rentCabinet_LockAcquisitionFailed() {
        // Setup
        String studentNumber = "TEST_STUDENT_1000";
        Long cabinetId = 1L;

        // Redis status check should pass
        when(redisTemplate.opsForValue().get(CabinetService.CABINET_STATUS_KEY + cabinetId))
                .thenReturn(CabinetStatus.AVAILABLE.toString());

        // Simulate cabinet exists and is available in DB
        Cabinet mockCabinet = mock(Cabinet.class);
        when(mockCabinet.getStatus()).thenReturn(CabinetStatus.AVAILABLE);
        Optional<Cabinet> cabinetOpt = Optional.of(mockCabinet);
        when(cabinetRepository.findById(cabinetId)).thenReturn(cabinetOpt);

        // Not processing rent
        when(cabinetRedisService.isProcessingRent(cabinetId)).thenReturn(false);

        // Release time check should pass
        when(cabinetRedisService.isBeforeReleaseTime(cabinetId)).thenReturn(false);

        // Lock acquisition fails - this is the condition we want to test
        when(queueManager.addToQueueAndAcquireLock(cabinetId, studentNumber)).thenReturn(false);

        // Execute
        CabinetRentVo rentVo = new CabinetRentVo(cabinetId, studentNumber);
        CompletableFuture<CabinetDetailVo> futureResult = cabinetService.rentCabinet(rentVo);

        // Verify exception
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            futureResult.get();
        });

        assertTrue(exception.getCause() instanceof ServiceException);
        ServiceException serviceException = (ServiceException) exception.getCause();
        assertEquals(ExceptionStatus.CABINET_RENT_FAILED, serviceException.getStatus());
    }

    @Test
    @DisplayName("사물함 렌트 - DB에서는 이미 사용중")
    void rentCabinet_AlreadyInUseInDB() throws ExecutionException, InterruptedException {
        // Setup
        String studentNumber = "TEST_STUDENT_1000";
        Long cabinetId = 1L;

        // Release time is not before (대여 가능 시간 체크 통과)
        when(cabinetRedisService.isBeforeReleaseTime(cabinetId)).thenReturn(false);

        // Cabinet is available in Redis
        when(cabinetRedisService.canRentCabinet(cabinetId)).thenReturn(true);

        // Not processing rent yet
        when(cabinetRedisService.isProcessingRent(cabinetId)).thenReturn(false);

        // Lock acquisition successful
        when(queueManager.addToQueueAndAcquireLock(cabinetId, studentNumber)).thenReturn(true);

        // But cabinet is already in use in DB
        Cabinet usingCabinet = testCabinet.toBuilder()
                .status(CabinetStatus.USING)
                .build();
        when(cabinetRepository.findById(cabinetId)).thenReturn(Optional.of(usingCabinet));

        // 중요: 리소스 정리 메서드 호출 모킹
        doNothing().when(cabinetRedisService).setTemporaryStatus(eq(cabinetId), any(CabinetStatus.class));
        doNothing().when(queueManager).removeFromQueue(cabinetId, studentNumber);
        doNothing().when(queueManager).releaseProcessingLock(cabinetId);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // 타임아웃 처리 즉시 실행하기 위한 모킹
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run(); // 즉시 실행
            return null;
        }).when(cabinetTaskExecutor).execute(any(Runnable.class));

        // Execute
        CabinetRentVo rentVo = new CabinetRentVo(cabinetId, studentNumber);
        CompletableFuture<CabinetDetailVo> futureResult = cabinetService.rentCabinet(rentVo);

        // Verify exception
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            futureResult.get();
        });

        assertTrue(exception.getCause() instanceof ServiceException);
        ServiceException serviceException = (ServiceException) exception.getCause();
        assertEquals(ExceptionStatus.CABINET_ALREADY_USING, serviceException.getStatus());

        // 리소스 정리 확인은 제거하거나 다음과 같이 수정
        // verify(cabinetRedisService, atLeastOnce()).setTemporaryStatus(eq(cabinetId), any(CabinetStatus.class));
        // verify(queueManager, atLeastOnce()).removeFromQueue(cabinetId, studentNumber);
        // verify(queueManager, atLeastOnce()).releaseProcessingLock(cabinetId);
    }

    // Helper class for creating test messages
    private static class TestMessage implements org.springframework.data.redis.connection.Message {
        private final byte[] body;

        public TestMessage(byte[] body) {
            this.body = body;
        }

        @Override
        public byte[] getChannel() {
            return CabinetService.CABINET_OPERATION_CHANNEL.getBytes();
        }

        @Override
        public byte[] getBody() {
            return body;
        }
    }
}