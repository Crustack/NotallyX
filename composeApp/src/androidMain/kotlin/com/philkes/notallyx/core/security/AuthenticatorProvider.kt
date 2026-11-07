package com.philkes.notallyx.core.security

object AuthenticatorProvider {
    var factory: () -> BiometricAuthenticator = { NoOpBiometricAuthenticator() }
    var instance: BiometricAuthenticator = NoOpBiometricAuthenticator()

    fun reset() {
        instance = factory()
    }
}
