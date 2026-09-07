package com.carsale.erp.account;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.account.Role;
import com.carsale.erp.account.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
