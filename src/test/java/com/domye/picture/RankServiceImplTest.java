package com.domye.picture;

import cn.hutool.core.lang.Console;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.domye.picture.service.rank.RankService;
import com.domye.picture.service.rank.model.dto.UserActivityScoreAddRequest;
import com.domye.picture.service.rank.model.vo.UserActiveRankItemVO;
import com.domye.picture.service.user.model.entity.User;
import com.domye.picture.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class RankServiceImplTest {

    @Resource
    private RankService rankService;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    public void setUp() {
        // 清理 Redis 中的测试数据
        stringRedisTemplate.getConnectionFactory().getConnection().flushDb();

        // 清理用户表中的数据
        userService.remove(new QueryWrapper<>());
    }

    // 创建测试用户的工厂方法，不再设置ID
    private User createTestUser(String userName) {
        User user = new User();
        user.setUserAccount("testAccount_" + System.currentTimeMillis());
        user.setUserPassword("testPassword_" + System.currentTimeMillis());
        user.setUserName(userName);
        user.setUserRole("user");
        return user;
    }

    @Test
    public void testAddActivityScoreWithPath() {
        // 使用工厂方法创建测试用户，不设置ID
        User user = createTestUser("testUser");
        userService.save(user);

        // 准备请求参数
        UserActivityScoreAddRequest request = new UserActivityScoreAddRequest();
        request.setPath("/test/path");

        // 执行测试
        rankService.addActivityScore(user, request);

        // 验证 Redis 中的数据
        String todayRankKey = "activity_rank_" + cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyyMMdd");
        Double score = stringRedisTemplate.opsForZSet().score(todayRankKey, user.getId().toString());
        assertEquals(1.0, score, 0.01);
    }

    @Test
    public void testQueryDayRankList() {
        // 使用工厂方法创建测试用户，不设置ID
        User user1 = createTestUser("user1");
        userService.save(user1);

        User user2 = createTestUser("user2");
        userService.save(user2);

        User user3 = createTestUser("user3");
        userService.save(user3);

        // 准备排行榜数据
        String todayRankKey = "activity_rank_" + cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyyMMdd");
        stringRedisTemplate.opsForZSet().add(todayRankKey, user1.getId().toString(), 10.0);
        stringRedisTemplate.opsForZSet().add(todayRankKey, user2.getId().toString(), 20.0);
        stringRedisTemplate.opsForZSet().add(todayRankKey, user3.getId().toString(), 15.0);

        // 执行查询日排行榜
        List<UserActiveRankItemVO> rankList = rankService.queryRankList(1, 10);
        Console.log(rankList);
        // 验证结果
        assertEquals(3, rankList.size());
        assertEquals(1, rankList.get(0).getRank().intValue()); // 第一名
        assertEquals(user2.getId(), rankList.get(0).getUser().getId()); // 用户ID
        assertEquals(20.0, rankList.get(0).getScore(), 0.01); // 分数

        assertEquals(2, rankList.get(1).getRank().intValue());
        assertEquals(user3.getId(), rankList.get(1).getUser().getId());
        assertEquals(15.0, rankList.get(1).getScore(), 0.01);

        assertEquals(3, rankList.get(2).getRank().intValue());
        assertEquals(user1.getId(), rankList.get(2).getUser().getId());
        assertEquals(10.0, rankList.get(2).getScore(), 0.01);
    }

    // 其他测试方法...
}
