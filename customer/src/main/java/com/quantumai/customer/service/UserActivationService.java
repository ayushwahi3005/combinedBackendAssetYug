package com.quantumai.customer.service;

public interface UserActivationService {
    /**
     * Updates active status of users based on the subscription limit for a company
     * @param companyId The ID of the company
     * @param activeLimit The maximum number of active users allowed
     * @return The number of users that were deactivated (if any)
     */
    int updateActiveUsersBySubscription(Long companyId, int activeLimit);
    
    /**
     * Checks if a user can be activated based on subscription limits
     * @param companyId The ID of the company
     * @return true if a new user can be activated, false otherwise
     */
    boolean canActivateNewUser(Long companyId);
    
    /**
     * Deactivates a specific user
     * @param userId The ID of the user to deactivate
     * @return true if the user was deactivated, false if the user was already inactive
     */
    boolean deactivateUser(String userId);
    
    /**
     * Activates a specific user if within subscription limits
     * @param userId The ID of the user to activate
     * @return true if the user was activated, false if activation would exceed limits
     */
    boolean activateUser(String userId);
}
