package com.zhangke.zlog;

import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;


class LogDispatcher extends Thread {

    private static final String TAG = "LogDispatcher";
    //每次只查看2M日志数据,超过50K则清除一次数据
    private final int MAX_LOG_SIZE = 2024 * 1000;

    /**
     * 存储日志的队列
     */
    private LinkedBlockingQueue<LogBean> mLogQueue;
    private String mLogDir;


    LogDispatcher(LinkedBlockingQueue<LogBean> logQueue, String logDir) {
        this.mLogQueue = logQueue;
        this.mLogDir = logDir;
    }

    @Override
    public void run() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            while (true) {
                try {
                    LogBean logBean;
                    logBean = mLogQueue.take();
                    saveTextToFile(getLogFilePath(logBean.getLogType()), logBean.getLogText());
                } catch (InterruptedException e) {
                    Log.e(TAG, "run: ", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "run: ", e);
        }
    }

    /**
     * 普通日志文件名： log1.txt；
     * 错误日志文件名：errorLog1.txt;
     * 每个日志文件最大为 1Mb，超过 1Mb 文件名累加 1.
     *
     * @param logType 日志类型
     * @return 文件绝对路径
     */
    private String getLogFilePath(LogType logType) {
        String returnFileName = "";
        try {
            switch (logType) {
                case ERROR: {
                    returnFileName = getLastLogFileName(mLogDir, "errorLog");
                    break;
                }
                case INFO:
                case WTF:
                case DEBUG: {
                    returnFileName = getLastLogFileName(mLogDir, "log");
                    break;
                }
                case CRASH: {
                    returnFileName = getLastLogFileName(mLogDir, "crash");
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getLogFile: ", e);
        }
        return returnFileName;
    }

    /**
     * 根据目录路径及文件前缀获取最后一个该日志文件；
     * 比如当前文件夹下有log1.txt, log2.txt, log3.txt,则返回 log3.txt;
     * 当文件个数为9，且最后一个文件大于1M，则清除Log文件
     *
     * @param dir     文件路径
     * @param logName 文件名开头，如 log
     * @return 文件名
     */
    private String getLastLogFileName(String dir, String logName) {
        String returnFileName = String.format("%s/%s.txt", dir, logName);
        File file = new File(dir);
        if (file.exists()) {
            File fileNow = new File(returnFileName);
            if(fileNow.exists() && fileNow.length()>MAX_LOG_SIZE){
                fileNow.delete();
            }
        }
        return returnFileName;
    }

    /**
     * 将文本追加到到文件末尾;
     * 文件不存在会创建文件；
     * 父目录不存在会创建父目录，
     * 会判断三级以内的目录是否存在，不存在则创建
     *
     * @param filePath 文件绝对路径（包含文件名）
     * @param text     需要保存的文本
     */
    private void saveTextToFile(String filePath, String text) {
        try {
            File file = new File(filePath);
            if (!new File(file.getParent()).exists()) {
                File parentFile1 = new File(file.getParent());
                if (!parentFile1.exists()) {
                    File parentFile2 = new File(parentFile1.getParent());
                    if (!parentFile2.exists()) {
                        parentFile2.mkdir();
                    }
                    parentFile1.mkdir();
                }
                new File(file.getParent()).mkdir();
            }
            if(file.isDirectory()){
                file.delete();
                file.createNewFile();
            }
            else {
                if (!file.exists()) {
                    file.createNewFile();
                }
            }
            FileWriter writer = new FileWriter(file, true);
            writer.write(text);
            writer.close();
        } catch (Exception e) {
            Log.e(TAG, "saveTextToFile: ", e);
        }
    }
}
