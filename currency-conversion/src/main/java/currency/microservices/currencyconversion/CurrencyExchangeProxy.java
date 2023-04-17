package currency.microservices.currencyconversion;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "currency-exchange")
public interface CurrencyExchangeProxy {
    
    @GetMapping("/currency-exchange/from/{from}/to/{to}") //when we call this method, the method from currency-exchange microservice will be executed
	public ResponseEntity<CurrencyConversion> getExchange(@PathVariable String from, @PathVariable String to);
}
