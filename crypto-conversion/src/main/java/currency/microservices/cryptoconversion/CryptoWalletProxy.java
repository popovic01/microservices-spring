package currency.microservices.cryptoconversion;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import currency.microservices.cryptoconversion.dtos.CryptoWalletDto;
import currency.microservices.cryptoconversion.dtos.CryptoWalletResponseDto;

@FeignClient(name = "crypto-wallet")
public interface CryptoWalletProxy {
    
    @PostMapping("/crypto-wallet/conversion")
    public ResponseEntity<CryptoWalletResponseDto> walletConversion(CryptoWalletDto walletDto);
}
