package com.olamide.receipthandler.configurations;

import com.olamide.receipthandler.models.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    public static User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated()){
            throw new AccessDeniedException("You are not authorized to perform this action.");
        }
        Object principal = authentication.getPrincipal();
        if(principal instanceof User currentUser){
            return currentUser;
        }
        else {
            throw new AccessDeniedException("Unexpected user type.");
        }
    }
    public static boolean isOwner(String walletOwnerEmail) {
        User currentUser = getAuthenticatedUser();
        return currentUser.getEmail().equals(walletOwnerEmail);
    }
}