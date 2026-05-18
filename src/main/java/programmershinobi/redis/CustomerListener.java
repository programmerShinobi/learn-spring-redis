package programmershinobi.redis;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomerListener implements MessageListener {
    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {
        log.info("Receive message : {}", new String(message.getBody()));
    }
}
