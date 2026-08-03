package EventTicketing.repository;

import EventTicketing.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    
    // ديه للUser والadmin
    @Query("SELECT t FROM Ticket t WHERE t.uuid = :uuid")
    Optional<Ticket> getTicketByUUID(@Param("uuid") UUID uuid);

    
    @Query("SELECT t FROM Ticket t WHERE t.customerId = :customerId")
    Optional<List<Ticket>> getAllTicketsMadeByCustomer(@Param("customer_id") Integer customerId);
    
    // علشان يعمل insert بتستخدم الsave() fuction الي بيقدمها الJpaRepository

    @Query("SELECT t FROM Ticket t WHERE t.userOwnerUUID = :uuid")
    Optional<Ticket> getTicketByOwnerUUID(@Param("user_owner_uuid") UUID uuid);

}
