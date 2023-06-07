package utility.microservices.usersservice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomUserRepository extends JpaRepository<CustomUser, Long> {
    
    boolean existsByRole(String role);
    CustomUser findByRole(String role);

}
