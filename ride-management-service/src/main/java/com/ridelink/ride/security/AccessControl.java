package com.ridelink.ride.security;


import com.ridelink.ride.entity.Ride;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
@Component
public class AccessControl {
    private Authentication authentication() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        return auth;
    }
    public String subject() { return authentication().getName(); }
    public boolean hasRole(String role) { return authentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_"+role)); }
    public boolean privileged() { return hasRole("ADMIN") || hasRole("SERVICE"); }
    public String driverEmail() {
        var auth=authentication();
        String email=auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getClaimAsString("email") : auth.getName();
        if(email==null || email.isBlank()) throw new AccessDeniedException("Verified driver email is required");
        return email;
    }
    public void ownerOrAdmin(Ride ride) {
        if(!privileged() && !(hasRole("PASSENGER") && subject().equals(ride.getPassengerId()))) throw new AccessDeniedException("Ride owner permission required");
    }
    public void driverOrAdmin(Ride ride) {
        if(!privileged() && !(hasRole("DRIVER") && driverEmail().equalsIgnoreCase(ride.getDriverEmail()))) throw new AccessDeniedException("Assigned driver permission required");
    }
    public void reader(Ride ride) {
        if(privileged()) return;
        if(hasRole("PASSENGER") && subject().equals(ride.getPassengerId())) return;
        if(hasRole("DRIVER") && driverEmail().equalsIgnoreCase(ride.getDriverEmail())) return;
        throw new AccessDeniedException("Ride access denied");
    }
}
