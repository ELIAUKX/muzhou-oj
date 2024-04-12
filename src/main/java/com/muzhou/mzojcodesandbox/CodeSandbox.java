package com.muzhou.mzojcodesandbox;


import com.muzhou.mzojcodesandbox.model.ExecuteCodeRequest;
import com.muzhou.mzojcodesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱的接口定义
 */
public interface CodeSandbox {

    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
