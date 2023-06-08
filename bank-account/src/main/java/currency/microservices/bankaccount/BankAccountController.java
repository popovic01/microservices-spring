package currency.microservices.bankaccount;

import java.util.HashMap;
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
import org.springframework.web.client.RestTemplate;

@RestController
public class BankAccountController {

    @Autowired //dependency injection
    private BankAccountRepository repo;

    //localhost:8200/bank-account/accounts - request example
    @GetMapping("/bank-account/accounts")
	public List<BankAccount> getAllAccounts(){
		return repo.findAll();
	}

    @PostMapping("/bank-account")
    public ResponseEntity<?> addBankAccount(@RequestBody BankAccount account) {

        HashMap<String, String> uriVariables = new HashMap<String, String>();
        uriVariables.put("email", account.getEmail());

        // send request to users microservice
        ResponseEntity<Boolean> response = 
            new RestTemplate()
            .getForEntity("http://localhost:8770/users-service/users/{email}", Boolean.class, uriVariables);

        // user with email exists
        if (response.getBody()) {
            if (repo.existsByEmail(account.getEmail())) {
		        return ResponseEntity.status(409).body("Bank account connected with email " + account.getEmail() + " already exists");
            }
            repo.save(account);
		    return ResponseEntity.status(201).body(account);
        } else {
		    return ResponseEntity.status(400).body("User with email " + account.getEmail() + " doesn't exist");
        }
    }

    @PutMapping("/bank-account")
    public ResponseEntity<?> editBankAccount(@RequestBody BankAccount account) {

		if (repo.existsById(account.getId())) {
            HashMap<String, String> uriVariables = new HashMap<String, String>();
            uriVariables.put("email", account.getEmail());
    
            // send request to users microservice
            ResponseEntity<Boolean> response = 
                new RestTemplate()
                .getForEntity("http://localhost:8770/users-service/users/{email}", Boolean.class, uriVariables);
    
            // user with email exists
            if (response.getBody()) {
                if (repo.existsByEmail(account.getEmail())) {
                    return ResponseEntity.status(409).body("Bank account connected with email " + account.getEmail() + " already exists");
                }
                repo.save(account);
                return ResponseEntity.status(200).body(account);
            } else {
                return ResponseEntity.status(400).body("User with email " + account.getEmail() + " doesn't exist");
            }
        } else {
		    return new ResponseEntity<BankAccount>(HttpStatus.NO_CONTENT);
        }
    }

    @DeleteMapping("/bank-account/{email}")
    public ResponseEntity<?> deleteBankAccount(@PathVariable("email") String email) {
		if (repo.existsByEmail(email)) {
            BankAccount account = repo.findByEmail(email);
			repo.delete(account);
			return new ResponseEntity<BankAccount>(HttpStatus.OK);
		}
		return new ResponseEntity<BankAccount>(HttpStatus.NO_CONTENT);
    }
    
}
