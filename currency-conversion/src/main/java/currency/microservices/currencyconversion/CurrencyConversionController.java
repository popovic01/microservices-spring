package currency.microservices.currencyconversion;

import java.math.BigDecimal;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import feign.FeignException;

@RestController
public class CurrencyConversionController {

    @Autowired
	private CurrencyExchangeProxy proxy;
    
    //localhost:8100/currency-conversion/from/EUR/to/RSD/quantity/100 - request example
	@GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}") //uri params
    public CurrencyConversion getConversion(@PathVariable String from, @PathVariable String to, @PathVariable double quantity) {
        
        HashMap<String, String> uriVariables = new HashMap<String, String>();
        uriVariables.put("from", from);
        uriVariables.put("to", to);

        //send request to currency-exchange microservice
        ResponseEntity<CurrencyConversion> response = 
            new RestTemplate()
            .getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}", CurrencyConversion.class, uriVariables);

        CurrencyConversion cc = response.getBody();

        return new CurrencyConversion(from, to, cc.getConversionMultiple(),
         cc.getEnvironment(), quantity, cc.getConversionMultiple().multiply(BigDecimal.valueOf(quantity)));
    }

    //localhost:8100/currency-conversion?from=EUR&to=RSD&quantity=50 - request example
	@GetMapping("/currency-conversion") //query params
	public ResponseEntity<?> getConversionParams
        (@RequestParam String from, @RequestParam String to, @RequestParam(defaultValue = "10") double quantity) {

		HashMap<String,String> uriVariables = new HashMap<String,String>(); //we need this for sending request to currency-exchange microservice
		uriVariables.put("from", from);
		uriVariables.put("to", to);

        try {
            //without feign
            ResponseEntity<CurrencyConversion> response = 
                new RestTemplate().
                getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
                    CurrencyConversion.class, uriVariables);

            CurrencyConversion responseBody = response.getBody(); //the response contains ConversionMultiple and Environment

            return ResponseEntity.status(HttpStatus.OK).body(new CurrencyConversion(from, to, responseBody.getConversionMultiple(), responseBody.getEnvironment(),
                quantity, responseBody.getConversionMultiple().multiply(BigDecimal.valueOf(quantity))));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
        }
	}

    //localhost:8100/currency-conversion-feign?from=EUR&to=RSD&quantity=50
	@GetMapping("/currency-conversion-feign")
    public ResponseEntity<?> getConversionFeign(@RequestParam String from, @RequestParam String to, @RequestParam double quantity) {
        try {
             ResponseEntity<CurrencyConversion> response = proxy.getExchange(from, to);
             CurrencyConversion responseBody = response.getBody();
             return ResponseEntity.ok(new CurrencyConversion(from, to, responseBody.getConversionMultiple(), responseBody.getEnvironment() + " feign",
                quantity, responseBody.getConversionMultiple().multiply(BigDecimal.valueOf(quantity))));
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body(e.getMessage());
        }
    }

    //handles error if there is missing parameter
    @ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<String> handleMissingParams(MissingServletRequestParameterException ex) {
	    String parameter = ex.getParameterName();
	    return ResponseEntity.status(ex.getStatusCode()).body("Value [" + ex.getParameterType() + "] of parameter [" + parameter + "] has been ommited");
	}
}
