package com.mini.fintech.wallet_app.reversal.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReversalRequestDTO {

	@NotBlank(message = "reason is required")
	private String reason;
}