//package com.example.appointmentservice.business.client;
//
//import com.example.appointmentservice.domain.dto.UserDto;
//import com.example.appointmentservice.persistence.model.AppointmentEntity;
//import com.example.appointmentservice.persistence.repository.AppointmentRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AppointmentSecurityService {
//
//    private final AppointmentRepository appointmentRepository;
//    private final UserServiceClient userServiceClient;
//
//
//    public boolean canAccessAppointment(String appointmentId) {
//        try {
//            String currentUsername = getCurrentUsername();
//            log.info("=== AUTHORIZATION CHECK ===");
//            log.info("Current username from JWT: {}", currentUsername);
//
//            if (currentUsername == null) {
//                log.warn("No authenticated user found");
//                return false;
//            }
//
//            // Get user details from User Service
//            UserDto currentUser = userServiceClient.getUserByUsername(currentUsername);
//            if (currentUser == null) {
//                log.warn("User not found in User Service: {}", currentUsername);
//                return false;
//            }
//
//            log.info("Current user ID: {}, Role: {}", currentUser.getId(), currentUser.getRole());
//
//            // ADMIN can access any appointment
//            if (isAdmin(currentUser)) {
//                log.info("✓ Admin user {} accessing appointment {}", currentUsername, appointmentId);
//                return true;
//            }
//
//            // Get the appointment entity
//            Optional<AppointmentEntity> appointmentOpt = appointmentRepository.findById(appointmentId);
//            if (appointmentOpt.isEmpty()) {
//                log.warn("Appointment not found: {}", appointmentId);
//                return false;
//            }
//
//            AppointmentEntity appointment = appointmentOpt.get();
//            log.info("Appointment requesterId: {}, providerId: {}",
//                    appointment.getRequesterId(), appointment.getProviderId());
//
//            // Allow if requester or provider matches
//            boolean isRequester = appointment.getRequesterId() != null &&
//                    appointment.getRequesterId().equals(currentUser.getId());
//            boolean isProvider = appointment.getProviderId() != null &&
//                    appointment.getProviderId().equals(currentUser.getId());
//
//            log.info("Access check - isRequester: {}, isProvider: {}", isRequester, isProvider);
//
//            if (isRequester || isProvider) {
//                log.info("✓ User {} can access appointment {} (requester: {}, provider: {})",
//                        currentUsername, appointmentId, isRequester, isProvider);
//                return true;
//            }
//
//            log.warn("✗ User {} (ID: {}) denied access to appointment {} (requester: {}, provider: {})",
//                    currentUsername, currentUser.getId(), appointmentId,
//                    appointment.getRequesterId(), appointment.getProviderId());
//            return false;
//
//        } catch (Exception e) {
//            log.error("Error checking appointment access: {}", e.getMessage(), e);
//            return false;
//        }
//    }
//
//
//    public boolean canModifyAppointment(String appointmentId) {
//        try {
//            String currentUsername = getCurrentUsername();
//            if (currentUsername == null) return false;
//
//            UserDto currentUser = userServiceClient.getUserByUsername(currentUsername);
//            if (currentUser == null) return false;
//
//            if (isAdmin(currentUser)) return true;
//
//            Optional<AppointmentEntity> appointmentOpt = appointmentRepository.findById(appointmentId);
//            if (appointmentOpt.isEmpty()) return false;
//
//            AppointmentEntity appointment = appointmentOpt.get();
//            boolean isRequester = appointment.getRequesterId() != null &&
//                    appointment.getRequesterId().equals(currentUser.getId());
//
//            log.debug("User {} modify access to appointment {}: {}",
//                    currentUsername, appointmentId, isRequester);
//
//            return isRequester;
//
//        } catch (Exception e) {
//            log.error("Error checking appointment modification access: {}", e.getMessage());
//            return false;
//        }
//    }
//
//
//    public boolean canAccessUserAppointments(String userId) {
//        try {
//            String currentUsername = getCurrentUsername();
//            if (currentUsername == null) return false;
//
//            UserDto currentUser = userServiceClient.getUserByUsername(currentUsername);
//            if (currentUser == null) return false;
//
//            if (isAdmin(currentUser)) return true;
//
//            // STUDENT can only access their own appointments
//            boolean isSameUser = String.valueOf(currentUser.getId()).equals(userId);
//            log.debug("User {} accessing appointments for user {}: {}",
//                    currentUsername, userId, isSameUser);
//            return isSameUser;
//
//        } catch (Exception e) {
//            log.error("Error checking user appointments access: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    public boolean canAccessRequesterAppointments(String requesterId) {
//        return canAccessUserAppointments(requesterId);
//    }
//
//    public boolean canAccessProviderAppointments(String providerId) {
//        return canAccessUserAppointments(providerId);
//    }
//
//
//    private String getCurrentUsername() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return null;
//        }
//        return authentication.getName();
//    }
//
//
//    private boolean isAdmin(UserDto user) {
//        if (user.getRole() == null) return false;
//        String role = user.getRole().trim().toUpperCase();
//        return role.equals("ADMIN") || role.equals("ROLE_ADMIN");
//    }
//
//
//    public boolean isCurrentUserAdmin() {
//        try {
//            String currentUsername = getCurrentUsername();
//            if (currentUsername == null) return false;
//
//            UserDto currentUser = userServiceClient.getUserByUsername(currentUsername);
//            return currentUser != null && isAdmin(currentUser);
//
//        } catch (Exception e) {
//            log.error("Error checking if user is admin: {}", e.getMessage());
//            return false;
//        }
//    }
//
//
//    public boolean canAccessStatistics(String userId) {
//        try {
//            String currentUsername = getCurrentUsername();
//            if (currentUsername == null) return false;
//
//            UserDto currentUser = userServiceClient.getUserByUsername(currentUsername);
//            if (currentUser == null) return false;
//
//            if (isAdmin(currentUser)) return true;
//
//            boolean isSameUser = String.valueOf(currentUser.getId()).equals(userId);
//            log.debug("User {} accessing statistics for user {}: {}",
//                    currentUsername, userId, isSameUser);
//            return isSameUser;
//
//        } catch (Exception e) {
//            log.error("Error checking statistics access: {}", e.getMessage());
//            return false;
//        }
//    }
//}


