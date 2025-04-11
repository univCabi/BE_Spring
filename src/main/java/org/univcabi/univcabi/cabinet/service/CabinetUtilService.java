package org.univcabi.univcabi.cabinet.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.univcabi.univcabi.cabinet.vo.CabinetPageResponseVo;
import org.univcabi.univcabi.cabinet.vo.CabinetPageVo;
import org.univcabi.univcabi.user.entity.User;

@Service
public class CabinetUtilService {

    public <T> CabinetPageResponseVo<T> convertToPageResponseVo(
            Page<T> page, CabinetPageVo requestVo, HttpServletRequest request) {

        int currentPage = requestVo.page() != null ? requestVo.page() : 0;
        int pageSize = requestVo.pageSize() != null ? requestVo.pageSize() : 12;

        String next = null;
        if (currentPage < page.getTotalPages() && !page.isEmpty()) {
            next = ServletUriComponentsBuilder.fromRequest(request)
                    .replaceQueryParam("page", currentPage + 1)
                    .replaceQueryParam("pageSize", pageSize)
                    .build()
                    .toUriString();
        }

        String previous = null;
        if (currentPage > 1) {
            previous = ServletUriComponentsBuilder.fromRequest(request)
                    .replaceQueryParam("page", currentPage - 1)
                    .replaceQueryParam("pageSize", pageSize)
                    .build()
                    .toUriString();
        }

        return new CabinetPageResponseVo<>(
                Math.toIntExact(page.getTotalElements()),
                next,
                previous,
                page.getContent()
        );
    }

    public boolean checkIsMine(String studentNumber, User cabinetOwner) {
        if (studentNumber != null && cabinetOwner != null) {
//            //TODO: 추가예정
//            // 학번으로 사용자 찾기
//            Optional<User> requestUser = userRepository.findByStudentNumber(requestVo.studentNumber());
//
//            if (requestUser.isPresent()) {
//                return requestUser.get().getId().equals(cabinetOwner.getId());
//            }
        }
        return false;
    }
}
