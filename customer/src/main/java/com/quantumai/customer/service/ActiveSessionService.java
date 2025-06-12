package com.quantumai.customer.service;

import com.quantumai.customer.entity.ActiveSession;
import java.util.Optional;

public interface ActiveSessionService {

  public Optional<ActiveSession> getActiveSessionByUserId(String userId);

  public void createOrUpdateSession(
      String userId, String sessionId, String userAgent, String deviceId);

  public void createOrUpdateSessionMobile(
      String userId, String sessionId, String userAgent, String mobileId);

  public void updateLastActivity(String sessionId);

  public void invalidateSession(String sessionId);

  public boolean isSameBrowserAndDevice(String userId, String deviceId, String userAgent);

  public boolean isSameMobile(String userId, String mobileId, String userAgent);

  public void removeSession(String userId);
}
