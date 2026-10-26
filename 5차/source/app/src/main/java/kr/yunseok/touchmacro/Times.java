package kr.yunseok.touchmacro;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Seconds at the interface boundary; existing saved gesture timings remain compatible. */
public final class Times {
    private Times() {}
    public static String seconds(int millis) {
        return BigDecimal.valueOf(millis,3).stripTrailingZeros().toPlainString();
    }
    public static String label(int millis) { return seconds(millis)+"초"; }
    public static int millis(String seconds) {
        try {
            BigDecimal value=new BigDecimal(seconds.trim());
            if(value.signum()<0)throw new IllegalArgumentException("시간은 0초 이상으로 입력하세요");
            return value.movePointRight(3).setScale(0,RoundingMode.UNNECESSARY).intValueExact();
        } catch(ArithmeticException|NumberFormatException e) {
            throw new IllegalArgumentException("초 단위 숫자를 입력하세요. 예: 1, 0.5, 1.5 (소수점 세 자리까지)");
        }
    }
}
