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

import dtos.BankAccountDto;
import dtos.BankAccountResponseDto;

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

    @PostMapping("/bank-account/conversion")
    public ResponseEntity<?> conversion(@RequestBody BankAccountDto account) {

        if (!repo.existsByEmail(account.getEmail()))
		    return ResponseEntity.status(400).body("Bank account with email " + account.getEmail() + " doesn't exist");

        BankAccount accountDb = repo.findByEmail(account.getEmail());

        switch (account.getFrom()) {
            case "EUR": {
                if (repo.findByEmail(account.getEmail()).getEur().compareTo(account.getFromValue()) == -1)
		            return ResponseEntity.status(400).body("You don't have enough " + account.getFrom() + " for the exchange");
                else {
                    accountDb.setEur(repo.findByEmail(account.getEmail()).getEur().subtract(account.getFromValue()));
                }
                break;
            }
            case "USD": {
                if (repo.findByEmail(account.getEmail()).getUsd().compareTo(account.getFromValue()) == -1)
		            return ResponseEntity.status(400).body("You don't have enough " + account.getFrom() + " for the exchange");
                else {
                    accountDb.setUsd(repo.findByEmail(account.getEmail()).getUsd().subtract(account.getFromValue()));
                }
                break;
            }
            case "CHF": {
                if (repo.findByEmail(account.getEmail()).getChf().compareTo(account.getFromValue()) == -1)
		            return ResponseEntity.status(400).body("You don't have enough " + account.getFrom() + " for the exchange");
                else {
                    accountDb.setChf(repo.findByEmail(account.getEmail()).getChf().subtract(account.getFromValue()));
                }
                break;
            }
            case "GBP": {
                if (repo.findByEmail(account.getEmail()).getGbp().compareTo(account.getFromValue()) == -1)
		            return ResponseEntity.status(400).body("You don't have enough " + account.getFrom() + " for the exchange");
                else {
                    accountDb.setGbp(repo.findByEmail(account.getEmail()).getGbp().subtract(account.getFromValue()));
                }
                break;
            }
            case "RSD": {
                if (repo.findByEmail(account.getEmail()).getRsd().compareTo(account.getFromValue()) == -1)
		            return ResponseEntity.status(400).body("You don't have enough " + account.getFrom() + " for the exchange");
                else {
                    accountDb.setRsd(repo.findByEmail(account.getEmail()).getRsd().subtract(account.getFromValue()));
                }
                break;
            }
        }

        switch (account.getTo()) {
            case "EUR": {
                accountDb.setEur(repo.findByEmail(account.getEmail()).getEur().add(account.getToValue()));
                break;
            }
            case "USD": {
                accountDb.setUsd(repo.findByEmail(account.getEmail()).getUsd().add(account.getToValue()));
                break;
            }
            case "CHF": {
                accountDb.setChf(repo.findByEmail(account.getEmail()).getChf().add(account.getToValue()));
                break;
            }
            case "GBP": {
                accountDb.setGbp(repo.findByEmail(account.getEmail()).getGbp().add(account.getToValue()));
                break;
            }
            case "RSD": {
                accountDb.setRsd(repo.findByEmail(account.getEmail()).getRsd().add(account.getToValue()));
                break;
            }
        }

        repo.save(accountDb);

        BankAccountResponseDto responseDto = new BankAccountResponseDto(
            accountDb.getEmail(), 
            accountDb.getEur(), accountDb.getUsd(), accountDb.getRsd(), accountDb.getGbp(), accountDb.getChf(),
            "Successfully exchanged " + account.getFromValue() + account.getFrom() + " to " + account.getTo());

        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
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
                if (repo.existsByEmail(account.getEmail()) && repo.findByEmail(account.getEmail()).getId() != repo.findById(account.getId()).get().getId()) {
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

			HashMap<String, String> uriVariables = new HashMap<String, String>();
            uriVariables.put("email", email);
			new RestTemplate().delete("http://localhost:8900/crypto-wallet/{email}", uriVariables);

			return new ResponseEntity<BankAccount>(HttpStatus.OK);
		}
		return new ResponseEntity<BankAccount>(HttpStatus.NO_CONTENT);
    }

    
	@GetMapping("/bank-account/{email}")
	public ResponseEntity<Boolean> existsByEmail(@PathVariable("email") String email){
		return ResponseEntity.status(200).body(repo.existsByEmail(email));
	}
    
}
