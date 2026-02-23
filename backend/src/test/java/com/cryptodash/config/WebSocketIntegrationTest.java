package com.cryptodash.config;

import com.cryptodash.dto.PriceTickDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired
    private org.springframework.core.env.Environment environment;

    @Test
    void stompSubscribeToPricesReceivesMessage() throws Exception {
        int port = Integer.parseInt(environment.getProperty("local.server.port", "8080"));
        String url = "http://localhost:" + port + "/ws";

        List<org.springframework.web.socket.sockjs.client.Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        BlockingQueue<PriceTickDto> received = new ArrayBlockingQueue<>(2);
        StompSession session = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/prices", new StompFrameHandler() {
            @Override
            public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
                return PriceTickDto.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
                if (payload instanceof PriceTickDto dto) {
                    received.add(dto);
                }
            }
        });

        PriceTickDto sent = new PriceTickDto(
                "BTCUSDT",
                new BigDecimal("50000.00"),
                new BigDecimal("1.5"),
                new BigDecimal("51000"),
                new BigDecimal("49000"),
                new BigDecimal("1000000"),
                System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/prices", sent);

        PriceTickDto receivedDto = received.poll(3, TimeUnit.SECONDS);
        assertThat(receivedDto).isNotNull();
        assertThat(receivedDto.symbol()).isEqualTo("BTCUSDT");
        assertThat(receivedDto.lastPrice()).hasToString("50000.00");

        session.disconnect();
    }
}
