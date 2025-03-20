package com.github.aiassistant.platform;

import com.github.aiassistant.service.text.sseemitter.SseEmitterResponseHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class SpringWebSseEmitterResponseHandler extends SseEmitterResponseHandler {

    public SpringWebSseEmitterResponseHandler(SseEmitter emitter, boolean debug, String userQueryTraceNumber, Boolean websearch) {
        super(new SpringWebSseEmitter(emitter), debug, userQueryTraceNumber, websearch);
    }
}
