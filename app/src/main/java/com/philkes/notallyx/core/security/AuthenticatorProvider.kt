package com.philkes.notallyx.core.security

object AuthenticatorProvider {
    var instance: BiometricAuthenticator = SystemBiometricAuthenticator()

    fun reset() {
        instance = SystemBiometricAuthenticator()
    }
}
