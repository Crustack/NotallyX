package com.philkes.notallyx.test

import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.KeyGenerator
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object FakeAndroidKeyStore {

    val setup by lazy {
        val provider =
            object : Provider("AndroidKeyStore", 1.0, "Fake Android KeyStore for Tests") {
                init {
                    put("KeyStore.AndroidKeyStore", FakeKeyStore::class.java.name)
                    put("KeyGenerator.AES", FakeAesKeyGenerator::class.java.name)
                }
            }
        // Insert at position 1 so it takes precedence over other providers
        Security.insertProviderAt(provider, 1)
    }

    class FakeKeyStore : KeyStoreSpi() {
        companion object {
            // Shared map across instances to persist keys during test execution
            private val keyMap = ConcurrentHashMap<String, Key>()
            private val fallbackKeyGenerator = KeyGenerator.getInstance("AES").apply { init(256) }
        }

        override fun engineGetKey(alias: String?, password: CharArray?): Key? {
            if (alias == null) return null
            // Return existing key, or generate a dummy AES key on the fly if Tink expects one
            return keyMap.getOrPut(alias) { fallbackKeyGenerator.generateKey() }
        }

        override fun engineSetKeyEntry(
            alias: String?,
            key: Key?,
            password: CharArray?,
            chain: Array<out Certificate>?,
        ) {
            if (alias != null && key != null) {
                keyMap[alias] = key
            }
        }

        override fun engineSetKeyEntry(
            alias: String?,
            key: ByteArray?,
            chain: Array<out Certificate>?,
        ) {
            if (alias != null && key != null) {
                keyMap[alias] = SecretKeySpec(key, "AES")
            }
        }

        override fun engineContainsAlias(alias: String?): Boolean {
            return alias != null && keyMap.containsKey(alias)
        }

        override fun engineIsKeyEntry(alias: String?): Boolean = engineContainsAlias(alias)

        override fun engineDeleteEntry(alias: String?) {
            alias?.let { keyMap.remove(it) }
        }

        override fun engineAliases(): Enumeration<String> = Collections.enumeration(keyMap.keys)

        // No-op overrides required for KeyStoreSpi
        override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit

        override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit

        override fun engineSize(): Int = keyMap.size

        override fun engineIsCertificateEntry(alias: String?): Boolean = false

        override fun engineGetCertificate(alias: String?): Certificate? = null

        override fun engineGetCreationDate(alias: String?): Date = Date()

        override fun engineGetCertificateChain(alias: String?): Array<Certificate>? = null

        override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) = Unit

        override fun engineGetCertificateAlias(cert: Certificate?): String? = null
    }

    class FakeAesKeyGenerator : KeyGeneratorSpi() {
        private val wrapped = KeyGenerator.getInstance("AES").apply { init(256) }

        override fun engineInit(random: SecureRandom?) = Unit

        override fun engineInit(params: AlgorithmParameterSpec?, random: SecureRandom?) = Unit

        override fun engineInit(keysize: Int, random: SecureRandom?) = Unit

        override fun engineGenerateKey(): SecretKey = wrapped.generateKey()
    }
}
