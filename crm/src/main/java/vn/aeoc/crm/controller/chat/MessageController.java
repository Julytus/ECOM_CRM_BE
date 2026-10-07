package vn.aeoc.crm.controller.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.aeoc.base.config.extention.paging.Cursor;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.request.SendMessageRequest;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.chat.MessageService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.crm.util.SecurityUtils;
import vn.aeoc.entity.data.mysql.Message;

import java.security.Principal;

@RestController
@RequestMapping("/message")
public class MessageController extends BaseController {

    @Autowired
    private MessageService service;

    @Autowired
    private UserService userService;

    @GetMapping("/list-cursor")
    public ResponseEntity<ApiResponse<Cursor<Message>>> listCursor(
            @RequestParam Integer chatId,
            @RequestParam(required = false) Integer beforeMessageId,
            @RequestParam(defaultValue = "20")  int limit,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        return Mapper.map(
                service.getMessagesCursor(chatId, userId, beforeMessageId, limit),
                ApiResponse::okEntity
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Cursor<Message>>> search(
            @RequestParam Integer chatId,
            @RequestParam String keyword,
            @RequestParam(required = false) Integer beforeMessageId,
            @RequestParam(defaultValue = "20")  int limit,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        return Mapper.map(
                service.searchMessages(chatId, userId, keyword, beforeMessageId, limit),
                ApiResponse::okEntity
        );
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Page<Message>>> list(
            @RequestParam Integer chatId,
            Pageable pageable,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        return Mapper.map(
                service.getMessages(chatId, userId, pageable),
                ApiResponse::okEntity
        );
    }

    /**
     * Send message with file attachment.
     */
    @PostMapping(value = "/send")
    public ResponseEntity<ApiResponse<Message>> send(
            @RequestPart("request") SendMessageRequest req,
            @RequestPart(name = "file", required = false) MultipartFile file,
            Principal principal
    ) {
        Integer userId   = SecurityUtils.userId(principal);
        String  userName = SecurityUtils.name(principal);
        var login = userService.getUserById(userId);
        Integer branchId = login != null && CommonUtils.NVL(login.getBranchId()) > 0
                ? login.getBranchId()
                : null;

        return Mapper.map(
                service.send(userId, userName, req, file, branchId),
                ApiResponse::okEntity
        );
    }

    @PostMapping("/edit")
    public ResponseEntity<ApiResponse<Message>> edit(
            @RequestParam Integer messageId,
            @RequestParam String content,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        return Mapper.map(
                service.editMessage(messageId, userId, content),
                ApiResponse::okEntity
        );
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @RequestParam Integer messageId,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        return Mapper.map(
                service.deleteMessage(messageId, userId),
                ApiResponse::okEntity
        );
    }

    @PostMapping("/mark-read")
    public ResponseEntity<ApiResponse<Integer>> markRead(
            @RequestParam Integer messageId,
            @RequestParam Integer chatId,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        service.markRead(messageId, chatId, userId);
        return Mapper.map(1, ApiResponse::okEntity);
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Integer>> markAllRead(
            @RequestParam Integer chatId,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        service.markAllRead(chatId, userId);
        return Mapper.map(1, ApiResponse::okEntity);
    }
}