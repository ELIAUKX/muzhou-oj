package com.muzhou.mzojcodesandbox;

public class SimpleCompute {

    public static void main(String[] args) {
        // 从参数数组中拿到a和b
        int a = Integer.parseInt(args[0]);
        int b = Integer.parseInt(args[1]);
        System.out.println("结果:" + (a + b));
    }
}
