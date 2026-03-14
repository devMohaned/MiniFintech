package com.mini.fintech.wallet_app.wallet.service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletAmountRequestDTO {

	@NotNull(message = "amount is required")
	@DecimalMin(value = "0.0001", inclusive = true, message = "amount must be greater than zero")
	private BigDecimal amount;

	@NotBlank(message = "currency is required")
	@Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter uppercase code")
	private String currency;

	@NotBlank(message = "reference is required")
	private String reference;

	private String description;
}