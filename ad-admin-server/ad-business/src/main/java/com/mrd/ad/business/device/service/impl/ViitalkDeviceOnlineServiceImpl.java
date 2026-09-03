package com.mrd.ad.business.device.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandAckRequest;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandAckResult;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandRequest;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandResult;
import com.mrd.ad.business.device.dto.ViitalkDeviceOnlineSession;
import com.mrd.ad.business.device.dto.ViitalkDeviceOnlineStatus;
import com.mrd.ad.business.device.service.ViitalkDeviceOnlineService;
import com.mrd.ad.common.exception.BusinessException;
import com.mysher.mtalk.entity.biz.UserOnline;
import com.mysher.platform.viiUser.ViiUserOnlineService;
import com.mysher.platform.viiUser.ViiUserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ViitalkDeviceOnlineServiceImpl implements ViitalkDeviceOnlineService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViitalkDeviceOnlineServiceImpl.class);
    private static final int FIRST_PAGE = 1;
    private static final int MAX_SESSION_SIZE = 100;
    private static final int MAX_ACK_CACHE_SIZE = 10000;
    private static final String DEFAULT_COMMAND = "ping";
    private static final String DEFAULT_ACK_STATUS = "success";
    private static final String PENDING_ACK_STATUS = "pending";

    private final ObjectMapper objectMapper;
    private final Map<String, ViitalkDeviceCommandAckResult> commandAckCache = new ConcurrentHashMap<String, ViitalkDeviceCommandAckResult>();

    @Reference(version = "2.0", check = false)
    private ViiUserOnlineService viiUserOnlineService;

    @Reference(version = "2.0", check = false)
    private ViiUserService viiUserService;

    public ViitalkDeviceOnlineServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ViitalkDeviceOnlineStatus queryOnlineStatus(String mzNumber) {
        if (StringUtils.isBlank(mzNumber)) {
            throw new BusinessException("大屏账号不能为空");
        }

        String queryMzNumber = mzNumber.trim();
        try {
            long onlineCount = viiUserOnlineService.listOnlineUserCount(queryMzNumber);
            List<UserOnline> onlineUsers = viiUserOnlineService.listOnlineUser(FIRST_PAGE, MAX_SESSION_SIZE, queryMzNumber);
            List<ViitalkDeviceOnlineSession> sessions = toSessions(onlineUsers);

            ViitalkDeviceOnlineStatus status = new ViitalkDeviceOnlineStatus();
            status.setMzNumber(queryMzNumber);
            status.setOnline(!sessions.isEmpty() || onlineCount > 0);
            status.setOnlineCount(onlineCount > 0 ? onlineCount : sessions.size());
            status.setSessions(sessions);
            return status;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "查询ViiTalk设备在线状态失败：" + e.getMessage());
        }
    }

    @Override
    public ViitalkDeviceCommandResult sendCommand(ViitalkDeviceCommandRequest request) {
        if (request == null || StringUtils.isBlank(request.getMzNumber())) {
            throw new BusinessException("大屏账号不能为空");
        }

        String mzNumber = request.getMzNumber().trim();
        ViitalkDeviceOnlineStatus onlineStatus = queryOnlineStatus(mzNumber);
        if (!onlineStatus.isOnline()) {
            throw new BusinessException("大屏设备不在线，无法下发消息");
        }

        String command = StringUtils.defaultIfBlank(request.getCommand(), DEFAULT_COMMAND).trim();
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String content = buildContent(request, command, requestId);

        try {
            viiUserService.synergyGeneralInfo(mzNumber, escapePercentForStringFormat(content));
        } catch (Exception e) {
            throw new BusinessException(500, "下发ViiTalk设备消息失败：" + e.getMessage());
        }

        Date sendTime = new Date();
        ViitalkDeviceCommandResult result = new ViitalkDeviceCommandResult();
        result.setMzNumber(mzNumber);
        result.setRequestId(requestId);
        result.setCommand(command);
        result.setContent(content);
        result.setOnline(true);
        result.setOnlineCount(onlineStatus.getOnlineCount());
        result.setSendTime(sendTime);
        cachePendingAck(requestId, mzNumber, command, sendTime);
        return result;
    }

    @Override
    public ViitalkDeviceCommandAckResult receiveCommandAck(ViitalkDeviceCommandAckRequest request) {
        if (request == null || StringUtils.isBlank(request.getRequestId())) {
            throw new BusinessException("requestId不能为空");
        }

        String requestId = request.getRequestId().trim();
        ViitalkDeviceCommandAckResult cached = commandAckCache.get(requestId);

        ViitalkDeviceCommandAckResult result = new ViitalkDeviceCommandAckResult();
        result.setRequestId(requestId);
        result.setMzNumber(StringUtils.defaultIfBlank(StringUtils.trimToNull(request.getMzNumber()), cached == null ? null : cached.getMzNumber()));
        result.setCommand(StringUtils.defaultIfBlank(StringUtils.trimToNull(request.getCommand()), cached == null ? null : cached.getCommand()));
        result.setStatus(StringUtils.defaultIfBlank(request.getStatus(), DEFAULT_ACK_STATUS).trim());
        result.setMessage(StringUtils.trimToNull(request.getMessage()));
        result.setSendTime(cached == null ? null : cached.getSendTime());
        result.setAckTime(new Date());
        commandAckCache.put(requestId, result);

        LOGGER.info("ViiTalk设备指令ACK，requestId={}, mzNumber={}, command={}, status={}, message={}, timestamp={}, payload={}",
                result.getRequestId(), result.getMzNumber(), result.getCommand(), result.getStatus(), result.getMessage(),
                request.getTimestamp(), request.getPayload());
        return result;
    }

    @Override
    public ViitalkDeviceCommandAckResult queryCommandAck(String requestId) {
        if (StringUtils.isBlank(requestId)) {
            throw new BusinessException("requestId不能为空");
        }
        ViitalkDeviceCommandAckResult result = commandAckCache.get(requestId.trim());
        if (result == null) {
            throw new BusinessException(404, "未找到指令ACK记录");
        }
        return result;
    }

    private void cachePendingAck(String requestId, String mzNumber, String command, Date sendTime) {
        if (commandAckCache.size() >= MAX_ACK_CACHE_SIZE) {
            commandAckCache.clear();
        }
        ViitalkDeviceCommandAckResult pending = new ViitalkDeviceCommandAckResult();
        pending.setRequestId(requestId);
        pending.setMzNumber(mzNumber);
        pending.setCommand(command);
        pending.setStatus(PENDING_ACK_STATUS);
        pending.setMessage("指令已下发，等待设备ACK");
        pending.setSendTime(sendTime);
        commandAckCache.put(requestId, pending);
    }

    private String buildContent(ViitalkDeviceCommandRequest request, String command, String requestId) {
        if (StringUtils.isNotBlank(request.getContent())) {
            return request.getContent().trim();
        }

        Map<String, Object> content = new LinkedHashMap<String, Object>();
        content.put("action", "viiad");
        content.put("command", command);
        content.put("requestId", requestId);
        content.put("timestamp", System.currentTimeMillis());
        content.put("payload", request.getPayload());
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "构建设备消息内容失败：" + e.getMessage());
        }
    }

    private String escapePercentForStringFormat(String content) {
        return content == null ? null : content.replace("%", "%%");
    }

    private List<ViitalkDeviceOnlineSession> toSessions(List<UserOnline> onlineUsers) {
        List<ViitalkDeviceOnlineSession> sessions = new ArrayList<ViitalkDeviceOnlineSession>();
        if (onlineUsers == null) {
            return sessions;
        }
        for (UserOnline userOnline : onlineUsers) {
            if (userOnline == null) {
                continue;
            }
            ViitalkDeviceOnlineSession session = new ViitalkDeviceOnlineSession();
            session.setJid(userOnline.getJid());
            session.setUsername(parseUsername(userOnline.getJid()));
            session.setMzNumber(userOnline.getMzNumber());
            session.setIp(userOnline.getIp());
            session.setAppVer(userOnline.getAppVer());
            session.setProVer(userOnline.getProVer());
            session.setPresenceState(userOnline.getPresenceState());
            session.setOnlineTime(userOnline.getDate());
            session.setPriority(userOnline.getPriority());
            session.setPresenceType(userOnline.getPresenceType());
            session.setPresenceShow(userOnline.getPresenceShow());
            session.setSecure(userOnline.isSecure());
            sessions.add(session);
        }
        return sessions;
    }

    private String parseUsername(String jid) {
        if (StringUtils.isBlank(jid)) {
            return null;
        }
        int atIndex = jid.indexOf('@');
        if (atIndex <= 0) {
            return jid;
        }
        return jid.substring(0, atIndex);
    }
}

