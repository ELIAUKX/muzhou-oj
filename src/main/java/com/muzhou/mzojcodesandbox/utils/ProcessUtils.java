package com.muzhou.mzojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.muzhou.mzojcodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;

/**
 * 进程工具类
 */
public class ProcessUtils {

    /**
     * 执行进程获取信息
     *
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch = new StopWatch();// 利用spring的StopWatch对象记录程序运行时间
            stopWatch.start(); // 开始记录时间
            // 等待程序执行，获取错误码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            // 如果正常退出
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                // 获取控制台的输出并输出(分批获取)
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine).append("\n");
                }
                executeMessage.setMessage(compileOutputStringBuilder.toString());
                // 关闭读取流
                bufferedReader.close();
            } else {
                // 异常退出
                System.out.println(opName + "失败，错误码：" + exitValue);
                // 获取控制台的输出并输出(分批获取)
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine).append("\n");
                }
                executeMessage.setMessage(compileOutputStringBuilder.toString());
                // 关闭读取流
                bufferedReader.close();
                // 分批次读取错误流信息(控制台的输出会被读入到输入流里，所以我们读取输入流)
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorCompileOutputStringBuilder.append(errorCompileOutputLine).append("\n");
                }
                executeMessage.setErrorMessage(errorCompileOutputStringBuilder.toString());
                // 关闭读取流
                errorBufferedReader.close();
            }
            stopWatch.stop(); // 停止统计时间
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis()); // 设置统计的时间
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 执行交互式进程获取信息(这种从控制台读入参数的方式只是参考)
     *
     * @param runProcess
     * @return
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            // 向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream(); // 拿到输出流
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            // 往控制台写数据（这种拼接字符串的方式不够优雅）
            String[] inputStr = args.split(" ");
            String join = StrUtil.join("\n", inputStr) + "\n";
            outputStreamWriter.write(join);
            // 作用相当于回车，执行输入的发送
            outputStreamWriter.flush();
            // 获取控制台的输出并输出(分批获取)
            InputStream inputStream = runProcess.getInputStream(); // 拿到输入流
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            // 关闭输入输出流(资源的释放)
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy(); // 销毁进程
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

}
