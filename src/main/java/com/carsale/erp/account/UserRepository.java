package com.carsale.erp.account;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.account.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);
}
