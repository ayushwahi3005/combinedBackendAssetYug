package com.quantumai.customer.entity;

import java.util.*;

public enum Role {
	ADMIN;
    // Add more predefined roles if needed

    // Custom method to dynamically add roles
    public static Set<Role> createRoles(String... roles) {
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(ADMIN); // Always add ROLE_ADMIN
        for (String role : roles) {
            userRoles.add(Role.valueOf(role));
        }
        return userRoles;
    }
}
