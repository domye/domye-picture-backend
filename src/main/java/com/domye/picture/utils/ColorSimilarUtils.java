package com.domye.picture.utils;

import java.awt.*;

public class ColorSimilarUtils {

    private ColorSimilarUtils() {
        // 工具类不需要实例化
    }

    /**
     * 计算两个颜色的相似度
     * @param color1 第一个颜色
     * @param color2 第二个颜色
     * @return 相似度（0到1之间，1为完全相同）
     */
    public static double calculateSimilarity(Color color1, Color color2) {
        int r1 = color1.getRed();
        int g1 = color1.getGreen();
        int b1 = color1.getBlue();

        int r2 = color2.getRed();
        int g2 = color2.getGreen();
        int b2 = color2.getBlue();

        // 计算欧氏距离
        double distance = Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));

        // 计算相似度
        return 1 - distance / Math.sqrt(3 * Math.pow(255, 2));
    }

    //拓展短格式十六进制颜色代码
    public static String expandShortHex(String shortHex) {
        if (shortHex == null || shortHex.length() < 2) {
            throw new IllegalArgumentException("Invalid hex color format: " + shortHex);
        }

        // 检查格式是否为 0xABC 或 #ABC
        boolean is0xFormat = shortHex.startsWith("0x") || shortHex.startsWith("0X");

        String prefix = is0xFormat ? shortHex.substring(0, 2) : "#";
        String hexDigits = shortHex.substring(is0xFormat ? 2 : 1);


        // 如果是 3 位短格式，扩展为 6 位
        if (hexDigits.length() == 3) {
            StringBuilder fullHex = new StringBuilder(prefix);
            for (char c : hexDigits.toCharArray()) {
                fullHex.append(c).append(c);
            }
            return fullHex.toString();
        }

        return shortHex;

    }

    /**
     * 根据十六进制颜色代码计算相似度
     * @param hexColor1 第一个颜色的十六进制代码（如 0xFF0000）
     * @param hexColor2 第二个颜色的十六进制代码（如 0xFE0101）
     * @return 相似度（0到1之间，1为完全相同）
     */
    public static double calculateSimilarity(String hexColor1, String hexColor2) {
        Color color1 = Color.decode(expandShortHex(hexColor1));
        Color color2 = Color.decode(expandShortHex(hexColor2));
        return calculateSimilarity(color1, color2);
    }
    
}