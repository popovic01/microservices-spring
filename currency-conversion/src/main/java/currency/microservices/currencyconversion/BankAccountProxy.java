package currency.microservices.currencyconversion;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import currency.microservices.currencyconversion.dtos.BankAccountDto;

@FeignClient(name = "bank-account")
public interface BankAccountProxy {
    
    @PostMapping("/bank-account/conversion")
	public ResponseEntity<?> conversion(@RequestBody BankAccountDto account);
}
