package com.carsale.erp.account;

import com.carsale.erp.shared.utils.Utils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.security.CustomUserDetails;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void updateProfile(
            User current,
            String fullName,
            String email,
            String currentPassword,
            String newPassword,
            String confirmPassword
    ) {
        if (current == null || current.getId() == null) {
            throw new IllegalArgumentException("Sign in again to update your profile.");
        }
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));

        String name = Utils.trimToNull(fullName);
        if (name == null) {
            throw new IllegalArgumentException("Full name is required.");
        }
        String mail = Utils.trimToNull(email);
        if (mail == null) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!mail.contains("@") || mail.length() < 5) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        User other = userRepository.findByEmailIgnoreCase(mail).orElse(null);
        if (other != null && !other.getId().equals(user.getId())) {
            throw new IllegalArgumentException("That email is already in use.");
        }

        user.setFullName(name);
        user.setEmail(mail);

        boolean changingPassword = !Utils.isBlank(newPassword) || !Utils.isBlank(confirmPassword);
        if (changingPassword) {
            if (Utils.isBlank(currentPassword) || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            if (Utils.isBlank(newPassword) || newPassword.length() < 8) {
                throw new IllegalArgumentException("New password must be at least 8 characters.");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("New password and confirmation do not match.");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        User saved = userRepository.save(user);
        refreshAuthentication(saved);
    }

    private void refreshAuthentication(User saved) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current == null) {
            return;
        }
        CustomUserDetails details = new CustomUserDetails(saved);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                details, current.getCredentials(), details.getAuthorities());
        token.setDetails(current.getDetails());
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
