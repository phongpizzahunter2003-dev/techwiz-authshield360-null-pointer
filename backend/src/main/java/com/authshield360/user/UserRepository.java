package com.authshield360.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRole(RoleType role);

    long countByStatus(UserStatus status);

    java.util.List<User> findByRoleOrderByUsernameAsc(RoleType role);

    @Query("select u from User u where "
            + "(:role is null or u.role = :role) and "
            + "(:q is null or lower(u.username) like lower(concat('%', :q, '%')) "
            + " or lower(u.email) like lower(concat('%', :q, '%')) "
            + " or lower(coalesce(u.fullName,'')) like lower(concat('%', :q, '%')))")
    Page<User> search(@Param("role") RoleType role, @Param("q") String q, Pageable pageable);
}
