package programmershinobi.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.springframework.test.web.servlet.MockMvcBuilder.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matcher.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class StringTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void name() {
        assertNotNull(redisTemplate);
    }

    @Test
    void string() throws InterruptedException {
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        operations.set("name", "Faqih", Duration.ofSeconds(2));
        assertEquals("Faqih", operations.get("name"));

        Thread.sleep(Duration.ofSeconds(3));
        assertNull(operations.get("name"));
    }
}
