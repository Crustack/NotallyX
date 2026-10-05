package com.philkes.notallyx.utils.security

import androidx.fragment.app.FragmentActivity
import com.philkes.notallyx.test.FakeBiometricAuthenticator
import javax.crypto.Cipher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class AuthenticatorProviderTest {

    private val activity: FragmentActivity = mock(FragmentActivity::class.java)

    @Before
    fun setUp() {
        AuthenticatorProvider.reset()
    }

    @After
    fun tearDown() {
        AuthenticatorProvider.reset()
    }

    @Test
    fun defaultInstance_isSystemBiometricAuthenticator() {
        assertTrue(AuthenticatorProvider.instance is SystemBiometricAuthenticator)
    }

    @Test
    fun setInstanceAndReset_restoresSystemBiometricAuthenticator() {
        val fake = FakeBiometricAuthenticator()
        AuthenticatorProvider.instance = fake
        assertSame(fake, AuthenticatorProvider.instance)

        AuthenticatorProvider.reset()
        assertTrue(AuthenticatorProvider.instance is SystemBiometricAuthenticator)
    }

    @Test
    fun fakeBiometricAuthenticator_whenShouldSucceedTrue_invokesOnSuccessWithCipher() {
        val fake = FakeBiometricAuthenticator()
        val dummyCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        fake.cipher = dummyCipher

        var receivedCipher: Cipher? = null
        var errorCalled = false

        fake.authenticate(
            activity,
            ByteArray(1),
            isForDecrypt = true,
            onSuccess = { cipher -> receivedCipher = cipher },
            onError = { errorCalled = true },
        )

        assertNotNull(receivedCipher)
        assertSame(dummyCipher, receivedCipher)
        assertFalse(errorCalled)
    }

    @Test
    fun fakeBiometricAuthenticator_whenShouldSucceedFalse_invokesOnError() {
        val fake = FakeBiometricAuthenticator()
        fake.shouldSucceed = false

        var successCalled = false
        var errorMessage: String? = null

        fake.authenticate(
            activity,
            ByteArray(1),
            onSuccess = { successCalled = true },
            onError = { msg -> errorMessage = msg },
        )

        assertFalse(successCalled)
        assertEquals("Authentication failed", errorMessage)
    }

    @Test
    fun authenticatorProvider_withFake_routesAuthenticateCalls() {
        val fake = FakeBiometricAuthenticator()
        val dummyCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        fake.cipher = dummyCipher
        AuthenticatorProvider.instance = fake

        var successCalled = false

        AuthenticatorProvider.instance.authenticate(
            activity,
            ByteArray(1),
            onSuccess = { successCalled = true },
            onError = {},
        )

        assertTrue(successCalled)
    }
}
