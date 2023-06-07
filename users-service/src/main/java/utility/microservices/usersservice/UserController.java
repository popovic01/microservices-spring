package utility.microservices.usersservice;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    
    @Autowired
	private CustomUserRepository repo;

	@GetMapping("/users-service/users")
	public List<CustomUser> getAllUsers(){
		return repo.findAll();
	}

	@PostMapping("/users-service/users")
	public ResponseEntity<?> createUser(@RequestBody CustomUser user) {
		if (user.getRole().equals("OWNER")) {
			boolean ownerExists = repo.existsByRole("OWNER");
			if (ownerExists) {
				return ResponseEntity.status(409).body("Already exists user with OWNER role");
			}
		}
		CustomUser createdUser = repo.save(user);
		return ResponseEntity.status(201).body(createdUser);
	}

	@PutMapping("/users-service/users")
	public ResponseEntity<?> editUser(@RequestBody CustomUser user) {
		if (repo.existsById(user.getId())) {
			if (user.getRole().equals("OWNER") && user.getId() != repo.findByRole("OWNER").getId()) {
				return ResponseEntity.status(409).body("Already exists user with OWNER role");
			}
			repo.save(user);
			return new ResponseEntity<CustomUser>(HttpStatus.OK);
		}
		return new ResponseEntity<CustomUser>(HttpStatus.NO_CONTENT);
	}

	@DeleteMapping("/users-service/users/{id}")
	public ResponseEntity<CustomUser> deleteUser(@PathVariable("id") Long id) {
		if (repo.existsById(id)) {
			repo.deleteById(id);
			return new ResponseEntity<CustomUser>(HttpStatus.OK);
		}
		return new ResponseEntity<CustomUser>(HttpStatus.NO_CONTENT);
	}
}
