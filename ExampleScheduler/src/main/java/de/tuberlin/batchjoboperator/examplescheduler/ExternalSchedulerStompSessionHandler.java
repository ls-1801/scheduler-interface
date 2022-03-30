package de.tuberlin.batchjoboperator.examplescheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tuberlin.batchjoboperator.schedulingreconciler.external.ExternalResourceModification;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class ExternalSchedulerStompSessionHandler<T> implements StompSessionHandler {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final WebSocketStompClient stompClient;
    private final String url;
    private final String topic;
    private final Class<T> clazz;
    private final Mono<Void> mono;
    AtomicBoolean reconnecting = new AtomicBoolean();
    private Subscriber<? super Void> publisher;
    private StompSession session;
    private StompSession.Subscription subscription;


    public ExternalSchedulerStompSessionHandler(String url, String topic, Class<T> clazz) {
        this.url = url;
        this.topic = topic;
        this.clazz = clazz;
        WebSocketClient client = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(client);
        stompClient.connect(url, this);
        this.mono = Mono.from((publisher) -> this.publisher = publisher);
    }

    public Mono<Void> toMono() {
        return mono.doFinally((v) -> this.disconnect());
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        initial().filter(b -> b).subscribe(b -> this.publisher.onComplete());
        this.session = session;
        this.subscription = session.subscribe(topic, this);
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
                                Throwable exception) {
        log.error(MessageFormat.format("Stomp threw an exception. Listing on topic: {0}", this.topic), exception);

    }

    protected void disconnect() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }

        if (stompClient.isRunning()) {
            stompClient.stop();
        }
    }

    public void handleTransportError(StompSession session, Throwable exception) {
        log.warn("Stomp: Disconnected");
        if (!session.isConnected()) {
            if (reconnecting.compareAndSet(false, true)) {  //Ensures that only one thread tries to reconnect
                try {
                    reestablishConnection();
                } finally {
                    reconnecting.set(false);
                }
            }
        }
        else {
            log.error("Transport Error", exception);
        }
    }

    private void reestablishConnection() {
        boolean disconnected = true;
        int tries = 0;
        while (disconnected) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {

            }
            try {
                log.warn("Stomp: Reconnecting... {}", tries++);
                stompClient.connect(url, this).get();
                log.info("Stomp: Reconnected");
                disconnected = false;
            } catch (Exception e) {
                log.warn("Stomp: Reconnect failed. {}", e.getMessage());
            }
        }
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return byte[].class;
    }

    protected abstract boolean handleMessage(ExternalResourceModification<T> payload);

    protected abstract Mono<Boolean> initial();

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        var type = mapper.getTypeFactory().constructParametricType(ExternalResourceModification.class, clazz);
        try {
            var parsedPayload = mapper.readValue(((byte[]) payload), type);
            try {
                if (handleMessage((ExternalResourceModification<T>) parsedPayload)) {
                    publisher.onComplete();
                }
            } catch (RuntimeException runtimeException) {
                publisher.onError(runtimeException);
            }
        } catch (IOException e) {
            log.info("ObjectMapper could not parse payload to type: {}", type, e);
        }
    }
}
