package com.bana.dotori.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
}
