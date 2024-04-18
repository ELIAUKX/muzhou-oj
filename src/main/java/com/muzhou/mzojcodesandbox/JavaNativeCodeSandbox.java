package com.muzhou.mzojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.muzhou.mzojcodesandbox.model.ExecuteCodeRequest;
import com.muzhou.mzojcodesandbox.model.ExecuteCodeResponse;
import com.muzhou.mzojcodesandbox.model.ExecuteMessage;
import com.muzhou.mzojcodesandbox.model.JudgeInfo;
import com.muzhou.mzojcodesandbox.security.DefaultSecurityManager;
import com.muzhou.mzojcodesandbox.security.DenySecurityManager;
import com.muzhou.mzojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandbox implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 10000L; // 程序执行最长时间

    public static final String SECURITY_MANAGER_PATH = "C:\\MyProject\\mzoj-code-sandbox\\src\\main\\resources\\security"; // 不包含最后一个Java文件路径

    public static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";

    public static final List<String> blackList = Arrays.asList("Files", "exec"); // 黑名单，这里面的命令不能执行！

    public static final WordTree WORD_TREE;

    static {
        // 初始化字典树
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
    }

    public static void main(String[] args) {
        // testCode/simpleComputeArgs
        // 测试代码
        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        // todo 这里记得改,下面那种方式是从控制台读入参数
//        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/" + GLOBAL_JAVA_CLASS_NAME, StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/simpleCompute/" + GLOBAL_JAVA_CLASS_NAME, StandardCharsets.UTF_8);

        String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
//        System.setSecurityManager(new DenySecurityManager());

        // 1.把用户的代码保存为文件
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 校验代码中是否包含黑名单中的禁用词
//        FoundWord foundWord = WORD_TREE.matchWord(code);
//        if (foundWord != null) {
//            // 找到了匹配的关键词，那就说明代码里面包含关键词
//            System.out.println("包含禁止词语：" + foundWord.getFoundWord());
//            return null;
//        }

        // 拿到项目的工作根目录
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME; // 定义保存文件的全局代码目录
        // 判断全局代码目录是否存在，如果不存在就新建文件夹
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 把用户代码隔离存放(每个代码都存放在一个单独文件夹内)
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        // 实际的代码存放文件
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        // 2.编译代码，得到class文件
        String compileCommand = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());// 编译命令
        try {
            // java执行程序，编译为class文件
            Process compileProcess = Runtime.getRuntime().exec(compileCommand);
            // 这个执行的是编译命令
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            return getErrorResponse(e); // 返回自定义的错误响应
        }

        // 3.执行代码，得到输出结果
        ArrayList<ExecuteMessage> executeMessageList = new ArrayList<>(); // 输出信息列表
        for (String inputArgs : inputList) {
            String runCommand = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            // 安全管理器这里貌似有些问题，没有检测到？
//            String runCommand = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
            // 这个执行的是运行命令
            try {
                Process runProcess = Runtime.getRuntime().exec(runCommand);

                // 超时控制，通过创建一个守护线程，超时后自动中断Process实现
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了，中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                // todo 这里记得改,下面那种方式是从控制台读入参数
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
//                ExecuteMessage executeMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess, inputArgs);
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage); // 将获取到的输出信息添加到列表中
            } catch (IOException e) {
                return getErrorResponse(e);
            }
        }

        // 4.收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        ArrayList<String> outputList = new ArrayList<>();
        long maxTime = 0; //记录最大的那个时间,只要最大值超时，就判断整个程序超时
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            // 错误信息不为空，就记录错误信息，并且跳出这一次循环，不执行下面的outputList.add方法
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage()); // 没问题就将处理结果信息添加到列表中
            Long time = executeMessage.getTime(); // 获取当前执行的时间
            // 获取最大运行时间
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        executeCodeResponse.setOutputList(outputList);
        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime); // 时间设置
        executeCodeResponse.setJudgeInfo(judgeInfo);
        // todo 要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//        judgeInfo.setMemory(); // 内存设置

        // 5.文件清理，释放空间
        if (userCodeFile.getParentFile() != null) {
            // 判断不为空再去删除文件
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }

        return executeCodeResponse;
    }

    /**
     * 获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2); // 表示代码沙箱错误
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
