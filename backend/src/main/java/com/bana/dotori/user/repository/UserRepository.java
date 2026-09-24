package com.bana.dotori.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.user.entity.User;

import jakarta.persistence.LockModeType;

/**
 * 아이디·이메일 조회는 전부 살아있는 회원(deleted_at IS NULL)만 본다.
 * 탈퇴는 소프트 삭제라 같은 아이디·이메일의 탈퇴 행이 남아 있을 수 있고,
 * DB 유니크도 부분 인덱스(UX_USERS_*_ACTIVE)라 살아있는 행끼리만 유일하다.
 * 필터 없이 조회하면 탈퇴 행이 중복 판정을 만들거나 단건 조회를 깨뜨린다.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

	boolean existsByLoginIdAndDeletedAtIsNull(String loginId);

	boolean existsByEmailAndDeletedAtIsNull(String email);

	Optional<User> findByLoginIdAndDeletedAtIsNull(String loginId);

	/**
	 * 아이디 찾기(AUTH-05)용. 저장된 이메일을 소문자로 환산해 대조한다 —
	 * 정규화 이전에 만들어졌거나 다른 경로로 들어온 대문자 이메일 행도 찾도록.
	 *
	 * @param email 소문자 정규화된 이메일
	 */
	@Query("select u from User u where u.name = :name and lower(u.email) = :email and u.deletedAt is null")
	Optional<User> findByNameAndEmail(@Param("name") String name, @Param("email") String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :userId")
	Optional<User> findByIdForUpdate(@Param("userId") Integer userId);
}
