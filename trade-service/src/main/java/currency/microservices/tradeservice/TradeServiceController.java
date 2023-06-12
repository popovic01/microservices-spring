package currency.microservices.tradeservice;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import currency.microservices.tradeservice.dtos.WalletAccountDto;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import currency.microservices.tradeservice.dtos.TradeServiceDto;

@RestController
public class TradeServiceController {
    
    @Autowired //dependency injection
    private TradeServiceRepository repo;

    @Autowired
    private BankAccountProxy bankAccountProxy;

    @Autowired
    private CryptoWalletProxy cryptoWalletProxy;

    @Autowired
    private CurrencyExchangeProxy currencyExchangeProxy;
    
    //localhost:8600/trade-service?from=BTC&to=EUR&quantity=0.5 - request example
    @GetMapping("/trade-service")
    @RateLimiter(name = "default")
    public ResponseEntity<?> getExchange(@RequestParam String from, @RequestParam String to, @RequestParam(defaultValue = "10") double quantity, 
        @RequestHeader("Authorization") String authorization) {

        TradeServiceDto request = new TradeServiceDto("", from, "", to, BigDecimal.valueOf(0), BigDecimal.valueOf(quantity));

        request.setFromActual(request.getFrom());        
        request.setToActual(request.getTo());
        request.setQuantityActual(request.getQuantity());

        TradeService kurs;

        if (request.getFrom().toUpperCase().equals("GBP") 
            || request.getFrom().toUpperCase().equals("CHF") 
            || request.getFrom().toUpperCase().equals("RSD")) {
            
            // convert to eur and then to crypto

            // send request to currency-exchange microservice
            ResponseEntity<WalletAccountDto> response = currencyExchangeProxy.getExchange(request.getFrom().toUpperCase(), "EUR"); 
                
            request.setFrom(response.getBody().getTo());
            request.setQuantity(response.getBody().getToValue().multiply(request.getQuantity()));
        }
        else if (request.getTo().toUpperCase().equals("GBP") 
            || request.getTo().toUpperCase().equals("CHF") 
            || request.getTo().toUpperCase().equals("RSD")) {

            kurs = repo.findByFromAndToIgnoreCase(request.getFrom().toUpperCase(), "EUR"); 

            // convert to eur and then to fiat

            // send request to currency-exchange microservice
            ResponseEntity<WalletAccountDto> response = currencyExchangeProxy.getExchange("EUR", request.getTo());
                
            request.setTo(response.getBody().getFrom());
            request.setQuantity(response.getBody().getToValue().multiply(kurs.getToValue()));
        }

        kurs = repo.findByFromAndToIgnoreCase(request.getFrom().toLowerCase(), request.getTo().toLowerCase()); //find this in database, based on from and to values

        if (kurs != null) {

            String email = getEmail(authorization);

            // crypto to fiat
            if (request.getFrom().toLowerCase().equals("btc") || request.getFrom().toLowerCase().equals("eth")
                || request.getFrom().toLowerCase().equals("bnb") || request.getFrom().toLowerCase().equals("ada")) {

                return ResponseEntity.status(200).body(cryptoToFiat(request, email).getBody());             
            }
            // fiat to crypto
            else if (request.getFrom().toLowerCase().equals("eur") || request.getFrom().toLowerCase().equals("usd"))
            {
                request.setQuantity(kurs.getToValue());
                return ResponseEntity.status(200).body(fiatToCrypto(request, email).getBody());
            }
        } 
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Requested exchange could not be found");
    }

    private ResponseEntity<?> fiatToCrypto(TradeServiceDto request, String email)
    {
        // call bank account service, check if there is enough money and update bank account
        WalletAccountDto requestDto = new WalletAccountDto(email, request.getFromActual(), request.getTo(), request.getQuantityActual(), 
            repo.findByFromAndToIgnoreCase(request.getFrom().toLowerCase(), request.getTo().toLowerCase()).getToValue().multiply(request.getQuantityActual()));

        // call crypto wallet service, check if there is enough money and update wallet  
        bankAccountProxy.conversion(requestDto);
                            
        ResponseEntity<?> responseWallet = cryptoWalletProxy.conversion(requestDto);
 
        return responseWallet;
    }

    private ResponseEntity<?> cryptoToFiat(TradeServiceDto request, String email)
    {
        WalletAccountDto requestDto = new WalletAccountDto(email, request.getFrom(), request.getToActual(), request.getQuantityActual(), 
            repo.findByFromAndToIgnoreCase(request.getFrom().toLowerCase(), request.getTo().toLowerCase()).getToValue().multiply(request.getQuantityActual()));

        // call crypto wallet service, check if there is enough money and update wallet  
    cryptoWalletProxy.conversion(requestDto);
                            
        ResponseEntity<?> responseAccount = bankAccountProxy.conversion(requestDto);  

        return responseAccount;
    }

    private String getEmail(String authorization) {
        // Extract the username and password from the Authorization header
        String base64Credentials = authorization.substring("Basic".length()).trim();
        byte[] decoded = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(decoded, StandardCharsets.UTF_8);
        String[] emailPassword = credentials.split(":", 2);
        String email = emailPassword[0];
        return email;
	}

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<String> rateLimiterExceptionHandler(RequestNotPermitted ex) {
        return ResponseEntity.status(503).body("Trade service can only serve up to 2 requests every 45 seconds");
    }
}
