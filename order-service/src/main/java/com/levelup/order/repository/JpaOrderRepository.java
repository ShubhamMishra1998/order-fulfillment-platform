package com.levelup.order.repository;

import com.levelup.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaOrderRepository extends JpaRepository<Order,String>, OrderRepository {
}
