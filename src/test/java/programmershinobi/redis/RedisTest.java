package programmershinobi.redis;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.data.geo.*;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.support.collections.DefaultRedisMap;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisSet;
import org.springframework.data.redis.support.collections.RedisZSet;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
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

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

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

    @Test
    void geo() {
        GeoOperations<String, String> operations = redisTemplate.opsForGeo();

        operations.add("sellers", new Point(106.822664, -6.176902), "Toko A");
        operations.add("sellers", new Point(106.820604, -6.175270), "Toko B");

        Distance distance = operations.distance("sellers", "Toko A", "Toko B", Metrics.KILOMETERS);
        assertEquals(0.2913, distance.getValue());

        GeoResults<RedisGeoCommands.GeoLocation<String>> sellers = operations.search("sellers", new Circle(
                new Point(106.820893, -6.172859),
                new Distance(5, Metrics.KILOMETERS)
        ));

        assertEquals(2, sellers.getContent().size());
        assertEquals("Toko A", sellers.getContent().get(0).getContent().getName());
        assertEquals("Toko B", sellers.getContent().get(1).getContent().getName());

    }

    @Test
    void hyperLogLog() {
        HyperLogLogOperations<String, String> operations = redisTemplate.opsForHyperLogLog();

        operations.add("traffics", "faqih", "pratama", "muhti");
        operations.add("traffics", "faqih", "fadli", "firly");
        operations.add("traffics", "firly", "fitrya");

        assertEquals(6L, operations.size("traffics"));
    }

    @Test
    void transaction() {
        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();

                operations.opsForValue().set("test1", "Faqih", Duration.ofSeconds(2));
                operations.opsForValue().set("test2", "Fadli", Duration.ofSeconds(2));

                operations.exec();
                return null;
            }
        });

        assertEquals("Faqih", redisTemplate.opsForValue().get("test1"));
        assertEquals("Fadli", redisTemplate.opsForValue().get("test2"));

    }

    @Test
    void pipeline() {
        List<Object> statuses = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.opsForValue().set("test1", "Faqih", Duration.ofSeconds(2));
                operations.opsForValue().set("test2", "Fadli", Duration.ofSeconds(2));
                operations.opsForValue().set("test3", "Firly", Duration.ofSeconds(2));
                operations.opsForValue().set("test4", "Fitrya", Duration.ofSeconds(2));
                return null;
            }
        });

        assertThat(statuses, hasSize(4));
        assertThat(statuses, hasItems(true));
        assertThat(statuses, not(hasItems(false)));
    }

    @Test
    void publishStream() {
        StreamOperations<String, Object, Object> operations = redisTemplate.opsForStream();

        MapRecord<String, String, String> record = MapRecord.create("stream-1", Map.of(
                "name", "Muhti",
                "address", "Indonesia"
        ));

        for (int i = 0; i < 10; i++) {
            operations.add(record);
        }
    }

    @Test
    void subscribeStream() {
        StreamOperations<String, Object, Object> operations = redisTemplate.opsForStream();

        try {
            operations.createGroup("stream-1", "sample-group");
        } catch (RedisSystemException exception) {
            // already exist
        }

        List<@NonNull MapRecord<String, Object, Object>> records = operations.read(Consumer.from("sample-group", "sample-1"),
                StreamOffset.create("stream-1", ReadOffset.lastConsumed()));

        for (MapRecord<String, Object, Object> record : records) {
            System.out.println(record);
        }
    }

    @Test
    void pubSub() {
        redisTemplate.getConnectionFactory().getConnection().subscribe(new MessageListener() {
            @Override
            public void onMessage(Message message, byte @Nullable [] pattern) {
                String event = new String(message.getBody());
                System.out.println("Receive message : " + event);
            }
        }, "my-channel".getBytes());

        for (int i = 0; i < 10; i++) {
            redisTemplate.convertAndSend("my-channel", "Hello World : " + i);
        }
    }

    @Test
    void redisList() {
        RedisList<String> list = RedisList.create("names", redisTemplate);
        list.add("Faqih");
        list.add("Pratama");
        list.add("Muhti");
        assertThat(list, hasItems("Faqih", "Pratama", "Muhti"));

        List<String> names = redisTemplate.opsForList().range("names", 0, -1);
        assertThat(names, hasItems("Faqih", "Pratama", "Muhti"));
    }

    @Test
    void redisSet() {
        RedisSet<String> set = RedisSet.create("traffic", redisTemplate);
        set.addAll(Set.of("Right", "Left", "Up", "Down"));
        set.addAll(Set.of("Left", "Down"));
        set.addAll(Set.of("Right", "Up"));
        assertThat(set, hasItems("Right", "Left", "Up", "Down"));

        Set<@NonNull String> members = redisTemplate.opsForSet().members("traffic");
        assertThat(members, hasItems("Right", "Left", "Up", "Down"));
    }

    @Test
    void redisZSet() {
        RedisZSet<String> set = RedisZSet.create("winner", redisTemplate);
        set.add("Faqih", 100);
        set.add("Fadli", 85);
        set.add("Firly", 95);
        set.add("Fitrya", 80);
        assertThat(set, hasItems("Faqih", "Fadli", "Firly", "Fitrya"));

        Set<String> winner = redisTemplate.opsForZSet().range("winner", 0, -1);
        assertThat(winner, hasItems("Faqih", "Fadli", "Firly", "Fitrya"));

        assertEquals("Faqih", set.popLast());
        assertEquals("Firly", set.popLast());
        assertEquals("Fadli", set.popLast());
        assertEquals("Fitrya", set.popLast());
    }

    @Test
    void redisMap() {
        Map<String, String> map = new DefaultRedisMap<>("user:1", redisTemplate);
        map.put("name", "Faqih");
        map.put("address", "Indonesia");
        assertThat(map, hasEntry("name", "Faqih"));
        assertThat(map, hasEntry("address", "Indonesia"));

        Map<Object, Object> entries = redisTemplate.opsForHash().entries("user:1");
        assertThat(entries, hasEntry("name", "Faqih"));
        assertThat(entries, hasEntry("address", "Indonesia"));
    }

    @Test
    void repository() {
        Product product = Product.builder()
                .id("1")
                .name("Nasi Goreng Udang")
                .price(25_000L)
                .ttl(1L)
                .build();

        productRepository.save(product);

        Product product2 = productRepository.findById("1").get();
        assertEquals(product, product2);

        Map<@NonNull Object, Object> map = redisTemplate.opsForHash().entries("products:1");
        assertEquals(product.getId(), map.get("id"));
        assertEquals(product.getName(), map.get("name"));
        assertEquals(product.getPrice().toString(), map.get("price"));
    }

    @Test
    void ttl() throws InterruptedException {
        Product product = Product.builder()
                .id("2")
                .name("Pizza")
                .price(50_000L)
                .ttl(3L)
                .build();
        productRepository.save(product);

        assertTrue(productRepository.findById("2").isPresent());

        Thread.sleep(Duration.ofSeconds(5));
        assertFalse(productRepository.findById("2").isPresent());
    }

    @Test
    void cache() {
        Cache cache = cacheManager.getCache("scores");
        cache.put("Faqih", 100);
        cache.put("Fadli", 85);

        assertEquals(100, cache.get("Faqih", Integer.class));
        assertEquals(85, cache.get("Fadli", Integer.class));

        cache.evict("Faqih");
        cache.evict("Fadli");

        assertNull(cache.get("Faqih"));
        assertNull(cache.get("Fadli"));
    }

    @Test
    void cacheable() {
        Product product = productService.getProduct("001");
        assertEquals("001", product.getId());

        Product product2 = productService.getProduct("001");
        assertEquals(product, product2);

        Product product3 = productService.getProduct("002");
        assertEquals(product, product2);
    }

    @Test
    void cachePut() {
        Product product = Product.builder()
                .id("P002")
                .name("Banana")
                .price(100L)
                .build();
        productService.saveProduct(product);

        Product product2 = productService.getProduct("P002");
        assertEquals(product, product2);
    }

    @Test
    void cacheEvict() {
        Product product = productService.getProduct("003");
        assertEquals("003", product.getId());


        productService.removeProduct("003");

        Product product2 = productService.getProduct("003");
        assertEquals(product, product2);
    }
}
