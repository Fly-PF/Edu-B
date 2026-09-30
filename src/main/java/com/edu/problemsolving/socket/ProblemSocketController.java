package com.edu.problemsolving.socket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ProblemSocketController {
    private final ProblemSocketService problemSocketService;

    @MessageMapping("/problem-solving/event")
    public void receive(ProblemSocketEvent event, Principal principal) {
        problemSocketService.receive(principal.getName(), event);
    }
}
