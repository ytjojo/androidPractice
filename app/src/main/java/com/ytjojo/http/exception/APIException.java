package com.ytjojo.http.exception;

/**
 * Created by Administrator on 2016/5/6 0006.
 */
public class APIException extends RuntimeException{
    //对应HTTP的状态码
    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;
    private static final int REQUEST_TIMEOUT = 408;
    private static final int INTERNAL_SERVER_ERROR = 500;
    private static final int BAD_GATEWAY = 502;
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final int GATEWAY_TIMEOUT = 504;
    public int code;
    public APIException(int code, String msg,String reponse) {
        super(msg);
    }
    public APIException(int code, String msg) {
        super(msg);
        this.code = code;
    }
    public APIException(int code, String msg,Throwable throwable) {
        super(msg,throwable);
        this.code = code;
    }
    public int getCode(){
        return code;
    }
}
