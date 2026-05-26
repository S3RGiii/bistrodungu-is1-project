package com.bistrodungu.identity.application.port.in;

/**
 * Authenticate User Use Case - Input Port
 */
public interface AuthenticateUserUseCase {
    AuthenticateUserResult execute(AuthenticateUserCommand command);

    record AuthenticateUserCommand(
            String tenantId,
            String email,
            String password
    ) {}

    record AuthenticateUserResult(
            String userId,
            String email,
            String fullName,
            String role,
            String token
    ) {}
}
