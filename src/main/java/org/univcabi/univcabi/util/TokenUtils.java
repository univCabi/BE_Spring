package org.univcabi.univcabi.util;


import jakarta.servlet.http.HttpServletRequest;

public class TokenUtils {
    // util 클래스이므로 인스턴스 호출 금지
    private TokenUtils(){}

    public static String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if(bearerToken!=null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        else{
            // 나중에 null 처리 로직 리팩토링 필요해 보임
            return null;
//          throw new IllegalArgumentException("유효하지 않은 accessToken");
        }
    }
}
