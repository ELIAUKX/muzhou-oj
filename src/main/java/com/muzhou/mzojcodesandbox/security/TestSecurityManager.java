package com.muzhou.mzojcodesandbox.security;

import cn.hutool.core.io.FileUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TestSecurityManager {
    public static void main(String[] args) {
        // 开启安全管理器
        System.setSecurityManager(new MySecurityManager());
        // 测试读文件
//        List<String> strings = FileUtil.readLines("C:\\MyProject\\mzoj-code-sandbox\\src\\main\\resources\\application.yml", StandardCharsets.UTF_8);
//        System.out.println(strings);

        // 测试写文件
        FileUtil.writeString("aa","aaa", Charset.defaultCharset());
    }
}
