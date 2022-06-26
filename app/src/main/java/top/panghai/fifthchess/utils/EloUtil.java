package top.panghai.fifthchess.utils;


import java.util.HashMap;
import java.util.Map;

import top.panghai.fifthchess.entity.User;

/**
 * @Author: panghai
 * @Date: 2022/06/16/20:29
 * @Description: ELO等级分计算工具
 * 等级分计算公式为Rn=Ro+K*(W-We)
 * Rn和Ro：分别代表比赛前和比赛后的等级分
 * W：比赛结果，胜为1.0，平为0.5，负为0.0
 * We：预期结果，即期望值
 * K：浮动系数，这个值应该视选手的资力而定。
 * 资力越深，参加的比赛应该越多，因此K值应该越低。通常业余选手为30，专业棋手为15，大师为10，特级大师为5。
 */

public class EloUtil {

    /**
     * 先行权优势
     */
    private final static double R = 30;

    /**
     * 比赛结果为胜
     */
    public static int Win = 1;

    /**
     * 比赛结果为平
     */
    public static int Draw = 0;

    /**
     * 比赛结果为负
     */
    public static int Loss = -1;

    /**
     * 等级积分对应段位
     */
    private static final Map<Integer, String> rankMap = new HashMap<Integer, String>() {{
        put(Rank.V1, Rank.V1_NAME);
        put(Rank.V2, Rank.V2_NAME);
        put(Rank.V3, Rank.V3_NAME);
        put(Rank.V4, Rank.V4_NAME);
        put(Rank.V5, Rank.V5_NAME);
        put(Rank.V6, Rank.V6_NAME);
        put(Rank.V7, Rank.V7_NAME);
    }};

    /**
     * 计算期望值
     *
     * @param R1 自己的等级分
     * @param R2 对手的等级分
     * @return 期望值(保留4位小数)
     */
    public static double wishVal(double R1, double R2) {
        double we = 1 / (1 + Math.pow(10, (R2 - R1 - R) / 400));
        return Math.round(we * 1000) / 1000.0;
    }

    /**
     * 计算浮动系数K
     *
     * @param R1 等级分
     * @return 浮动系数
     */
    public static int flo(double R1) {
        if (R1 >= Rank.E7) {
            return Rank.E7_K;
        } else if (R1 >= Rank.E6) {
            return Rank.E6_K;
        } else if (R1 >= Rank.E5) {
            return Rank.E5_K;
        } else if (R1 >= Rank.E4) {
            return Rank.E4_K;
        } else if (R1 >= Rank.E3) {
            return Rank.E3_K;
        } else if (R1 >= Rank.E2) {
            return Rank.E2_K;
        } else if (R1 >= Rank.E1) {
            return Rank.E1_K;
        }
        return 30;
    }

    /**
     * 计算比赛后等级分
     *
     * @param R1 自己的等级分
     * @param R2 对手的等级分
     * @param w  比赛结果
     * @return 赛后等级分(保留4位小数)
     */
    public static double calculate(double R1, double R2, double w) {
        double we = wishVal(R1, R2);
        int k = flo(R1);
        double Rn = R1 + k * (w - we);
        return Math.round(Rn * 1000) / 1000.0;
    }

    /**
     * 根据积分和等级分匹配分段
     *
     * @param user 用户
     * @return 用户
     */
    public static User match(User user) {
        if (user.getIntegral() >= Rank.V7) {
            user.setRankName(Rank.V7_NAME);
        } else if (user.getIntegral() >= Rank.V6) {
            user.setRankName(Rank.V6_NAME);
        } else if (user.getIntegral() >= Rank.V5) {
            user.setRankName(Rank.V5_NAME);
        } else if (user.getIntegral() >= Rank.V4) {
            user.setRankName(Rank.V4_NAME);
        } else if (user.getIntegral() >= Rank.V3) {
            user.setRankName(Rank.V3_NAME);
        } else if (user.getIntegral() >= Rank.V2) {
            user.setRankName(Rank.V2_NAME);
        } else if (user.getIntegral() >= Rank.V1) {
            user.setRankName(Rank.V1_NAME);
        } else {
            user.setRankName(Rank.V1_NAME);
        }
        if(user.getRating() >= Rank.E7){
            user.setRank(7);
        } else if (user.getRating() >= Rank.E6) {
            user.setRank(6);
        } else if (user.getRating() >= Rank.E5) {
            user.setRank(5);
        } else if (user.getRating() >= Rank.E4) {
            user.setRank(4);
        } else if (user.getRating() >= Rank.E3) {
            user.setRank(3);
        } else if (user.getRating() >= Rank.E2) {
            user.setRank(2);
        } else if (user.getRating() >= Rank.E1) {
            user.setRank(1);
        } else {
            user.setRank(1);
        }
        return user;
    }

}
