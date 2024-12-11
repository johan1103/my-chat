package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class WebSocketChatServer {

    private static final ConcurrentHashMap<ChannelId, Channel> clients = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(WebSocketChatServer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)
                     .childHandler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) throws Exception {
                             ChannelPipeline pipeline = ch.pipeline();
                             pipeline.addLast(new HttpServerCodec());
                             pipeline.addLast(new HttpObjectAggregator(65536));
                             pipeline.addLast(new WebSocketServerProtocolHandler("/chat"));
                             pipeline.addLast(new WebSocketChatHandler());
                         }
                     });

            ChannelFuture future = bootstrap.bind(8080).sync();
            System.out.println("WebSocket Chat Server is running on ws://localhost:8080/chat");
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static class WebSocketChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            String userId = ctx.channel().id().toString();
            clients.put(ctx.channel().id(), ctx.channel());
            broadcastMessage(userId + "A new client has joined!", userId);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
            String jsonMessage = msg.text();
            try {
                String userId = ctx.channel().id().toString();
                ClientMessage clientMessage = objectMapper.readValue(jsonMessage, ClientMessage.class);
                ServerMessage serverMessage = ServerMessage.of(userId, clientMessage);
                broadcastServerMessage(serverMessage, userId);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getLocalizedMessage());
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            String userId = ctx.channel().id().toString();
            clients.remove(ctx.channel().id());
            broadcastMessage(userId + " client has left.", userId);
        }

        private void broadcastServerMessage(ServerMessage serverMessage, String senderChannelId) throws JsonProcessingException {
            String message = MessagePrefix.SERVER_MESSAGE.getType() + objectMapper.writeValueAsString(serverMessage);
            log.info("jsonStr : {}", message);
            broadcast(message, senderChannelId);
        }

        private void broadcastMessage(String message, String senderChannelId) {
            message = MessagePrefix.STRING.getType() + message;
            log.info("str : {}", message);
            broadcast(message, senderChannelId);
        }

        private void broadcast(String message, String senderChannelId) {
            for (Channel channel : clients.values()) {
                if(channel.id().toString().equals(senderChannelId))
                    continue;
                channel.writeAndFlush(new TextWebSocketFrame(message));
            }
        }
    }
}
