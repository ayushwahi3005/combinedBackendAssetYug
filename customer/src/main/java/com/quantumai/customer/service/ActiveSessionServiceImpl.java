package com.quantumai.customer.service;

import com.quantumai.customer.entity.ActiveSession;
import com.quantumai.customer.entity.ActiveSessionMobile;
import com.quantumai.customer.repository.ActiveSessionMobileRepository;
import com.quantumai.customer.repository.ActiveSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class ActiveSessionServiceImpl implements ActiveSessionService {

  @Autowired private ActiveSessionRepository sessionRepository;
  @Autowired private ActiveSessionMobileRepository sessionMobileRepository;
  @Autowired private SessionRegistry sessionRegistry;

  @Override
  public Optional<ActiveSession> getActiveSessionByUserId(String userId) {
    return sessionRepository.findByUserId(userId);
  }

  @Override
  public boolean isSameBrowserAndDevice(String userId, String deviceId, String userAgent) {
    Optional<ActiveSession> activeSession = sessionRepository.findByUserId(userId);

    if (activeSession.isPresent()) {
      ActiveSession session = activeSession.get();
      System.out.println("Device Id:" + session.getDeviceId().equals(deviceId));
      System.out.println(
          "Time Stamp:"
              + session.getLastActivityTime().isAfter(LocalDateTime.now().minusHours(24)));

      return (session.getDeviceId().equals(deviceId)
          || session.getLastActivityTime().isAfter(LocalDateTime.now().minusHours(24)));
    }

    return true;
  }

  @Override
  public boolean isSameMobile(String userId, String mobileId, String userAgent) {
    Optional<ActiveSessionMobile> activeSession = sessionMobileRepository.findByUserId(userId);

    if (activeSession.isPresent()) {
      ActiveSessionMobile session = activeSession.get();
      System.out.println("Device Id:" + session.getMobileId().equals(mobileId));
      System.out.println(
          "Time Stamp:"
              + session.getLastActivityTime().isAfter(LocalDateTime.now().minusHours(24)));

      return (session.getMobileId().equals(mobileId)
          && session.getLastActivityTime().isAfter(LocalDateTime.now().minusHours(24)));
    }

    return true;
  }

  @Override
  public void removeSession(String userId) {
    Optional<ActiveSession> activeSession = sessionRepository.findByUserId(userId);
    Optional<ActiveSessionMobile> activeSessionMobile =
        sessionMobileRepository.findByUserId(userId);
    if (activeSession.isPresent()) {
      sessionRepository.deleteByUserId(userId);
    }
    if (activeSessionMobile.isPresent()) {
      sessionMobileRepository.deleteBySessionId(userId);
    }
    List<Object> principals = sessionRegistry.getAllPrincipals();

    for (Object principal : principals) {
      if (principal instanceof UserDetails
          && ((UserDetails) principal).getUsername().equals(userId)) {
        List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
        for (SessionInformation session : sessions) {
          System.out.println("Session expiring forcefully->" + session.getSessionId());
          session.expireNow(); // Mark the session as expired
          System.out.println("Session expiring forcefully->" + session.isExpired());
        }
      }
    }
  }

  @Override
  public void createOrUpdateSession(
      String userId, String sessionId, String userAgent, String deviceId) {
    sessionRepository.deleteByUserId(userId);

    // Create a new session
    ActiveSession newSession = new ActiveSession();
    newSession.setUserId(userId);
    newSession.setSessionId(sessionId);
    newSession.setDeviceId(deviceId);
    newSession.setUserAgent(userAgent);
    newSession.setLastActivityTime(LocalDateTime.now());
    sessionRepository.save(newSession);
  }

  @Override
  public void createOrUpdateSessionMobile(
      String userId, String sessionId, String userAgent, String mobileId) {
    sessionMobileRepository.deleteByUserId(userId);

    // Create a new session
    ActiveSessionMobile newSession = new ActiveSessionMobile();
    newSession.setUserId(userId);
    newSession.setSessionId(sessionId);
    newSession.setMobileId(mobileId);
    newSession.setUserAgent(userAgent);
    newSession.setLastActivityTime(LocalDateTime.now());
    System.out.println("====> Mobile New Session--->" + newSession.toString());
    sessionMobileRepository.save(newSession);
  }

  @Override
  public void updateLastActivity(String sessionId) {
    Optional<ActiveSession> session = sessionRepository.findById(sessionId);
    if (session.isPresent()) {
      session.get().setLastActivityTime(LocalDateTime.now());
      sessionRepository.save(session.get());
    }
  }

  @Override
  public void invalidateSession(String sessionId) {
    sessionRepository.deleteBySessionId(sessionId);
  }
}
