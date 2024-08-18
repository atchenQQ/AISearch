package com.atchen.AISearch.utils;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.boot.test.context.SpringBootTest;
/**
 * @Author: atchen
 * @CreateTime: 2024-08-17
 * @Description: 本地大模型调用
 * @Version: 1.0
 */
@SpringBootTest
public class OllamaTest {
    @Resource
    private OllamaChatClient ollamaChatClient;
    @Test
    public void test() {
        String input = "你好";
        String output = ollamaChatClient.call(input);
        System.out.println(output);
    }
}
    