package vn.aeoc.crm.controller.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Cursor;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.request.CreateChatRequest;
import vn.aeoc.crm.data.response.ChatSidebarSummary;
import vn.aeoc.crm.data.response.ChatView;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.chat.ChatService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.crm.util.SecurityUtils;
import vn.aeoc.entity.data.mysql.chat.Chat;
import vn.aeoc.entity.data.mysql.chat.ChatParticipant;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController extends BaseController {

    @Autowired
    private ChatService service;

    @Autowired
    private UserService userService;

    @GetMapping("/list-cursor")
    public ResponseEntity<ApiResponse<Cursor<ChatView>>> listCursor(
            @RequestParam(required = false) Integer beforeChatId,
            @RequestParam(defaultValue = "20")  int limit,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        return Mapper.map(
                service.getChatsByUserCursor(userId, beforeChatId, limit, null),
                ApiResponse::okEntity
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Cursor<ChatView>>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer beforeChatId,
            @RequestParam(defaultValue = "20")  int limit,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        int branchId = BranchFilter.listScopeBranchId(userService.getUserById(userId));
        return Mapper.map(
                service.searchChats(userId, keyword, beforeChatId, limit, branchId),
                ApiResponse::okEntity
        );
    }

    @GetMapping("/sidebar-summary")
    public ResponseEntity<ApiResponse<ChatSidebarSummary>> sidebarSummary(
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        int branchId = BranchFilter.listScopeBranchId(userService.getUserById(userId));
        return Mapper.map(
                service.getSidebarSummary(userId, branchId),
                ApiResponse::okEntity
        );
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Page<ChatView>>> list(
            Pageable pageable,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        int branchId = BranchFilter.listScopeBranchId(userService.getUserById(userId));
        return Mapper.map(
                service.getChatsByUser(userId, pageable, branchId),
                ApiResponse::okEntity
        );
    }

    @GetMapping("/get")
    public ResponseEntity<ApiResponse<Chat>> get(
            @RequestParam Integer id
    ) {
        return Mapper.map(service.getById(id), ApiResponse::okEntity);
    }

    @GetMapping("/view")
    public ResponseEntity<ApiResponse<ChatView>> view(
            @RequestParam Integer id,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        Chat chat = service.getById(id);
        ChatView view = service.toChatView(chat, userId);
        return Mapper.map(view, ApiResponse::okEntity);
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ChatView>> create(
            @RequestBody CreateChatRequest req,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        String userName = SecurityUtils.userName(principal);
        var login = userService.getUserById(userId);
        Integer branchId = login != null ? login.getBranchId() : null;
        return Mapper.map(
                service.createChat(userId, userName, req, branchId),
                ApiResponse::okEntity
        );
    }

    @GetMapping("/participants")
    public ResponseEntity<ApiResponse<List<ChatParticipant>>> participants(
            @RequestParam Integer chatId
    ) {
        return Mapper.map(service.getParticipants(chatId), ApiResponse::okEntity);
    }

    @PostMapping("/add-member")
    public ResponseEntity<ApiResponse<ChatParticipant>> addMember(
            @RequestParam Integer chatId,
            @RequestParam Integer userId,
            @RequestParam(required = false) String displayName,
            Principal principal
    ) {
        Integer addedBy   = SecurityUtils.userId(principal);
        String  addedName = SecurityUtils.userName(principal);
        return Mapper.map(
                service.addMember(chatId, userId, displayName, addedBy, addedName),
                ApiResponse::okEntity
        );
    }

    @PostMapping("/leave")
    public ResponseEntity<ApiResponse<Integer>> leave(
            @RequestParam Integer chatId,
            Principal principal
    ) {
        Integer userId = SecurityUtils.userId(principal);
        return Mapper.map(service.leaveChat(chatId, userId), ApiResponse::okEntity);
    }
}