package org.univcabi.univcabi.util;


import jakarta.servlet.http.HttpServletRequest;
import org.univcabi.univcabi.exception.UtilException;

import static org.univcabi.univcabi.exception.ExceptionStatus.AUTH_COOKIE_UNAUTHORIZED;

public class TokenUtils {
    // util 클래스이므로 인스턴스 호출 금지
    private TokenUtils(){}

    public static String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if(bearerToken!=null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        else{
            // 잘못된 토큰 요청시 예외 처리
          throw new UtilException(AUTH_COOKIE_UNAUTHORIZED);
        }
    }
}
