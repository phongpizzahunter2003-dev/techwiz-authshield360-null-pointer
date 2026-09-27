package com.authshield360.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmMfaRequest(
        @NotBlank(message = "Mã xác nhận không được để trống.")
        @Pattern(regexp = "[0-9]{6}", message = "Mã xác nhận phải bao gồm 6 chữ số.")
        String code
) {
}
