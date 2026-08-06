package EventTicketing.repository;

import EventTicketing.model.Event;
import EventTicketing.model.SeatCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatCategoryRepository extends JpaRepository<SeatCategory, UUID> {

    List<SeatCategory> findByEvent(Event event);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sc from SeatCategory sc where sc.id = :id")
    Optional<SeatCategory> findByIdWithLock(@Param("id") UUID id);
}
