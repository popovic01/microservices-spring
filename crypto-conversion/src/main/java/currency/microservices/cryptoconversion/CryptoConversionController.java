package currency.microservices.cryptoconversion;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import currency.microservices.cryptoconversion.dtos.CryptoWalletDto;
import currency.microservices.cryptoconversion.dtos.CryptoWalletResponseDto;

@RestController
public class CryptoConversionController {

    //localhost:8100/crypto-conversion?from=EUR&to=RSD&quantity=50 - request example
	@GetMapping("/crypto-conversion") //query params
	public ResponseEntity<?> getConversionParams
        (@RequestParam String from, @RequestParam String to, @RequestParam(defaultValue = "10") double quantity,
        @RequestHeader("Authorization") String authorization) {

		HashMap<String,String> uriVariables = new HashMap<String,String>(); //we need this for sending request to crypto-exchange microservice
		uriVariables.put("from", from);
		uriVariables.put("to", to);

        String email = getEmail(authorization);

        try {
            //without feign
            ResponseEntity<CryptoConversion> response = 
                new RestTemplate().
                getForEntity("http://localhost:8400/crypto-exchange/from/{from}/to/{to}",
                CryptoConversion.class, uriVariables);

                CryptoConversion responseBody = response.getBody(); //the response contains ToValue and Environment

                // request to bank wallet service
                CryptoWalletDto walletDto = new CryptoWalletDto(email, from, to, BigDecimal.valueOf(quantity),
                    responseBody.getToValue().multiply(BigDecimal.valueOf(quantity)));

                ResponseEntity<CryptoWalletResponseDto> responseWallet = 
                new RestTemplate().
                postForEntity("http://localhost:8900/crypto-wallet/conversion", 
                    walletDto, CryptoWalletResponseDto.class);            
                
            return ResponseEntity.status(HttpStatus.OK).body(responseWallet).getBody();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
        }
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
    
}
