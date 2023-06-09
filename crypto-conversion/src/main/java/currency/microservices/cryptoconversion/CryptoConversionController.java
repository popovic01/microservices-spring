package currency.microservices.cryptoconversion;

import java.math.BigDecimal;
import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
public class CryptoConversionController {

    //localhost:8100/crypto-conversion?from=EUR&to=RSD&quantity=50 - request example
	@GetMapping("/crypto-conversion") //query params
	public ResponseEntity<?> getConversionParams
        (@RequestParam String from, @RequestParam String to, @RequestParam(defaultValue = "10") double quantity) {

		HashMap<String,String> uriVariables = new HashMap<String,String>(); //we need this for sending request to crypto-exchange microservice
		uriVariables.put("from", from);
		uriVariables.put("to", to);

        try {
            //without feign
            ResponseEntity<CryptoConversion> response = 
                new RestTemplate().
                getForEntity("http://localhost:8400/crypto-exchange/from/{from}/to/{to}",
                CryptoConversion.class, uriVariables);

                CryptoConversion responseBody = response.getBody(); //the response contains ConversionMultiple and Environment
                
            return ResponseEntity.status(HttpStatus.OK).body(new CryptoConversion(from, to, responseBody.getConversionMultiple(), responseBody.getEnvironment(),
                responseBody.getConversionMultiple().multiply(BigDecimal.valueOf(quantity)), quantity));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
        }
	}
    
}