// TODO : Implement security checks

package com.example.appointmentservice.business.client;

import com.example.appointmentservice.domain.dto.UserDto;
import com.example.appointmentservice.persistence.model.AppointmentEntity;
import com.example.appointmentservice.persistence.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;


// Authorization Rules:
// * - ADMIN: Can access and modify any appointment
// * - REQUESTER: Can access and modify their own requested appointments
// * - PROVIDER: Can access (but not modify) appointments where they are the provider
// * - STUDENT: Can only access their own appointments (as requester or provider)
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentSecurityService {

    private final AppointmentRepository appointmentRepository;
    private final UserServiceClient userServiceClient;


    //Check if the current user can access (view) a specific appointment
    // User is the requester of the appointment
    //    User is the provider of the appointment
    public boolean canAccessAppointment(String appointmentId) {
        try {
            String currentUsername = getCurrentUsername();
            log.info("=== AUTHORIZATION CHECK ===");
            log.info("Current username from JWT: {}", currentUsername);

            if (currentUsername == null) {
                log.warn("No authenticated user found");
                return false;
            }

            // Get user details from User Service
            UserDto currentUser = userServiceClient.getUserByUsername(currentUsername);
            if (currentUser == null) {
                log.warn("User not found in User Service: {}", currentUsername);
                return false;
            }

            log.info("Current user ID: {}, Role: {}", currentUser.getId(), currentUser.getRole());

            // ADMIN can access any appointment
            if (isAdmin()) {
                log.info("✓ Admin user {} accessing appointment {}", currentUsername, appointmentId);
                return true;
            }

            // Get the appointment entity
            Optional<AppointmentEntity> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (appointmentOpt.isEmpty()) {
                log.warn("Appointment not found: {}", appointmentId);
                return false;
            }

            AppointmentEntity appointment = appointmentOpt.get();
            log.info("Appointment requesterId: {}, providerId: {}",
                    appointment.getRequesterId(), appointment.getProviderId());

            // Allow if requester or provider matches
            boolean isRequester = appointment.getRequesterId() != null &&
                    appointment.getRequesterId().equals(currentUser.getId());
            boolean isProvider = appointment.getProviderId() != null &&
                    appointment.getProviderId().equals(currentUser.getId());

            log.info("Access check - isRequester: {}, isProvider: {}", isRequester, isProvider);

            if (isRequester || isProvider) {
                log.info("✓ User {} can access appointment {} (requester: {}, provider: {})",
                        currentUsername, appointmentId, isRequester, isProvider);
                return true;
            }

            log.warn("✗ User {} (ID: {}) denied access to appointment {} (requester: {}, provider: {})",
                    currentUsername, currentUser.getId(), appointmentId,
                    appointment.getRequesterId(), appointment.getProviderId());
            return false;

        } catch (Exception e) {
            log.error("Error checking appointment access: {}", e.getMessage(), e);
            return false;
        }
    }


    //Modification is granted if:
    //      - User is ADMIN
    //      - User is the requester of the appointment (requesters can modify their appointments)
    //
    public boolean canModifyAppointment(String appointmentId) {
        try {
            String currentUsername = getCurrentUsername();
            if (currentUsername == null) return false;

            // ADMIN can modify any appointment
            if (isAdmin()) {
                log.info("✓ Admin user {} can modify appointment {}", currentUsername, appointmentId);
                return true;
            }

            UserDto currentUser = userServiceClient.getUserByUsername(currentUsername);
            if (currentUser == null) return false;

            Optional<AppointmentEntity> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (appointmentOpt.isEmpty()) return false;

            AppointmentEntity appointment = appointmentOpt.get();

            // Only the requester can modify the appointment
            boolean isRequester = appointment.getRequesterId() != null &&
                    appointment.getRequesterId().equals(currentUser.getId());

            log.debug("User {} modify access to appointment {}: {}",
                    currentUsername, appointmentId, isRequester);

            return isRequester;

        } catch (Exception e) {
            log.error("Error checking appointment modification access: {}", e.getMessage());
            return false;
        }
    }


    // Access is granted if:
    //     * - User is ADMIN
    //     * - User is requesting their own appointments
    public boolean canAccessUserAppointments(String userId) {
        try {
            String currentUsername = getCurrentUsername();
            if (currentUsername == null) return false;

            // ADMIN can access any user's appointments
            if (isAdmin()) {
                log.info("✓ Admin user {} accessing appointments for user {}", currentUsername, userId);
                return true;
            }

            UserDto currentUser = userServiceClient.getUserByUsername(currentUsername);
            if (currentUser == null) return false;

            // Users can only access their own appointments
            boolean isSameUser = String.valueOf(currentUser.getId()).equals(userId);
            log.debug("User {} accessing appointments for user {}: {}",
                    currentUsername, userId, isSameUser);
            return isSameUser;

        } catch (Exception e) {
            log.error("Error checking user appointments access: {}", e.getMessage());
            return false;
        }
    }
    ///Check if the current user can access appointments where they are the requester

    public boolean canAccessRequesterAppointments(String requesterId) {
        return canAccessUserAppointments(requesterId);
    }


      ///Check if the current user can access appointments where they are the provider

    public boolean canAccessProviderAppointments(String providerId) {
        return canAccessUserAppointments(providerId);
    }


     // Check if the current user can access statistics for a specific user

    public boolean canAccessStatistics(String userId) {
        try {
            String currentUsername = getCurrentUsername();
            if (currentUsername == null) return false;

            // ADMIN can access any user's statistics
            if (isAdmin()) {
                log.info("✓ Admin user {} accessing statistics for user {}", currentUsername, userId);
                return true;
            }

            UserDto currentUser = userServiceClient.getUserByUsername(currentUsername);
            if (currentUser == null) return false;

            // Users can only access their own statistics
            boolean isSameUser = String.valueOf(currentUser.getId()).equals(userId);
            log.debug("User {} accessing statistics for user {}: {}",
                    currentUsername, userId, isSameUser);
            return isSameUser;

        } catch (Exception e) {
            log.error("Error checking statistics access: {}", e.getMessage());
            return false;
        }
    }


    // Check if the current user has ADMIN role
    public boolean isCurrentUserAdmin() {
        return isAdmin();
    }

    ///Check if the current user has ADMIN role
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }



    // KEYCLOAK INTEGRATION: Checks Spring Security authorities
    //  for ROLE_ADMIN (which comes from Keycloak JWT token)
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Check if user has ROLE_ADMIN authority
        boolean hasAdminRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority ->
                        authority.equals("ROLE_ADMIN") ||
                                authority.equals("ADMIN")
                        || authority.equals("ROLE_PROPERTY_MANAGER") ||
                        authority.equals("PROPERTY_MANAGER")
                        || authority.equals("ROLE_STUDENT") ||
                        authority.equals("STUDENT")
                );

        log.debug("Admin check for user {}: {}", authentication.getName(), hasAdminRole);
        return hasAdminRole;
    }
}












