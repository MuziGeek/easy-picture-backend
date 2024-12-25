package com.muzi.easypicturebackend.utils;

public class ColorTransformUtils {

    private ColorTransformUtils() {
        // 工具类不需要实例化
    }

    /**
     * 颜色转换工具类
     * @param color
     * @return
     */
    public static String transformColor(String color) {
       if (color.length() == 7) {
           color =color.substring(0,4)+"0"+color.substring(4,7);
       }
       return color;
    }
}
