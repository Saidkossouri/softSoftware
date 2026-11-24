package fr.carsupervision.administration.member;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    /** Recherche par email déjà normalisé. */
    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    /**
     * Verrouille les lignes du rôle demandé ({@code SELECT ... FOR UPDATE}) pour garantir la
     * règle « au moins un ADMIN » même en cas de modifications concurrentes (research R9).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.role = :role")
    List<Member> findAllByRoleForUpdate(Role role);
}
