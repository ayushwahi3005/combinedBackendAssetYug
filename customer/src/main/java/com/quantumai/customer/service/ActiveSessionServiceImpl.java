package com.quantumai.customer.service;

import com.quantumai.customer.entity.ActiveSession;
import com.quantumai.customer.repository.ActiveSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ActiveSessionServiceImpl implements  ActiveSessionService{

    @Autowired
    private ActiveSessionRepository sessionRepository;
    @Autowired
    private SessionRegistry sessionRegistry;



    @Override
    public Optional<ActiveSession> getActiveSessionByUserId(String userId) {
        return sessionRepository.findByUserId(userId);
    }

    @Override
    public boolean isSameBrowserAndDevice(String userId, String deviceId, String userAgent) {
        Optional<ActiveSession> activeSession = sessionRepository.findByUserId(userId);


        if (activeSession.isPresent()) {
            ActiveSession session = activeSession.get();
            System.out.println("Device Id:"+session.getDeviceId().equals(deviceId));
            System.out.println("Time Stamp:"+session.getLastActivityTime().isBefore(LocalDateTime.now().minusHours(24)));

            return (session.getDeviceId().equals(deviceId)||session.getLastActivityTime().isBefore(LocalDateTime.now().minusHours(24)));
        }


        return true;
    }

    @Override
    public void removeSession(String userId) {
        Optional<ActiveSession> activeSession = sessionRepository.findByUserId(userId);
        if (activeSession.isPresent()) {
            sessionRepository.deleteByUserId(userId);
        }
        List<Object> principals = sessionRegistry.getAllPrincipals();

        for (Object principal : principals) {
            if (principal instanceof UserDetails && ((UserDetails) principal).getUsername().equals(userId)) {
                List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                for (SessionInformation session : sessions) {
                    System.out.println("Session expiring forcefully->"+session.getSessionId());
                    session.expireNow(); // Mark the session as expired
                    System.out.println("Session expiring forcefully->"+session.isExpired());
                }
            }
        }
    }

    @Override
    public void createOrUpdateSession(String userId, String sessionId, String userAgent,String deviceId) {
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
