package com.tencent.devops.common.util;

import kotlin.Triple;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CronBuilder {

    private CronBuilder() {
    }

    private static final String ZERO = "0";
    private static final String SPACE = " ";
    private static final String EMPTY = "";
    private static final String SLASH = "/";
    private static final String ASTERISK = "*";
    private static final String QUESTION_MARK = "?";
    private static final String WELL_NO = "#";
    private static final String LAST = "L";
    private static final String DASHED = "-";
    private static final String COMMA = ",";
    private static final String LASTWEEK = "LW";
    private static final String WEEK = "W";

    private String seconds;
    private String minutes;
    private String hours;
    private String dayOfMonth;
    private String month;
    private String dayOfWeek;
    private String year;


    /**
     * 构建器
     *
     * @return {@link CronBuilder}  构建器对象
     */
    public static CronBuilder builder() {
        return new CronBuilder();
    }

    /**
     * 记录Cron 表达式
     */
    private StringJoiner joiner = new StringJoiner(SPACE);

    /**
     * 返回Cron 表达式
     *
     * @return Cron 表达式
     */
    public String build() {

        generateCronValue();

        joiner.add(seconds).add(minutes).add(hours).add(dayOfMonth).add(month).add(dayOfWeek);

        String cron = joiner.toString();
        joiner = null;
        return cron;
    }


    private void generateCronValue() {
        initBuildValue();
        if (isNotBlank(this.dayOfMonth) && isBlank(this.dayOfWeek)) {
            this.dayOfWeek = QUESTION_MARK;
        } else if (isNotBlank(this.dayOfWeek) && isBlank(this.dayOfMonth)) {
            this.dayOfMonth = QUESTION_MARK;
        } else if (isNotBlank(this.dayOfWeek) && isNotBlank(this.dayOfMonth)) {
            if (equals(QUESTION_MARK, this.dayOfMonth) && equals(QUESTION_MARK, this.dayOfWeek)) {
                this.dayOfWeek = QUESTION_MARK;
            }
            if (equals(ASTERISK, this.dayOfMonth) && equals(ASTERISK, this.dayOfWeek)) {
                this.dayOfWeek = QUESTION_MARK;
            }

            if (notEquals(QUESTION_MARK, this.dayOfMonth) && notEquals(QUESTION_MARK, this.dayOfWeek)) {
                if (equals(ASTERISK, this.dayOfMonth)
                        && notEquals(QUESTION_MARK, this.dayOfWeek) && notEquals(ASTERISK, this.dayOfWeek)) {
                    this.dayOfMonth = QUESTION_MARK;
                } else if (equals(ASTERISK, this.dayOfWeek)
                        && notEquals(QUESTION_MARK, this.dayOfMonth) && notEquals(ASTERISK, this.dayOfMonth)) {
                    this.dayOfWeek = QUESTION_MARK;
                } else if (notEquals(QUESTION_MARK, this.dayOfWeek)
                        && notEquals(ASTERISK, this.dayOfWeek) && isNumeric(this.dayOfMonth)) {
                    this.dayOfMonth = QUESTION_MARK;
                } else if (notEquals(QUESTION_MARK, this.dayOfMonth)
                        && notEquals(ASTERISK, this.dayOfMonth) && isNumeric(this.dayOfWeek)) {
                    this.dayOfWeek = QUESTION_MARK;
                } else {
                    this.dayOfWeek = QUESTION_MARK;
                }
            }

        } else if (isBlank(this.dayOfWeek) && isBlank(this.dayOfMonth)) {
            this.dayOfWeek = QUESTION_MARK;
        }
    }

    private void initBuildValue() {
        if (isBlank(this.seconds)) {
            this.seconds = ASTERISK;
        }
        if (isBlank(this.minutes)) {
            this.minutes = ASTERISK;
        }
        if (isBlank(this.hours)) {
            this.hours = ASTERISK;
        }
        if (isBlank(this.minutes)) {
            this.minutes = ASTERISK;
        }
        if (isBlank(this.dayOfMonth)) {
            this.dayOfMonth = ASTERISK;
        }
        if (isBlank(this.month)) {
            this.month = ASTERISK;
        }
        if (isBlank(this.dayOfWeek)) {
            this.dayOfWeek = ASTERISK;
        }
        if (isBlank(this.year)) {
            this.year = ASTERISK;
        }
    }

    /**
     * 每秒
     *
     * @return 返回当前对象
     */
    public CronBuilder seconds() {
        this.seconds = ASTERISK;
        return this;
    }

    /**
     * 指定秒
     *
     * @param seconds 秒，0~59的整数
     * @return 返回当前对象
     */
    public CronBuilder seconds(Integer... seconds) {
        setTime(0, seconds);
        this.seconds = getSampleValue(0, 59, Arrays.asList(seconds)).toString();
        return this;
    }


    /**
     * 指定秒 周期
     *
     * @param start         开始，0~59的整数
     * @param endOrInterval 结束/间隔，0~59的整数
     * @param isCycle       true: 是周期，false: 是间隔
     * @return 返回当前对象
     */
    public CronBuilder seconds(int start, int endOrInterval, boolean isCycle) {
        Triple<Integer, String, Integer> triple = getIntervalAndCycleValue(start, endOrInterval,
                0, 59, isCycle);
        this.seconds = triple.getFirst() + triple.getSecond() + triple.getThird();
        return this;
    }


    /**
     * 每分
     *
     * @return 返回当前对象
     */
    public CronBuilder minutes() {
        setDefaultValueIfBlank(true, false, false, false, false);
        this.minutes = ASTERISK;
        return this;
    }

    /**
     * 指定分
     *
     * @param minutesArr 分，0~59的整数
     * @return 返回当前对象
     */
    public CronBuilder minutes(Integer... minutesArr) {
        setTime(0, minutesArr);
        setDefaultValueIfBlank(true, false, false, false, false);
        this.minutes = getSampleValue(0, 59, Arrays.asList(minutesArr)).toString();
        return this;
    }

    /**
     * 指定分 周期
     *
     * @param start         开始，0~59的整数
     * @param endOrInterval 结束/间隔，0~59的整数
     * @param isCycle       true: 是周期，false: 是间隔
     * @return 返回当前对象
     */
    public CronBuilder minutes(int start, int endOrInterval, boolean isCycle) {
        setDefaultValueIfBlank(true, false, false, false, false);
        Triple<Integer, String, Integer> triple = getIntervalAndCycleValue(start, endOrInterval,
                0, 59, isCycle);
        this.minutes = triple.getFirst() + triple.getSecond() + triple.getThird();
        return this;
    }

    /**
     * 每小时
     *
     * @return 返回当前对象
     */
    public CronBuilder hours() {
        setDefaultValueIfBlank(true, true, false, false, false);
        this.hours = ASTERISK;
        return this;
    }

    /**
     * 指定时
     *
     * @param hoursArr 分，0~23的整数
     * @return 返回当前对象
     */
    public CronBuilder hours(Integer... hoursArr) {
        setTime(0, hoursArr);
        setDefaultValueIfBlank(true, true, false, false, false);
        this.hours = getSampleValue(0, 23, Arrays.asList(hoursArr)).toString();
        ;
        return this;
    }

    /**
     * 指定分 周期
     *
     * @param start         开始，0~23的整数
     * @param endOrInterval 结束/间隔，0~23的整数
     * @param isCycle       true: 是周期，false: 是间隔
     * @return 返回当前对象
     */
    public CronBuilder hours(int start, int endOrInterval, boolean isCycle) {
        setDefaultValueIfBlank(true, true, false, false, false);
        Triple<Integer, String, Integer> triple = getIntervalAndCycleValue(start, endOrInterval,
                0, 23, isCycle);
        this.minutes = triple.getFirst() + triple.getSecond() + triple.getThird();
        return this;
    }

    /**
     * 每日
     *
     * @return 返回当前对象
     */
    public CronBuilder dayOfMonth() {
        setDefaultValueIfBlank(true, true, true, false, false);
        this.dayOfMonth = ASTERISK;
        return this;
    }

    /**
     * 指定日
     *
     * @param days 日，0~31的整数
     * @return 返回当前对象
     */
    public CronBuilder dayOfMonth(Integer... days) {
        setTime(1, days);
        setDefaultValueIfBlank(true, true, true, false, false);
        this.dayOfMonth = getSampleValue(1, 31, Arrays.asList(days)).toString();
        return this;
    }

    /**
     * 指定天 周期
     *
     * @param start         开始，0~31的整数
     * @param endOrInterval 结束/间隔，0~31的整数
     * @param isCycle       true: 是周期，false: 是间隔
     * @return 返回当前对象
     */
    public CronBuilder dayOfMonth(int start, int endOrInterval, boolean isCycle) {
        setDefaultValueIfBlank(true, true, true, false, false);
        Triple<Integer, String, Integer> triple = getIntervalAndCycleValue(start, endOrInterval,
                1, 31, isCycle);
        this.dayOfMonth = triple.getFirst() + triple.getSecond() + triple.getThird();
        return this;
    }

    /**
     * 月最后一日
     *
     * @return {@link CronBuilder} 返回当前对象
     */
    public CronBuilder dayOfMonthLast() {
        setDefaultValueIfBlank(true, true, true, false, false);
        this.dayOfMonth = LAST;
        return this;
    }

    /**
     * 月最后一个工作日
     *
     * @return {@link CronBuilder} 返回当前对象
     */
    public CronBuilder dayOfMonthLastWeek() {
        setDefaultValueIfBlank(true, true, true, false, false);
        this.dayOfMonth = LASTWEEK;
        return this;
    }

    /**
     * 每月指定时间最近的那个工作日
     *
     * @return {@link CronBuilder} 返回当前对象
     */
    public CronBuilder dayOfMonthWeek(int day) {
        if (day > 31) {
            day = 31;
        }
        if (day < 1) {
            day = 1;
        }
        setDefaultValueIfBlank(true, true, true, false, false);
        this.dayOfMonth = day + WEEK;
        return this;
    }

    /**
     * 每月
     *
     * @return 返回当前对象
     */
    public CronBuilder month() {
        setDefaultValueIfBlank(true, true, true, false, true);
        this.month = ASTERISK;
        return this;
    }

    /**
     * 指定月
     *
     * @param months 月，1~12的整数
     * @return 返回当前对象
     */
    public CronBuilder month(Integer... months) {
        setTime(1, months);
        setDefaultValueIfBlank(true, true, true, false, true);
        this.month = getSampleValue(1, 12, Arrays.asList(months)).toString();
        return this;
    }

    /**
     * 指定月周期
     *
     * @param start         开始，0~12的整数
     * @param endOrInterval 结束/间隔，0~12的整数
     * @param isCycle       true: 是周期，false: 是间隔
     * @return 返回当前对象
     */
    public CronBuilder month(int start, int endOrInterval, boolean isCycle) {
        setDefaultValueIfBlank(true, true, true, false, true);
        Triple<Integer, String, Integer> triple = getIntervalAndCycleValue(start, endOrInterval,
                1, 12, isCycle);
        this.month = triple.getFirst() + triple.getSecond() + triple.getThird();
        return this;
    }

    /**
     * 每周
     *
     * @return 返回当前对象
     */
    public CronBuilder dayOfWeek() {
        setDefaultValueIfBlank(true, true, true, true, false);
        this.dayOfWeek = ASTERISK;
        return this;
    }

    /**
     * 指定周
     *
     * @param dayOfWeeks 周，1~7的整数
     * @return 返回当前对象
     */
    public CronBuilder dayOfWeek(Integer... dayOfWeeks) {
        setTime(1, dayOfWeeks);
        setDefaultValueIfBlank(true, true, true, true, false);
        this.dayOfWeek = getSampleValue(1, 7, Arrays.asList(dayOfWeeks)).toString();
        return this;
    }

    /**
     * 指定周 周期
     *
     * @param start         开始，1-7, 1-4
     * @param endOrInterval 结束/间隔，1-5
     * @param isCycle       true: 是周期，false: 是间隔
     * @return 返回当前对象
     */
    public CronBuilder dayOfWeek(int start, int endOrInterval, boolean isCycle) {

        String symbol;

        if (isCycle) {
            if (start > 7) {
                start = 7;
            }
            if (start < 1) {
                start = 1;
            }
            if (endOrInterval > 7) {
                endOrInterval = 7;
            }
            if (endOrInterval < 2) {
                endOrInterval = 2;
            }
            symbol = DASHED;
        } else {
            if (start > 7) {
                start = 7;
            }
            if (start < 0) {
                start = 0;
            }
            if (endOrInterval > 5) {
                endOrInterval = 5;
            }
            if (endOrInterval < 1) {
                endOrInterval = 1;
            }
            symbol = WELL_NO;
        }
        setDefaultValueIfBlank(true, true, true, true, false);
        this.dayOfWeek = start + symbol + endOrInterval;
        return this;
    }


    /**
     * 本月的最后一个星期几
     *
     * @return {@link CronBuilder}  返回当前对象
     */
    public CronBuilder dayOfWeekLast(int week) {

        if (week > 7) {
            week = 7;
        }
        if (week < 1) {
            week = 1;
        }

        setDefaultValueIfBlank(true, true, true, true, false);
        this.dayOfWeek = week + LAST;
        return this;
    }

    /**
     * 每年
     *
     * @return 返回当前对象
     */
    public CronBuilder year() {
        setDefaultValueIfBlank(true, true, true, true, true);
        this.year = ASTERISK;
        return this;
    }

    /**
     * 指定年
     *
     * @param year 年
     * @return 返回当前对象
     */
    public CronBuilder year(int year) {
        setDefaultValueIfBlank(true, true, true, true, true);
        this.year = year < getCurrentYear() ? ASTERISK : year + EMPTY;
        return this;
    }


    /**
     * 指定年 周期
     *
     * @param start 开始  1970~2099
     * @param end   结束  1970~2099
     * @return 返回当前对象
     */
    public CronBuilder year(int start, int end) {

        if (start > end) {
            start = end;
        }
        if (start < getCurrentYear()) {
            start = getCurrentYear();
        }
        if (end < getCurrentYear()) {
            end = getCurrentYear();
        }
        setDefaultValueIfBlank(true, true, true, true, true);
        this.year = start + DASHED + end;
        return this;
    }

    /**
     * 是否相等
     *
     * @param obj1 对象
     * @param obj2 对象
     * @return boolean
     */
    private static boolean equals(Object obj1, Object obj2) {
        return obj1.equals(obj2);
    }

    /**
     * 是否不相等
     *
     * @param obj1 对象
     * @param obj2 对象
     * @return boolean
     */
    private static boolean notEquals(Object obj1, Object obj2) {
        return !obj1.equals(obj2);
    }

    /**
     * 设置时间
     *
     * @param index0Value 索引0位置的值
     * @param time        时间
     */
    private static void setTime(Integer index0Value, Integer... time) {
        if (time == null || time.length <= 0) {
            time = new Integer[1];
            time[0] = index0Value;
        }
    }

    /**
     * 获取当前年
     *
     * @return 当前年份
     */
    private static int getCurrentYear() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR);
    }

    /**
     * 是数字
     *
     * @param str str
     * @return boolean
     */
    private static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        return !isNum.matches() ? Boolean.FALSE : Boolean.TRUE;
    }

    private StringJoiner getSampleValue(Integer start, Integer end, List<Integer> values) {
        StringJoiner stringJoiner = new StringJoiner(COMMA);
        for (Integer value : values) {
            if (value > end) {
                value = end;
            }
            if (value < start) {
                value = start;
            }
            stringJoiner.add(value + EMPTY);
        }
        return stringJoiner;
    }

    private Triple<Integer, String, Integer> getIntervalAndCycleValue(int start, int endOrInterval, int leftBorder,
                                                                      int rightBorder, boolean isCycle) {
        String symbol;
        if (isCycle) {
            if (start > rightBorder) {
                start = rightBorder;
            }
            int cycleStart = leftBorder == 0 ? 1 : leftBorder;
            if (start < cycleStart) {
                start = cycleStart;
            }
            if (endOrInterval > rightBorder) {
                endOrInterval = rightBorder;
            }
            if (endOrInterval < cycleStart + 1) {
                endOrInterval = cycleStart + 1;
            }
            symbol = DASHED;
        } else {
            if (start > rightBorder) {
                start = rightBorder;
            }
            if (start < leftBorder) {
                start = leftBorder;
            }
            if (endOrInterval > rightBorder) {
                endOrInterval = rightBorder;
            }
            if (endOrInterval < leftBorder + 1) {
                endOrInterval = leftBorder + 1;
            }
            symbol = SLASH;
        }
        return new Triple<Integer, String, Integer>(start, symbol, endOrInterval);
    }

    private void setDefaultValueIfBlank(boolean seconds, boolean minutes, boolean hours,
                                        boolean month, boolean dayOfMonth) {
        if (seconds && isBlank(this.seconds)) {
            seconds(0);
        }
        if (minutes && isBlank(this.minutes)) {
            minutes(0);
        }
        if (hours && isBlank(this.hours)) {
            hours(0);
        }
        if (month && isBlank(this.month)) {
            month(1);
        }
        if (dayOfMonth && isBlank(this.dayOfMonth)) {
            dayOfMonth(1);
        }
    }

}
