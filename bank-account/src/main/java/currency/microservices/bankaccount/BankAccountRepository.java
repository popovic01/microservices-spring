package currency.microservices.bankaccount;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    
    boolean existsByEmail(String email);
    BankAccount findByEmail(String email);
}
