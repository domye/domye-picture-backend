package com.domye.picture.helper;

import org.springframework.data.redis.core.ZSetOperations;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface Cache<T> {


    T get(Object key);

    String getString(Object key);

    /**
     * multiGet 批量获取
     */
    List multiGet(Collection keys);

    /**
     * 批量set
     */
    void multiSet(Map map);

    /**
     * 批量删除
     */
    void multiDel(Collection keys);

    void put(Object key, T value);

    /**
     * 往缓存中写入内容
     * @param key   缓存key
     * @param value 缓存value
     * @param exp   有效时间，单位为秒
     */
    void put(Object key, T value, Long exp);

    /**
     * 往缓存中写入内容
     * @param key      缓存key
     * @param value    缓存value
     * @param exp      过期时间
     * @param timeUnit 过期单位
     */
    void put(Object key, T value, Long exp, TimeUnit timeUnit);

    /**
     * 删除 key
     */
    Boolean remove(Object key);

    /**
     * 删除 模糊删除key
     * @param key 缓存key
     */
    void vagueDel(Object key);

    /**
     * Clear the cache 删除所有 key*
     */
    void clear();

    /**
     * 往缓存中写入内容
     * @param key       缓存key
     * @param hashKey   缓存中hashKey
     * @param hashValue hash值
     */
    void putHash(Object key, Object hashKey, Object hashValue);

    /**
     * 往缓存中写入内容
     * @param key 缓存key
     * @param map map value
     */
    void putAllHash(Object key, Map map);

    void expireKey(String key, long time, TimeUnit timeUnit);

    /**
     * 读取缓存值
     * @param key     缓存key
     * @param hashKey map value
     * @return 返回缓存中的数据
     */
    T getHash(Object key, Object hashKey);

    /**
     * 读取缓存值
     * @param key 缓存key
     * @return 缓存中的数据
     */
    Map<Object, Object> getHash(Object key);

    /**
     * 是否包含
     * @param key 缓存key
     * @return true/false
     */
    boolean hasKey(Object key);

    /**
     * 模糊匹配key
     * @param pattern 模糊key
     * @return 缓存中的数据
     */
    List<String> keys(String pattern);

    /**
     * 模糊查询
     * @param pattern
     * @return
     */
    Set<String> fuzzySearchKeys(String pattern);


    //-----------------------------------------------用于特殊场景，redis去重计数---------------------------------------------

    /**
     * 累计数目
     * 效率较高的 计数器
     * 如需清零，按照普通key 移除即可
     * @param key   key值
     * @param value 去重统计值
     * @return 计数器结果
     */
    Long cumulative(Object key, Object value);

    /**
     * 计数器结果
     * <p>
     * 效率较高的 计数器 统计返回
     * 如需清零，按照普通key 移除即可
     * @param key 计数器key
     * @return 计数器结果
     */
    Long counter(Object key);

    /**
     * 批量计数
     * @param keys 要查询的key集合
     * @return 批量计数
     */
    List multiCounter(Collection keys);

    /**
     * 计数器结果
     * <p>
     * 效率较高的 计数器 统计返回
     * 如需清零，按照普通key 移除即可
     * @param key key值
     * @return 计数器结果
     */
    Long mergeCounter(Object... key);

    //---------------------------------------------------用于特殊场景，redis去重统计-----------------------------------------

    //-----------------------------------------------redis计数---------------------------------------------

    /**
     * redis 计数器 累加
     * 注：到达liveTime之后，该次增加取消，即自动-1，而不是redis值为空
     * @param key      为累计的key，同一key每次调用则值 +1
     * @param liveTime 单位秒后失效
     * @return 计数器结果 （自增前的结果）
     */
    Long incr(String key, long liveTime);

    /**
     * redis 计数器 累加
     * 注：到达liveTime之后，该次增加取消，即自动-1，而不是redis值为空
     * @param key 为累计的key，同一key每次调用则值 +1
     * @return 计数器结果
     */
    Long incr(String key);

    //-----------------------------------------------redis计数---------------------------------------------

    /**
     * 使用Sorted Set记录keyword
     * zincrby命令，对于一个Sorted Set，存在的就把分数加x(x可自行设定)，不存在就创建一个分数为1的成员
     * @param sortedSetName sortedSetName的Sorted Set不用预先创建，不存在会自动创建，存在则向里添加数据
     * @param keyword       关键词
     */
    void incrementScore(String sortedSetName, String keyword);

    /**
     * 使用Sorted Set记录keyword
     * zincrby命令，对于一个Sorted Set，存在的就把分数加x(x可自行设定)，不存在就创建一个分数为1的成员
     * @param sortedSetName sortedSetName的Sorted Set不用预先创建，不存在会自动创建，存在则向里添加数据
     * @param keyword       关键词
     * @param score         分数
     * @return
     */
    Double incrementScore(String sortedSetName, String keyword, Integer score);

    /**
     * zrevrange命令, 查询Sorted Set中指定范围的值
     * 返回的有序集合中，score大的在前面
     * zrevrange方法无需担心用于指定范围的start和end出现越界报错问题
     * @param sortedSetName sortedSetName
     * @param start         查询范围开始位置
     * @param end           查询范围结束位置
     * @return 获取满足条件的集合
     */
    Set<ZSetOperations.TypedTuple<Object>> reverseRangeWithScores(String sortedSetName, Integer start, Integer end);

    /**
     * zrevrange命令, 查询Sorted Set中指定范围的值
     * 返回的有序集合中，score大的在前面
     * zrevrange方法无需担心用于指定范围的start和end出现越界报错问题
     * @param sortedSetName sortedSetName
     * @param count         查询数量
     * @return 获取满足条件的集合
     */
    Set<ZSetOperations.TypedTuple<Object>> reverseRangeWithScores(String sortedSetName, Integer count);

    /**
     * 向Zset里添加成员
     * @param key   key值
     * @param score 分数
     * @param value 值
     * @return 增加状态
     */
    boolean zAdd(String key, long score, String value);

    /**
     * 获取 某key 下 某一分值区间的队列
     * @param key  缓存key
     * @param from 开始时间
     * @param to   结束时间
     * @return 数据
     */
    Set<ZSetOperations.TypedTuple<Object>> zRangeByScore(String key, int from, long to);

    /**
     * 移除 Zset队列值
     * @param key   key值
     * @param value 删除的集合
     * @return 删除数量
     */
    Long zRemove(String key, String... value);

    /**
     * redis 加锁
     * @param key
     * @param lockTimeout 超时过期时间
     * @param checkCount  尝试次数
     * @return 成功or失败
     */
    Boolean lock(String key, Long lockTimeout, Integer checkCount);

    //-----------------------------------------------用于Set操作---------------------------------------------

    /**
     * 向Set集合中添加元素
     * @param key    Set的键
     * @param values 要添加的元素
     * @return 添加成功的元素数量（不包括已存在的元素）
     */
    Long sAdd(String key, Object... values);

    /**
     * 获取Set集合中的所有元素
     * @param key Set的键
     * @return Set集合中的所有元素
     */
    Set<Object> sMembers(String key);

    /**
     * 判断元素是否存在于Set集合中
     * @param key   Set的键
     * @param value 要判断的元素
     * @return 存在返回true，不存在返回false
     */
    Boolean sIsMember(String key, Object value);

    /**
     * 获取Set集合的大小
     * @param key Set的键
     * @return Set集合的大小
     */
    Long sSize(String key);

    /**
     * 从Set集合中移除指定元素
     * @param key    Set的键
     * @param values 要移除的元素
     * @return 实际移除的元素数量
     */
    Long sRemove(String key, Object... values);

    /**
     * 获取多个Set集合的交集
     * @param keys 多个Set的键
     * @return 交集结果
     */
    Set<Object> sIntersect(String... keys);

    /**
     * 获取多个Set集合的并集
     * @param keys 多个Set的键
     * @return 并集结果
     */
    Set<Object> sUnion(String... keys);

    /**
     * 获取Set集合的差集（第一个Set与其他Set的差集）
     * @param key       第一个Set的键
     * @param otherKeys 其他Set的键
     * @return 差集结果
     */
    Set<Object> sDifference(String key, String... otherKeys);


}
