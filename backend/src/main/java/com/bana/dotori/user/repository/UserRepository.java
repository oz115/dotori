package com.bana.dotori.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.user.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {

	boolean existsByLoginId(String loginId);

	boolean existsByEmail(String email);

	Optional<User> findByLoginId(String loginId);
}
