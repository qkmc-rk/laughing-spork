package xyz.ruankun.laughingspork.util;

import java.util.HashMap;
import java.util.Map;

public class EasyPwdValidator {
    private final static Integer MIN_LENGTH = 6;

    // 字母表
    private final static String SMALL_LETTERS = "abcdefghijklmnopqrstuvwxyz";

    private final static String BIG_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // 特殊字符
    private final static String SPECIAL_CHARS = "~!@#$%^&*()_+-={}[],./<>?:|";

    private final static String NUMBERS = "1234567890";

    /**
     *  密码要求: 长度大于6位.
     *  @param pwd
     *  @return
     */
    public static Map<Boolean,String> validate(String pwd){

        Map<Boolean, String> map = new HashMap<>(1);
        if (null == pwd){
            map.put(false, "pwd is null.");
        }
        boolean isLongEnough = (pwd.length() >= MIN_LENGTH)?true:false;
        if (!isLongEnough){
            //长度不够
            map.put(false,"密码长度必须超过6位.");
            return map;
        }else {
            map.put(true,"password is validated");
            return map;
        }

    }
}
