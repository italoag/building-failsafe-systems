import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SensitiveDataService {

    @Cacheable(value = "sensitiveDataCache", keyGenerator = "RSAEncryptionKeyGenerator")
    public String getEncryptedSensitiveData() throws Exception {
        String sensitiveData = "dados sensível";
        return DataEncryption.encryptData(sensitiveData);
    }
}
