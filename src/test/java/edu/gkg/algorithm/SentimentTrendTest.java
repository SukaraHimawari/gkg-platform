package edu.gkg.algorithm;

import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.SharedRecords.Inflection;
import edu.gkg.common.SharedRecords.SentimentSeries;
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

    @Test
    void nullSeriesReturnsEmpty() {
        assertTrue(SentimentTrend.detect(null, 3, 1.5).isEmpty());
    }

    @Test
    void constantSeriesNoInflection() {
        List<DateTone> s = series(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        assertTrue(SentimentTrend.detect(s, 3, 1.5).isEmpty());
    }

    @Test
    void monotonicallyIncreasingNoInflection() {
        // 即使符号 0 → +，因为左均值为 0 会被跳过；不算拐点
        List<DateTone> s = series(0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7);
        assertTrue(SentimentTrend.detect(s, 3, 1.5).isEmpty(),
                "左窗口均值=0，按规则忽略");
    }

    @Test
    void multipleFlipsAllDetected() {
        // 三段：+3 *5  /  -3 *5  /  +3 *5  应当检出至少两个拐点
        List<DateTone> s = series(
                3, 3, 3, 3, 3,
                -3, -3, -3, -3, -3,
                3, 3, 3, 3, 3);
        List<Inflection> infl = SentimentTrend.detect(s, 3, 1.5);
        assertTrue(infl.size() >= 2, "应至少检出两个拐点，实际：" + infl.size());
        assertEquals("POS_TO_NEG", infl.get(0).direction());
        assertEquals("NEG_TO_POS", infl.get(infl.size() - 1).direction());
    }

    @Test
    void inflectionDateIsCenterPoint() {
        List<DateTone> s = series(2, 2, 2, 2, 2, 2, 2, -2, -2, -2, -2, -2, -2, -2);
        List<Inflection> infl = SentimentTrend.detect(s, 3, 1.5);
        assertFalse(infl.isEmpty());
        // 第一个拐点的位置应当落在符号反转那一带（索引 6/7 附近）
        LocalDate first = infl.get(0).date();
        LocalDate base = LocalDate.of(2024, 1, 1);
        long days = java.time.temporal.ChronoUnit.DAYS.between(base, first);
        assertTrue(days >= 3 && days <= 10, "拐点位置应靠近中段，实际偏移 " + days);
    }

    @Test
    void smallNoiseNotInflection() {
        // 在 +0.3 附近震荡，无符号反转 + 振幅小
        double[] vals = new double[20];
        for (int i = 0; i < vals.length; i++) vals[i] = 0.3 + Math.sin(i) * 0.2;
        assertTrue(SentimentTrend.detect(series(vals), 3, 1.5).isEmpty());
    }

    @Test
    void computeReturnsSameSeriesAndInflections() {
        List<DateTone> s = series(2, 2, 2, 2, 2, 2, 2, -2, -2, -2, -2, -2, -2, -2);
        SentimentSeries result = new SentimentTrend().compute(s);
        assertSame(s, result.series(), "compute 不应拷贝输入序列");
        assertEquals(SentimentTrend.detect(s,
                SentimentTrend.DEFAULT_WINDOW,
                SentimentTrend.DEFAULT_THRESHOLD).size(),
                result.inflectionPoints().size());
    }
}
