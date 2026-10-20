package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.dto.ChatDTO;
import com.mindharbor.psych.entity.AiMessage;
import com.mindharbor.psych.entity.AiSession;
import com.mindharbor.psych.service.AiChatService;
import com.mindharbor.psych.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;
    private final AuthService authService;

    @GetMapping("/sessions")
    public R<List<AiSession>> sessions() {
        return R.ok(aiChatService.sessions(authService.currentUser().getId()));
    }

    @PostMapping("/sessions")
    public R<AiSession> create(@RequestParam(required = false) String title) {
        return R.ok(aiChatService.createSession(authService.currentUser().getId(), title));
    }

    @DeleteMapping("/sessions/{id}")
    public R<Void> delete(@PathVariable Long id) {
        aiChatService.deleteSession(authService.currentUser().getId(), id);
        return R.ok();
    }

    @GetMapping("/sessions/{id}/messages")
    public R<List<AiMessage>> messages(@PathVariable Long id) {
        return R.ok(aiChatService.messages(authService.currentUser().getId(), id));
    }

    /** SSE 流式对话 */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatDTO dto) {
        return aiChatService.chat(authService.currentUser().getId(), dto);
    }

    /** 当前 AI 可调用的技能清单 */
    @GetMapping("/skills")
    public R<List<String>> skills() {
        return R.ok(aiChatService.skillNames());
    }
}
