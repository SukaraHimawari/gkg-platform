package edu.gkg.algorithm;

import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.SharedRecords.Inflection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SentimentTrendTest {

    private static List<DateTone> series(double... tones) {
        LocalDate d = LocalDate.of(2024, 1, 1);
        List<DateTone> list = new ArrayList<>();
        for (int i = 0; i < tones.length; i++) {
            list.add(new DateTone(d.plusDays(i), tones[i], 10));
        }
        return list;
    }

    @Test
    void noInflectionWhenAllPositive() {
        List<DateTone> s = series(1, 2, 3, 2, 1, 2, 3, 2, 1, 2, 3, 2, 1, 2);
        assertTrue(SentimentTrend.detect(s, 3, 1.5).isEmpty());
    }

    @Test
    void detectsClearSignFlip() {
        // 前 7 天 +3，后 7 天 -3 —— 第 7 天前后符号反转，|6|≥1.5
        List<DateTone> s = series(3, 3, 3, 3, 3, 3, 3, -3, -3, -3, -3, -3, -3, -3);
        List<Inflection> infl = SentimentTrend.detect(s, 3, 1.5);
        assertFalse(infl.isEmpty(), "应至少检测出一个拐点");
        assertEquals("POS_TO_NEG", infl.get(0).direction());
        assertTrue(infl.get(0).magnitude() >= 1.5);
    }

    @Test
    void detectsNegToPos() {
        List<DateTone> s = series(-2, -2, -2, -2, -2, -2, -2, 2, 2, 2, 2, 2, 2, 2);
        List<Inflection> infl = SentimentTrend.detect(s, 3, 1.5);
        assertFalse(infl.isEmpty());
        assertEquals("NEG_TO_POS", infl.get(0).direction());
    }

    @Test
    void belowThresholdNotInflection() {
        // 符号变了但幅度 0.4 < 1.5，不算拐点
        List<DateTone> s = series(0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2,
                                 -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2);
        assertTrue(SentimentTrend.detect(s, 3, 1.5).isEmpty());
    }

    @Test
    void tooShortSeriesReturnsEmpty() {
        assertTrue(SentimentTrend.detect(series(1, -1, 1), 3, 1.5).isEmpty());
    }
}
