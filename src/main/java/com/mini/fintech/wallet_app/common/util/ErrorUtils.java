package com.mini.fintech.wallet_app.common.util;

import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

public class ErrorUtils {

	private ErrorUtils() {
	}

	public static String formatError(ObjectError error) {
		if (error instanceof FieldError fieldError) {
			return fieldError.getField() + ": " + fieldError.getDefaultMessage();
		}
		return error.getDefaultMessage();
	}
}
