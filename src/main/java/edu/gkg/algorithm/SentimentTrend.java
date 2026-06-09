package edu.gkg.algorithm;

import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.SharedRecords.Inflection;
import edu.gkg.common.SharedRecords.SentimentSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * F-16 情感趋势 + 滑动窗口拐点检测（纯函数实现）。
 *
 * 上层用法：先用 DAO 把指定实体的 (date, avgTone, count) 序列查出来，
 * 然后调 {@link #detect(List, int, double)} 得到拐点。
 *
 * 拐点判定：对每个时间点 i，比较"前 W 个点的均值符号"与"后 W 个点的均值符号"——
 *   若符号反转 (正→负 / 负→正) 且 |前均 - 后均| ≥ threshold，则 i 为拐点。
 *
 * 默认窗口 W=3，阈值 1.5。
 */
public class SentimentTrend {

    public static final int    DEFAULT_WINDOW    = 3;
    public static final double DEFAULT_THRESHOLD = 1.5;

    public SentimentSeries compute(List<DateTone> series) {
        return compute(series, DEFAULT_WINDOW, DEFAULT_THRESHOLD);
    }

    public SentimentSeries compute(List<DateTone> series, int window, double threshold) {
        Objects.requireNonNull(series);
        return new SentimentSeries(series, detect(series, window, threshold));
    }

    /**
     * 滑动窗口拐点检测。纯函数：相同输入永远得到相同输出。
     */
    public static List<Inflection> detect(List<DateTone> series, int window, double threshold) {
        List<Inflection> out = new ArrayList<>();
        if (series == null || series.size() < window * 2 + 1) return out;

        for (int i = window; i < series.size() - window; i++) {
            double left  = mean(series, i - window, i);          // [i-W, i)
            double right = mean(series, i + 1, i + 1 + window);  // (i, i+W]
            if (Math.signum(left) == Math.signum(right)) continue;
            if (left == 0 || right == 0) continue;
            double mag = Math.abs(left - right);
            if (mag < threshold) continue;
            String dir = left < 0 && right > 0 ? "NEG_TO_POS" : "POS_TO_NEG";
            out.add(new Inflection(series.get(i).date(), dir, mag));
        }
        return out;
    }

    private static double mean(List<DateTone> s, int fromIncl, int toExcl) {
        double sum = 0;
        int n = 0;
        for (int k = fromIncl; k < toExcl; k++) {
            sum += s.get(k).avgTone();
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }
}
