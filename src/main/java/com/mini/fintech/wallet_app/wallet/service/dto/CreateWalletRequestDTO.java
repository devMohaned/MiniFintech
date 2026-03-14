package com.mini.fintech.wallet_app.wallet.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWalletRequestDTO {

	@NotBlank(message = "customerId is required")
	private String customerId;

	@NotBlank(message = "currency is required")
	@Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter uppercase code")
	private String currency;
}
