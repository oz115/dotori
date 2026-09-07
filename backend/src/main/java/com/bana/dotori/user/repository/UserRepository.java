package com.bana.dotori.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.user.entity.User;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Integer> {

	boolean existsByLoginId(String loginId);

	boolean existsByEmail(String email);

	Optional<User> findByLoginId(String loginId);

	/**
	 * 아이디 찾기(AUTH-05)용. 저장된 이메일을 소문자로 환산해 대조한다 —
	 * 정규화 이전에 만들어졌거나 다른 경로로 들어온 대문자 이메일 행도 찾도록.
	 *
	 * @param email 소문자 정규화된 이메일
	 */
	@Query("select u from User u where u.name = :name and lower(u.email) = :email")
	Optional<User> findByNameAndEmail(@Param("name") String name, @Param("email") String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :userId")
	Optional<User> findByIdForUpdate(@Param("userId") Integer userId);
}
