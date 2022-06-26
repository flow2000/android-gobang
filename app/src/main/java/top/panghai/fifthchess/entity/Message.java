package top.panghai.fifthchess.entity;

import java.io.Serializable;

/**
 * @Author: panghai
 * @Date: 2022/05/15/16:57
 * @Description: 消息类
 */
public class Message implements Serializable {

    /** 发送者 */
    private String from;

    /** 接收者 */
    private String to;

    /** 消息类型 */
    private int type;

    /** 消息内容 */
    private String info;

    public Message() {

    }

    public Message(String from, String to, int type, String info) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.info = info;
    }

    @Override
    public String toString() {
        return "Message{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", type=" + type +
                ", info='" + info + '\'' +
                '}';
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
