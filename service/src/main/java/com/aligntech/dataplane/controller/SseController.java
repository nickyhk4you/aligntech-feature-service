package com.aligntech.dataplane.controller;

import com.aligntech.event.FlagChangeListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
public class SseController {

    private final FlagChangeListener flagChangeListener;

    public SseController(FlagChangeListener listener) {
        this.flagChangeListener = listener;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return flagChangeListener.subscribe();
    }
}
