package com.domye.picture.common.mdc;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Slf4j
public class SelfTraceIdGenerator {
    private final static Integer MIN_AUTO_NUMBER = 1000;
    private final static Integer MAX_AUTO_NUMBER = 10000;
    private static volatile Integer autoIncreaseNumber = MIN_AUTO_NUMBER;
    private static String cachedIpHex = null;

    /**
     * <p>
     * 生成 traceId，规则是 服务器 IP + 产生ID时的时间 + 当前进程号 + 自增序列
     * IP 8位：39.105.208.175 -> 2769d0af (16进制)
     * 时间戳 13位： 毫秒时间戳
     * 进程号 5位： PID
     * 自增序列 4位： 1000-9999循环
     * </p>
     *
     * @return ac13e001.1685348263825.095001000
     */
    public static String generate() {
        StringBuilder traceId = new StringBuilder();
        try {
            // 1. IP地址 - 8位十六进制
            traceId.append(getIpHex()).append(".");
            // 2. 时间戳 - 13位
            traceId.append(Instant.now().toEpochMilli()).append(".");
            // 3. 当前进程号 - 5位
            traceId.append(getProcessId());
            // 4. 自增序列 - 4位
            traceId.append(getAutoIncreaseNumber());
        } catch (Exception e) {
            log.error("generate trace id error!", e);
            return UUID.randomUUID().toString().replaceAll("-", "");
        }
        return traceId.toString();
    }

    /**
     * 获取本机IP地址的十六进制表示
     * 缓存结果以提高性能
     *
     * @return 8位十六进制IP地址
     */
    private static String getIpHex() {
        if (cachedIpHex != null) {
            return cachedIpHex;
        }
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            byte[] ipBytes = inetAddress.getAddress();
            StringBuilder sb = new StringBuilder();
            for (byte b : ipBytes) {
                // 将byte转换为无符号整数，再转为16进制
                sb.append(String.format("%02x", b & 0xff));
            }
            cachedIpHex = sb.toString();
            return cachedIpHex;
        } catch (Exception e) {
            log.warn("Failed to get local IP address, using default", e);
            cachedIpHex = "00000000";
            return cachedIpHex;
        }
    }

    /**
     * 使得自增序列在1000-9999之间循环  - 4位
     *
     * @return 自增序列号
     */
    private static int getAutoIncreaseNumber() {
        synchronized (SelfTraceIdGenerator.class) {
            if (autoIncreaseNumber >= MAX_AUTO_NUMBER) {
                autoIncreaseNumber = MIN_AUTO_NUMBER;
                return autoIncreaseNumber;
            } else {
                return autoIncreaseNumber++;
            }
        }
    }

    /**
     * @return 5位当前进程号
     */
    private static String getProcessId() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String processId = runtime.getName().split("@")[0];
        try {
            return String.format("%05d", Integer.parseInt(processId));
        } catch (NumberFormatException e) {
            // 如果进程号超过5位，取后5位
            if (processId.length() > 5) {
                return processId.substring(processId.length() - 5);
            }
            return String.format("%05d", 0);
        }
    }

    public static void main(String[] args) {
        String t = generate();
        System.out.println(t);
        String t2 = generate();
        System.out.println(t2);
    }
}
