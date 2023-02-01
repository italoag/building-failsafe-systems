import java.security.KeyPair;
import java.security.KeyPairGenerator;
import javax.crypto.Cipher;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.EnableCaching;

@Configuration
@EnableCaching
public class ConfiguracaoCache {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("dadosSensiveisCache");
    }
}

@Component
public class RSAEncryptionKeyGenerator implements KeyGenerator {
    private KeyPair keyPair;
    public RSAEncryptionKeyGenerator() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        this.keyPair = keyGen.genKeyPair();
    }

@Override
public Object generate(Object target, Method method, Object... params) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            byte[] encryptedBytes = cipher.doFinal(params[0].toString().getBytes());
            return new String(Base64.getEncoder().encode(encryptedBytes));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not generate key", e);
        }
    }
}

@Component
public class TokenizationKeyGenerator implements KeyGenerator {
    @Override
public Object generate(Object target, Method method, Object... params) {
        return UUID.randomUUID().toString();
    }
}

@Service
public class DadosSensiveisService {
    @Cacheable(value="dadosSensiveisCache", keyGenerator="RSAEncryptionKeyGenerator")
public String obterDadosSensiveis(String dados) {
        // lógica de obtenção de dados sensíveis
    return dados;
    }
}