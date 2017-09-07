package com.ytjojo.http;

import java.io.Serializable;

/**
 * Created by Administrator on 2016/10/18 0018.
 */
public class ServerResponse<T> implements Serializable{
    public static int RESULT_OK =200;
    public static int EXCEPTION_CCONVERT_JSON =-1;
    public int code;
    public String msg;
    public T body;
    public ServerResponse(int code, String msg, T body){
        this.code =code;
        this.msg = msg;
        this.body = body;
    }
    public ServerResponse(){
    }
}