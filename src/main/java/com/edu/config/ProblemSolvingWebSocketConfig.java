package com.edu.config;

import com.edu.problemsolving.session.ProblemLessonSession;
import com.edu.problemsolving.session.ProblemLessonSessionRegistry;
import com.edu.problemsolving.socket.ProblemSocketPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ProblemSolvingWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final ProblemLessonSessionRegistry lessonSessionRegistry;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/problem-solving").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    ProblemLessonSession session = lessonSessionRegistry.connect(
                            accessor.getFirstNativeHeader("clientSessionId"), accessor.getFirstNativeHeader("wsToken"));
                    accessor.setUser(new ProblemSocketPrincipal(session.getClientSessionId()));
                    lessonSessionRegistry.bindTransport(accessor.getSessionId(), session.getClientSessionId());
                }
                return message;
            }
        });
    }
}
