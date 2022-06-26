package top.panghai.fifthchess.utils;

public class HttpStatus {

    public static final String CODE = "code";
    public static final String MSG = "msg";
    public static final String DATA = "data";
    public static final String INFO = "info";
    public static final String TYPE = "type";

    /**
     * 匹配成功
     */
    public static final String MATCH_SUCCESS = "0";

    /**
     * 操作成功
     */
    public static final String SUCCESS = "200";

    /**
     * 系统内部错误
     */
    public static final String ERROR = "500";

    /**
     * 超时
     */
    public static final String TIMEOUT = "10000";

    /**
     * JSON对象转换错误
     */
    public static final String JSON_ERROR = "10001";

    /**
     * 缺少参数
     */
    public static final String PARAMS_LACK_ERROR = "10002";

    /**
     * 用户不存在
     */
    public static final String NOT_USER_ERROR = "10003";


    /**
     * 对手认输
     */
    public static final String RIVAL_GIVE_UP = "10004";

}
