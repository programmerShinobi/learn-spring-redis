package programmershinobi.redis;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Set;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.MockMvcBuilder.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matcher.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class RedisTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void redisTemplate() {
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

    @Test
    void list() {
        ListOperations<String, String> operations = redisTemplate.opsForList();

        operations.rightPush("names", "Faqih");
        operations.rightPush("names", "Pratama");
        operations.rightPush("names", "Muhti");

        assertEquals("Faqih", operations.leftPop("names"));
        assertEquals("Pratama", operations.leftPop("names"));
        assertEquals("Muhti", operations.leftPop("names"));

    }

    @Test
    void set() {
        SetOperations<String, String> operations = redisTemplate.opsForSet();

        operations.add("students", "Faqih");
        operations.add("students", "Faqih");
        operations.add("students", "Pratama");
        operations.add("students", "Pratama");
        operations.add("students", "Muhti");
        operations.add("students", "Muhti");
        operations.add("students", "Muhti");

        Set<String> students = operations.members("students");
        assertEquals(3, students.size());
        assertThat(students, hasItems("Faqih", "Pratama", "Muhti"));
    }

    @Test
    void zSet() {
        ZSetOperations<String, String> operations = redisTemplate.opsForZSet();

        operations.add("score", "Faqih", 100);
        operations.add("score", "Fadli", 85);
        operations.add("score", "Firly", 95 );
        operations.add("score", "Fitrya", 90);

        assertEquals("Faqih", operations.popMax("score").getValue());
        assertEquals("Firly", operations.popMax("score").getValue());
        assertEquals("Fitrya", operations.popMax("score").getValue());
        assertEquals("Fadli", operations.popMax("score").getValue());

    }

    @Test
    void hash() {
        HashOperations<String, Object, Object> operations = redisTemplate.opsForHash();

//        operations.put("user:1", "id", "1");
//        operations.put("user:1", "name", "Shinobi");
//        operations.put("user:1", "email", "shinobi@example.com");

        HashMap<Object, Object> map = new HashMap<>();
        map.put("id", "1");
        map.put("name", "Shinobi");
        map.put("email", "shinobi@example.com");

        operations.putAll("user:1", map);

        assertEquals("1", operations.get("user:1", "id"));
        assertEquals("Shinobi", operations.get("user:1", "name"));
        assertEquals("shinobi@example.com", operations.get("user:1", "email"));

        redisTemplate.delete("user:1");
    }
}
