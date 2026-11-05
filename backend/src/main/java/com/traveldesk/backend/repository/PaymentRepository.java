
package com.traveldesk.backend.repository;

import com.traveldesk.backend.entity.Payment;
import com.traveldesk.backend.enums.PaymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBooking_Id(Long bookingId);
    Optional<Payment> findByTransactionReference(String reference);
    List<Payment> findByPaymentStatus(PaymentStatus status);
}
