package org.univcabi.univcabi.cabinet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetHistory;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.projection.CabinetStatusCountProjectionImpl;
import org.univcabi.univcabi.cabinet.repository.BuildingRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetHistoryRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.vo.*;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.ServiceException;
import org.univcabi.univcabi.user.entity.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCabinetService {
    private final BuildingRepository buildingRepository;
    private final CabinetRepository cabinetRepository;
    private final CabinetHistoryRepository cabinetHistoryRepository;
    private final AuthnRepository authnRepository;

    // 빌딩에 따라 사물함 상태에 따른 사물함 개수를 반환
    public List<CabinetStatusCountVo> getCabinetStatusCountsGroupByBuilding(){
       List<CabinetStatusCountProjectionImpl> projections =
               cabinetRepository.findCabinetStatusCountsGroupByBuilding();

        return projections.stream()
                .map(projection -> {
                    return new CabinetStatusCountVo(
                            projection.getBuildingName(),
                            projection.getTotalCount(),
                            projection.getUsingCount(),
                            projection.getOverdueCount(),
                            projection.getBrokenCount(),
                            projection.getAvailableCount()
                    );
                })
                .toList();
    }

    // 사물함 Id 값들을 보고 해당 상태값이 USING || OVERDUE 인경우 AVAILABLE로 변환하는 메서드
    public CabinetReturnResultVo returnCabinetsByCabinetIds(CabinetReturnCabinetIdsVo requestVo){
        List<Cabinet> cabinetList = cabinetRepository.findAllById(requestVo.cabinetIds());

        List<CabinetReturnDataVo> returnDataVoList = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for(Cabinet cabinet: cabinetList){
            if(cabinet.getStatus() == CabinetStatus.USING || cabinet.getStatus() == CabinetStatus.OVERDUE){
                CabinetHistory lateHistory = cabinetHistoryRepository.findLatestActiveHistoryByCabinetId(cabinet.getId());
                if(lateHistory!=null) {
                    lateHistory.setEndedAtNow();
                }
                cabinet.replaceStatusToAVAILVABLE();

                returnDataVoList.add(new CabinetReturnDataVo(
                        cabinet.getId(),
                        cabinet.getBuildingId().getName(),
                        cabinet.getCabinetNumber(),
                        cabinet.getStatus(),
                        cabinet.getUserId() != null? cabinet.getUserId().getName() : null
                ));
            }
            else{
                errors.add(String.format("사물함 ID %d: 반납 가능한 상태(USING 또는 OVERDUE)가 아닙니다",cabinet.getId()));
            }
        }
        String message = String.format("일부 사물함 반납 처리가 완료되었습니다. (처리된 개수 %d)",returnDataVoList.size());

        cabinetRepository.saveAll(cabinetList);

        return new CabinetReturnResultVo(returnDataVoList,message,errors);
    }

    // 요청들어온 사물함들의 상태 값을 변경하고 해당 사물함 정보를 반환
    // 추후 모듈화 필요해 보임
    public CabinetAdminChangeStatusResultVo changeCabinetStatusByCabinetIdsAndNewStatus(CabinetAdminChangeStatusVo requestVo)
    {
        CabinetStatus status = requestVo.newStatus();

        // USING, BROKEN, OVERDUE 상태 변경 시 사물함 한 개만 변경 가능
        if(status!=CabinetStatus.AVAILABLE && requestVo.cabinetIds().size()!=1){
            throw new ServiceException(ExceptionStatus.CABINET_STATUS_MULTI_UPDATE_FAILED);
        }

        // BROKEN 상태 변경 시 이유가 존재해야 함
        if(status==CabinetStatus.BROKEN && (requestVo.reason()==null||requestVo.reason().isBlank())){
            throw new ServiceException(ExceptionStatus.CABINET_STATUS_BROKEN_REASON_UPDATE_FAILED);
        }

        List<Cabinet> cabinetList = cabinetRepository.findAllById(requestVo.cabinetIds());

        List<CabinetStatusInfoVo> infoVoList = new ArrayList<>();

        User user = null;

        if(status == CabinetStatus.OVERDUE || status == CabinetStatus.USING)
        {
            // 인증 정보와 사용자는 반드시 둘다 존재해야하기에 USER 정보로 예외처리
            Authn authn = authnRepository.findByStudentNumber(requestVo.studentNumber()).orElseThrow(()-> new ServiceException(ExceptionStatus.USER_NOT_FOUND));
            user = authn.getUser();
        }
        for(Cabinet cabinet: cabinetList){

            // 분기 별로 사물함 상태 변경
            switch (status){
                // 사용가능한 상태로 변경 및 해당 history의 updatedAt, endedAt 정보 변경
                case AVAILABLE -> {
                    CabinetHistory lateHistory = cabinetHistoryRepository.findLatestActiveHistoryByCabinetId(cabinet.getId());
                    if(lateHistory!=null) {
                        lateHistory.setEndedAtNow();
                    }
                    cabinet.replaceStatusToAVAILVABLE();
                }
                // 새로운 히스토리 정보를 만듬
                case USING -> {
                    cabinet.setUser(user);
                    cabinet.replaceStatusToUSING();

                    CabinetHistory history = CabinetHistory.createRentHistory(user,cabinet,null);
                    cabinetHistoryRepository.save(history);
                }
                // 연체 상태로 변경 및 history의 updatedAt, expiredAt 정보 변경
                case OVERDUE -> {
                    cabinet.replaceStatusToOVERDUE();
                    CabinetHistory lateHistory = cabinetHistoryRepository.findLatestActiveHistoryByCabinetId(cabinet.getId());
                    if(lateHistory!=null) {
                        lateHistory.setExpiredAtNow();
                    }
                }
                // 망가진 상태로 변경 및 history의 updatedAt, expiredAt 정보 변경
                case BROKEN -> {
                    cabinet.replaceStatusToBROKEN(requestVo.reason());
                    CabinetHistory lateHistory = cabinetHistoryRepository.findLatestActiveHistoryByCabinetId(cabinet.getId());
                    if(lateHistory!=null) {
                        lateHistory.setExpiredAtNow();
                    }
                }
            }

            infoVoList.add(new CabinetStatusInfoVo(
                    cabinet.getId(),
                    cabinet.getBuildingId().getName(),
                    cabinet.getBuildingId().getFloor(),
                    cabinet.getCabinetNumber(),
                    status,
                    status== CabinetStatus.BROKEN? cabinet.getReason():null,
                    status== CabinetStatus.BROKEN? LocalDate.now():null,
                    user != null? user.getName() : null
            ));
        }

        return new CabinetAdminChangeStatusResultVo(infoVoList,
                "모든 사물함 상태 변경이 완료되었습니다. ( 처리된 개수:"+infoVoList.size()+"개 )");
    }

}
