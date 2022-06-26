package top.panghai.fifthchess.utils;

/**
 * @Author: panghai
 * @Date: 2022/06/17/10:27
 * @Description: 分数段位
 */
public class Rank {

    /**
     * 段位
     */
    public static final String V1_NAME = "草民";
    public static final String V2_NAME = "庶士";
    public static final String V3_NAME = "书生";
    public static final String V4_NAME = "秀才";
    public static final String V5_NAME = "棋痴";
    public static final String V6_NAME = "棋圣";
    public static final String V7_NAME = "棋仙";

    /**
     * 等级积分
     * V1-V7分别是
     * 草民、庶士、书生、秀才、棋痴、棋圣、棋仙
     */
    public static final int V1 = 0;
    public static final int V2 = 25;
    public static final int V3 = 50;
    public static final int V4 = 75;
    public static final int V5 = 100;
    public static final int V6 = 125;
    public static final int V7 = 150;

    /**
     * 等级分
     * E1-E7分别是
     * 草民、庶士、书生、秀才、棋痴、棋圣、棋仙
     */
    public static final int E1 = 1500;
    public static final int E2 = 1700;
    public static final int E3 = 1800;
    public static final int E4 = 1900;
    public static final int E5 = 2000;
    public static final int E6 = 2100;
    public static final int E7 = 2200;

    /**
     * 等级分浮动系数
     * E1_K-E7_K分别是
     * 草民、庶士、书生、秀才、棋痴、棋圣、棋仙
     */
    public static final int E1_K = 40;
    public static final int E2_K = 35;
    public static final int E3_K = 30;
    public static final int E4_K = 20;
    public static final int E5_K = 15;
    public static final int E6_K = 10;
    public static final int E7_K = 5;

}
