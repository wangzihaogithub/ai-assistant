package com.github.aiassistant.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {

    public static String md5DigestAsHex(byte[] input) {
        try {
            // 获取MD5 MessageDigest实例
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 计算哈希值
            byte[] hashBytes = md.digest(input);

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                // 将每个字节转换为两位十六进制数
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    // 如果只有一位，则前面补0
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5算法应该总是可用的，所以这个异常通常不会抛出
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

}
