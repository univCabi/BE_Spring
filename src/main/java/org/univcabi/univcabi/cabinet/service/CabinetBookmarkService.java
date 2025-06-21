package org.univcabi.univcabi.cabinet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetBookmark;
import org.univcabi.univcabi.cabinet.repository.CabinetBookmarkRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.vo.CabinetBookmarkVo;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.ServiceException;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;

import static org.univcabi.univcabi.exception.ExceptionStatus.BOOKMARK_ALREADY_EXIST;

@Service
@RequiredArgsConstructor
@Transactional
public class CabinetBookmarkService {
    private final CabinetRepository cabinetRepository;
    private final UserRepository userRepository;
    private final CabinetBookmarkRepository cabinetBookmarkRepository;

    // 북마크 추가
    public void addBookmarkByCabinetId(CabinetBookmarkVo requestVo){
        User user = userRepository.findUserByStudentNumber(requestVo.studentNumber())
                .orElseThrow(()-> new ServiceException(ExceptionStatus.USER_NOT_FOUND));

        Cabinet cabinet = cabinetRepository.findById(requestVo.cabinetId())
                .orElseThrow(()-> new ServiceException(ExceptionStatus.CABINET_NOT_FOUND));

        // 이미 북마크 상태인지 체크
        boolean exists = cabinetBookmarkRepository.existsByUserAndCabinetAndDeletedAtIsNull(user,cabinet);

        if(exists){
            throw new ServiceException(BOOKMARK_ALREADY_EXIST);
        }

        CabinetBookmark cabinetBookmark = CabinetBookmark.builder()
                .cabinet(cabinet)
                .user(user)
                .build();

        // 저장
        cabinetBookmarkRepository.save(cabinetBookmark);
    }

    // 북마크 삭제(soft)
    public void removeBookmarkByCabinetId(CabinetBookmarkVo requestVo){
        User user = userRepository.findUserByStudentNumber(requestVo.studentNumber())
                .orElseThrow(()-> new ServiceException(ExceptionStatus.USER_NOT_FOUND));

        Cabinet cabinet = cabinetRepository.findById(requestVo.cabinetId())
                .orElseThrow(()-> new ServiceException(ExceptionStatus.CABINET_NOT_FOUND));

        CabinetBookmark cabinetBookmark = cabinetBookmarkRepository.findByUserAndCabinetAndDeletedAtIsNull(user,cabinet)
                .orElseThrow(()->new ServiceException(ExceptionStatus.BOOKMARK_NOT_FOUND));

        cabinetBookmark.setDeletedAtForSoftDelete();
    }
}
