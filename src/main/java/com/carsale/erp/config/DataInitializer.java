package com.carsale.erp.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.carsale.erp.entity.Role;
import com.carsale.erp.entity.RoleName;
import com.carsale.erp.entity.User;
import com.carsale.erp.repository.RoleRepository;
import com.carsale.erp.repository.UserRepository;

@Component
@Order(2)
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Role userRole = getOrCreateRole(RoleName.ROLE_USER);
        Role adminRole = getOrCreateRole(RoleName.ROLE_ADMIN);
        Role superAdminRole = getOrCreateRole(RoleName.ROLE_SUPER_ADMIN);

        createUserIfMissing(
                "gihan",
                "Gihan@123",
                "Gihan Kadawathage",
                "gihan@carerp.local",
                new HashSet<>(Arrays.asList(userRole, adminRole, superAdminRole))
        );
        createUserIfMissing(
                "admin",
                "Admin@123",
                "Nimal Perera",
                "admin@carerp.local",
                new HashSet<>(Arrays.asList(userRole, adminRole))
        );
        createUserIfMissing(
                "staff",
                "User@123",
                "Kasun Silva",
                "staff@carerp.local",
                new HashSet<>(Collections.singletonList(userRole))
        );
    }

    private Role getOrCreateRole(RoleName name) {
        Role existing = roleRepository.findByName(name).orElse(null);
        if (existing != null) {
            return existing;
        }
        return roleRepository.save(new Role(name));
    }

    private void createUserIfMissing(String username, String rawPassword, String fullName,
                                     String email, HashSet<Role> roles) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setEnabled(true);
        user.setRoles(roles);
        userRepository.save(user);
    }
}
