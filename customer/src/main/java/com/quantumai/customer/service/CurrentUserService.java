package com.quantumai.customer.service;

/**
 * Resolves the display name of the currently authenticated user.
 */
public interface CurrentUserService {

  /**
   * Returns {@code firstName + " " + lastName} for the logged-in user,
   * falling back to email when name fields are unavailable.
   */
  String getCurrentUserDisplayName();
}
