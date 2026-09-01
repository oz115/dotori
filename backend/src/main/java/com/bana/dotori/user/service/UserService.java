package com.bana.dotori.user.service;

import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;

public interface UserService {

	SignupResponse signup(SignupRequest request);
}
