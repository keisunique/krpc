package com.mycz.krpc.stater.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.response.LogicalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Vault2Environment implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Boolean vaultEnabled = environment.getProperty("krpc.vault.enable", Boolean.class, false);
        if (!vaultEnabled) {
            return;
        }

        String vaultUrl = environment.getProperty("krpc.vault.url");
        String vaultToken = environment.getProperty("krpc.vault.token");
        String[] secretPaths = getSecretPaths(environment);

        if (!StringUtils.hasText(vaultUrl) || !StringUtils.hasText(vaultToken)) {
            return;
        }

        try {
            VaultConfig config = new VaultConfig()
                    .address(vaultUrl)
                    .token(vaultToken)
                    .engineVersion(2)
                    .build();

            Vault vault = new Vault(config);
            Map<String, String> allProperties = new HashMap<>();

            for (String secretPath : secretPaths) {
                try {
                    LogicalResponse response = vault.logical().read(secretPath);
                    if (response != null && response.getData() != null) {
                        Map<String, String> rawData = response.getData();
                        String pathPrefix = extractPathPrefix(secretPath);
                        rawData.forEach((key, value) -> {
                            String finalKey = StringUtils.hasText(pathPrefix) ? 
                                "vault." + pathPrefix + "." + key : "vault." + key;
                            allProperties.put(finalKey, value);
                        });
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            if (!allProperties.isEmpty()) {
                VaultSource vaultPropertySource = new VaultSource("vault-properties", allProperties);
                environment.getPropertySources().addFirst(vaultPropertySource);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] getSecretPaths(ConfigurableEnvironment environment) {
        // 读取数组配置的正确方式
        String secretPathsStr = environment.getProperty("krpc.vault.secret-path");
        if (StringUtils.hasText(secretPathsStr)) {
            // 如果是单个字符串，按逗号分割
            String[] paths = secretPathsStr.split(",");
            for (int i = 0; i < paths.length; i++) {
                paths[i] = paths[i].trim();
            }
            return paths;
        } else {
            // 尝试读取数组形式 krpc.vault.secret-path[0], krpc.vault.secret-path[1] etc.
            java.util.List<String> pathList = new java.util.ArrayList<>();
            int index = 0;
            String path;
            while ((path = environment.getProperty("krpc.vault.secret-path[" + index + "]")) != null) {
                pathList.add(path);
                index++;
            }
            if (!pathList.isEmpty()) {
                return pathList.toArray(new String[0]);
            } else {
                return new String[]{"secret/data/krpc"}; // 默认值
            }
        }
    }

    private String extractPathPrefix(String secretPath) {
        if (!StringUtils.hasText(secretPath)) {
            return "";
        }
        
        // 处理路径格式，如 secret/data/mysql -> mysql, secret/mysql -> mysql
        String[] parts = secretPath.split("/");
        if (parts.length > 1) {
            // 获取最后一个部分作为前缀
            return parts[parts.length - 1];
        }
        
        return secretPath;
    }
}
