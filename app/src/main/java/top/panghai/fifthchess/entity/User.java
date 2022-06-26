package top.panghai.fifthchess.entity;

import android.graphics.Bitmap;
import android.os.Binder;

/**
 * 用户对象 user
 *
 * @author panghai
 * @date 2022-06-15
 */
public class User extends Binder {

    /**
     * 用户id
     */
    private String userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private Bitmap avatar;

    /**
     * 等级分
     */
    private double rating;

    /**
     * 积分
     */
    private int integral;

    /**
     * 段位
     */
    private int rank;

    /**
     * 段位名称
     */
    private String rankName;

    /**
     * 开始匹配时间
     */
    private Long matchTime;

    /**
     * 比赛结果
     * 胜 1
     * 平 0
     * 负 -1
     */
    private int playRes;

    /**
     * 对手等级分
     */
    private double rivalRating;

    /**
     * 是否先手
     */
    private boolean isFirst;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Bitmap getAvatar() {
        return avatar;
    }

    public void setAvatar(Bitmap avatar) {
        this.avatar = avatar;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getIntegral() {
        return integral;
    }

    public void setIntegral(int integral) {
        this.integral = integral;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getRankName() {
        return rankName;
    }

    public void setRankName(String rankName) {
        this.rankName = rankName;
    }

    public Long getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(Long matchTime) {
        this.matchTime = matchTime;
    }

    public int getPlayRes() {
        return playRes;
    }

    public void setPlayRes(int playRes) {
        this.playRes = playRes;
    }

    public double getRivalRating() {
        return rivalRating;
    }

    public void setRivalRating(double rivalRating) {
        this.rivalRating = rivalRating;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", nickname='" + nickname + '\'' +
                ", avatar=" + avatar +
                ", rating=" + rating +
                ", integral=" + integral +
                ", rank=" + rank +
                ", rankName='" + rankName + '\'' +
                ", matchTime=" + matchTime +
                ", playRes=" + playRes +
                ", rivalRating=" + rivalRating +
                ", isFirst=" + isFirst +
                '}';
    }
}
