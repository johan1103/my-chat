package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketChatServer {

    private static final ConcurrentHashMap<String, WebsocketOutbound> clients = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(WebSocketChatServer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws InterruptedException {
        DisposableServer server = HttpServer.create().port(9000)
                .route(routes -> routes.ws("/chat", (in, out)->handleWebSocket(in, out))).bindNow();

        System.out.println("WebSocket Chat Server is running on ws://localhost:9000/chat");
        server.onDispose().block();
    }

    private static Flux<Void> handleWebSocket(WebsocketInbound inbound, WebsocketOutbound outbound) {
        String clientId = String.valueOf(inbound.hashCode());
        clients.put(clientId, outbound);

        broadcastMessage(MessagePrefix.STRING.getType()+clientId+" joined the chat", clientId).subscribe();
        log.info("{} joined the chat", clientId);

        // 연결 종료 시 특정 로직 실행
        inbound.receiveCloseStatus()
                .flatMap(status ->{
                        clients.remove(clientId);
                        return broadcastMessage(MessagePrefix.STRING.getType()+clientId+" has left the chat", clientId)
                                .then();
                }).subscribeOn(Schedulers.boundedElastic())
                .subscribe(); // 스트림을 구독하여 종료 상태 감지

        return inbound.receive().asString()
                .flatMap(message -> processMessage(clientId, message));
    }

    private static Flux<Void> processMessage(String senderId, String jsonMessage) {
        try {
            ClientMessage clientMessage = objectMapper.readValue(jsonMessage, ClientMessage.class);
            ServerMessage serverMessage = ServerMessage.of(senderId, clientMessage);
            String serverMessageStr = MessagePrefix.SERVER_MESSAGE.getType() + objectMapper.writeValueAsString(serverMessage);
            return broadcastMessage(serverMessageStr, senderId);
        }catch (JsonProcessingException e) {
            log.error("Failed to process message: {}", e.getMessage());
            return Flux.empty();
        }
    }

    private static Flux<Void> broadcastMessage(String message, String senderId) {
        return Flux.fromIterable(clients.entrySet())
                .filter(entry -> !entry.getKey().equals(senderId))
                .flatMap(entry -> entry.getValue().sendString(Flux.just(message)).then());
    }

}
