package backend.pineapple_ecommerce.common.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security")
public class TrustedProxyProperties {

    private List<String> trustedProxies = List.of(
            "127.0.0.1",
            "::1",
            "0:0:0:0:0:0:0:1"
    );

    private boolean trustAllProxies = false;

    @PostConstruct
    public void validate() {
        if (trustedProxies != null) {
            for (String cidr : trustedProxies) {
                new IpAddressMatcher(cidr); // will throw if config is wrong -> fail fast
            }
        }
    }
}
