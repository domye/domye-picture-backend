package com.domye.picture.service.helper.comment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析评论内容中的@用户名
 */
public class MentionParser {
    
    // 匹配@后跟中文、字母、数字、下划线
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w\\u4e00-\\u9fa5]+)");
    
    /**
     * 提取@用户名列表
     * @param content 评论内容
     * @return 用户名列表（不包含@符号）
     */
    public static List<String> extractUserNames(String content) {
        List<String> userNames = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return userNames;
        }
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            userNames.add(matcher.group(1));
        }
        return userNames;
    }
    
    /**
     * 从内容中移除@自己的部分
     * @param content 评论内容
     * @param currentUserName 当前用户名
     * @return 移除@自己后的内容
     */
    public static String removeSelfMention(String content, String currentUserName) {
        if (content == null || currentUserName == null) {
            return content;
        }
        String pattern = "@" + Pattern.quote(currentUserName);
        return content.replaceAll(pattern, "").trim();
    }
    
    /**
     * 测试方法 - 验证正则逻辑
     */
    public static void main(String[] args) {
        System.out.println("=== MentionParser 测试用例 ===\n");
        
        // 测试用例 1: 多个@用户名
        testCase1();
        
        // 测试用例 2: 连续@符号
        testCase2();
        
        // 测试用例 3: 包含下划线和数字的用户名
        testCase3();
        
        // 测试用例 4: @后有空格（不匹配）
        testCase4();
        
        // 测试用例 5: removeSelfMention 测试
        testCase5();
        
        // 测试用例 6: 空值和 null 测试
        testCase6();
        
        System.out.println("\n=== 所有测试完成 ===");
    }
    
    private static void testCase1() {
        String input = "@张三 你好 @李四 看看这个";
        List<String> result = extractUserNames(input);
        System.out.println("测试 1: 多个@用户名");
        System.out.println("  输入: \"" + input + "\"");
        System.out.println("  期望: [张三，李四]");
        System.out.println("  实际: " + result);
        System.out.println("  结果: " + (result.equals(List.of("张三", "李四")) ? "✓ 通过" : "✗ 失败"));
    }
    
    private static void testCase2() {
        String input = "@@@张三";
        List<String> result = extractUserNames(input);
        System.out.println("\n测试 2: 连续@符号");
        System.out.println("  输入: \"" + input + "\"");
        System.out.println("  期望: [张三]");
        System.out.println("  实际: " + result);
        System.out.println("  结果: " + (result.equals(List.of("张三")) ? "✓ 通过" : "✗ 失败"));
    }
    
    private static void testCase3() {
        String input = "@user_123";
        List<String> result = extractUserNames(input);
        System.out.println("\n测试 3: 包含下划线和数字的用户名");
        System.out.println("  输入: \"" + input + "\"");
        System.out.println("  期望: [user_123]");
        System.out.println("  实际: " + result);
        System.out.println("  结果: " + (result.equals(List.of("user_123")) ? "✓ 通过" : "✗ 失败"));
    }
    
    private static void testCase4() {
        String input = "@ 张三";
        List<String> result = extractUserNames(input);
        System.out.println("\n测试 4: @后有空格（不匹配）");
        System.out.println("  输入: \"" + input + "\"");
        System.out.println("  期望: []");
        System.out.println("  实际: " + result);
        System.out.println("  结果: " + (result.isEmpty() ? "✓ 通过" : "✗ 失败"));
    }
    
    private static void testCase5() {
        String input = "@张三 你好 @李四 看看这个";
        String currentUser = "张三";
        String result = removeSelfMention(input, currentUser);
        System.out.println("\n测试 5: removeSelfMention 测试");
        System.out.println("  输入: \"" + input + "\"");
        System.out.println("  当前用户: \"" + currentUser + "\"");
        System.out.println("  期望: \"你好 @李四 看看这个\"");
        System.out.println("  实际: \"" + result + "\"");
        System.out.println("  结果: " + (result.equals("你好 @李四 看看这个") ? "✓ 通过" : "✗ 失败"));
    }
    
    private static void testCase6() {
        List<String> result1 = extractUserNames(null);
        List<String> result2 = extractUserNames("");
        List<String> result3 = extractUserNames("   ");
        String result4 = removeSelfMention(null, "张三");
        String result5 = removeSelfMention("@张三", null);
        
        System.out.println("\n测试 6: 空值和 null 测试");
        System.out.println("  extractUserNames(null): " + result1 + " - " + (result1.isEmpty() ? "✓" : "✗"));
        System.out.println("  extractUserNames(\"\"): " + result2 + " - " + (result2.isEmpty() ? "✓" : "✗"));
        System.out.println("  extractUserNames(\"   \"): " + result3 + " - " + (result3.isEmpty() ? "✓" : "✗"));
        System.out.println("  removeSelfMention(null, ...): " + result4 + " - " + (result4 == null ? "✓" : "✗"));
        System.out.println("  removeSelfMention(..., null): " + result5 + " - " + (result5 == null ? "✓" : "✗"));
    }
}
