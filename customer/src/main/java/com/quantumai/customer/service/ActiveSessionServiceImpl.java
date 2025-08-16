package com.quantumai.customer.service;

import com.mongodb.DuplicateKeyException;
import com.quantumai.customer.entity.ActiveSession;
import com.quantumai.customer.entity.ActiveSessionMobile;
import com.quantumai.customer.repository.ActiveSessionMobileRepository;
import com.quantumai.customer.repository.ActiveSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@Service
public class ActiveSessionServiceImpl implements ActiveSessionService {

  @Autowired private ActiveSessionRepository sessionRepository;
  @Autowired private ActiveSessionMobileRepository sessionMobileRepository;
  @Autowired private SessionRegistry sessionRegistry;

  private final MongoTemplate mongoTemplate;

  public ActiveSessionServiceImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

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
          && session.getLastActivityTime().isAfter(LocalDateTime.now().minusHours(24)));
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

//  @Override
//  public void createOrUpdateSession(String userId, String sessionId, String userAgent, String deviceId) {
//    // Try to find an existing session for this user
//    ActiveSession existingSession = sessionRepository.findByUserId(userId).orElse(null);
//
//    if (existingSession != null) {
//      // Update the existing session
////      existingSession.setSessionId(sessionId);
//      existingSession.setDeviceId(deviceId);
//      existingSession.setUserAgent(userAgent);
//      existingSession.setLastActivityTime(LocalDateTime.now());
//      log.info("Existing session -- {}",sessionId);
//      sessionRepository.save(existingSession);
//    } else {
//      // Create a new session
//      ActiveSession newSession = new ActiveSession();
//      newSession.setUserId(userId);
////      newSession.setSessionId(sessionId);
//      newSession.setDeviceId(deviceId);
//      newSession.setUserAgent(userAgent);
//      newSession.setLastActivityTime(LocalDateTime.now());
//      log.info("New session --");
//      sessionRepository.save(newSession);
//    }
//  }

@Override
public void createOrUpdateSession(String userId, String sessionId, String userAgent, String deviceId) {
  Query query = Query.query(Criteria.where("userId").is(userId));

  Update update = new Update()
          .set("deviceId", deviceId)
          .set("userAgent", userAgent)
          .set("lastActivityTime", LocalDateTime.now())
          // set these only on insert (so we don't overwrite existing sessionId/_id)
          .setOnInsert("userId", userId)
          .setOnInsert("_id", sessionId); // set _id to provided sessionId on insert

  FindAndModifyOptions options = FindAndModifyOptions.options()
          .upsert(true)
          .returnNew(true);

  try {
    ActiveSession updated = mongoTemplate.findAndModify(query, update, options, ActiveSession.class);
    if (updated == null) {
      // Defensive: if DB returned null, read it explicitly
      updated = mongoTemplate.findOne(query, ActiveSession.class);
    }
    log.info("Upserted session for {} -> id {}", userId, updated != null ? updated.getSessionId() : "null");
  } catch (DuplicateKeyException ex) {
    // Rare race: two upserts hit unique index simultaneously. Retry read-and-update.
    log.warn("DuplicateKey on upsert for userId {}, retrying read", userId, ex);
    ActiveSession existing = mongoTemplate.findOne(query, ActiveSession.class);
    if (existing != null) {
      Update update2 = new Update()
              .set("deviceId", deviceId)
              .set("userAgent", userAgent)
              .set("lastActivityTime", LocalDateTime.now());
      mongoTemplate.findAndModify(
              Query.query(Criteria.where("_id").is(existing.getSessionId())),
              update2,
              FindAndModifyOptions.options().returnNew(true),
              ActiveSession.class);
    } else {
      // as a last resort, try upsert once more (very unlikely)
      mongoTemplate.findAndModify(query, update, options, ActiveSession.class);
    }
  }

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
      log.info("Updating LastActivity");
      sessionRepository.save(session.get());
    }
  }

  @Override
  public void invalidateSession(String sessionId) {
    sessionRepository.deleteBySessionId(sessionId);
  }
}
