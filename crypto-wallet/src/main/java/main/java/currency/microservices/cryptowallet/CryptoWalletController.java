package main.java.currency.microservices.cryptowallet;

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
public class CryptoWalletController {
    
    @Autowired //dependency injection
    private CryptoWalletRepository repo;
    
    @GetMapping("/crypto-wallet/wallets")
	public List<CryptoWallet> getAllWallets(){
		return repo.findAll();
	}

    @PostMapping("/crypto-wallet")
    public ResponseEntity<?> addCryptoWallet(@RequestBody CryptoWallet wallet) {

        HashMap<String, String> uriVariables = new HashMap<String, String>();
        uriVariables.put("email", wallet.getEmail());

        // send request to users microservice
        ResponseEntity<Boolean> response = 
            new RestTemplate()
            .getForEntity("http://localhost:8200/bank-account/{email}", Boolean.class, uriVariables);

        // account with email exists
        if (response.getBody()) {
            if (repo.existsByEmail(wallet.getEmail())) {
		        return ResponseEntity.status(409).body("Crypto wallet connected with email " + wallet.getEmail() + " already exists");
            }
            repo.save(wallet);
		    return ResponseEntity.status(201).body(wallet);
        } else {
		    return ResponseEntity.status(400).body("Bank account with email " + wallet.getEmail() + " doesn't exist");
        }
    }

    @PutMapping("/crypto-wallet")
    public ResponseEntity<?> editCryptoWallet(@RequestBody CryptoWallet wallet) {

		if (repo.existsById(wallet.getId())) {
            HashMap<String, String> uriVariables = new HashMap<String, String>();
            uriVariables.put("email", wallet.getEmail());
    
            // send request to users microservice
            ResponseEntity<Boolean> response = 
                new RestTemplate()
                .getForEntity("http://localhost:8200/bank-account/{email}", Boolean.class, uriVariables);
    
            // account with email exists
            if (response.getBody()) {
                if (repo.existsByEmail(wallet.getEmail()) && repo.findByEmail(wallet.getEmail()).getId() != repo.findById(wallet.getId()).get().getId()) {
                    return ResponseEntity.status(409).body("Crypto wallet connected with email " + wallet.getEmail() + " already exists");
                }
                repo.save(wallet);
                return ResponseEntity.status(200).body(wallet);
            } else {
                return ResponseEntity.status(400).body("Bank account with email " + wallet.getEmail() + " doesn't exist");
            }
        } else {
		    return new ResponseEntity<CryptoWallet>(HttpStatus.NO_CONTENT);
        }
    }

    @DeleteMapping("/crypto-wallet/{email}")
    public ResponseEntity<?> deleteCryptoWallet(@PathVariable("email") String email) {
		if (repo.existsByEmail(email)) {
            CryptoWallet wallet = repo.findByEmail(email);
			repo.delete(wallet);
			return new ResponseEntity<CryptoWallet>(HttpStatus.OK);
		}
		return new ResponseEntity<CryptoWallet>(HttpStatus.NO_CONTENT);
    }

}
