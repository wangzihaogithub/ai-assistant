package com.github.aiassistant.platform;

import com.github.aiassistant.service.text.sseemitter.SseEmitterResponseHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public class SpringWebSseEmitter implements SseEmitterResponseHandler.Emitter {
    private final SseEmitter sseEmitter;

    public SpringWebSseEmitter(SseEmitter sseEmitter) {
        this.sseEmitter = sseEmitter;
    }

    @Override
    public void send(String id, String name, Object data) throws IOException, IllegalStateException {
        SseEmitter.SseEventBuilder event = SseEmitter.event();
        if (id != null) {
            event.id(id);
        }
        sseEmitter.send(event.name(name).data(data));
    }

    @Override
    public void complete() {
        sseEmitter.complete();
    }
}
