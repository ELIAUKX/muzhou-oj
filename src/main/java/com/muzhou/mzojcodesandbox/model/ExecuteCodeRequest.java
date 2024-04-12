package com.muzhou.mzojcodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {

    /**
     * 执行测试的一组输入
     */
    private List<String> inputList;

    /**
     * 进行测试的代码
     */
    private String code;

    /**
     * 测试代码的编程语言
     */
    private String language;
}
