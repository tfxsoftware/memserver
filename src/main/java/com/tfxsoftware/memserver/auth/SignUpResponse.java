package com.tfxsoftware.memserver.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class SignUpResponse {
	@Getter
	private String id;
	@Getter
	private String email;
	@Getter
	private String username;
}
