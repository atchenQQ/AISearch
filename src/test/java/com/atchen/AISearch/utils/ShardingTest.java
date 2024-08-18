package com.atchen.AISearch.utils;
import com.atchen.AISearch.entity.Answer;
import com.atchen.AISearch.service.IAnswerService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-17
 * @Description: 分库测试类
 * @Version: 1.0
 */

@SpringBootTest
public class ShardingTest {

    @Resource
    private IAnswerService answerService;

    @Test
    public void insertTest(){
        for (long i = 1; i < 5; i++) {
            Answer answer = new Answer();
            answer.setTitle("test");
            answer.setContent("test");
            answer.setType(1);
            answer.setModel(1);
            answer.setUid(i);
            answerService.save(answer);
        }
    }

    @Test
    public void selectTest(){
        System.out.println(answerService.getById(3L));
        System.out.println("-------------------------------");
        System.out.println(answerService.getById(1023346740801044480L));
    }

    @Test
    public void deleteTest(){
        QueryWrapper<Answer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type",1);
        List<Answer> list = answerService.list(queryWrapper);
        for (Answer answer : list) {
            System.out.println(answer.getContent());
        }
    }

}



    