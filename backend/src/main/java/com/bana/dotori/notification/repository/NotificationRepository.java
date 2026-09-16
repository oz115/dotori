package com.bana.dotori.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer>,
		JpaSpecificationExecutor<Notification> {

	/**
	 * 본인의 안 읽은 알림 전부를 한 번의 UPDATE 로 읽음 처리한다 (NOTI-04). 엔티티를
	 * 하나씩 불러와 markAsRead() 를 반복 호출하지 않는다 — 대상이 몇 건이든 쿼리 한 번.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Notification n set n.read = true where n.user.id = :userId and n.read = false")
	int markAllAsRead(@Param("userId") Integer userId);
}
