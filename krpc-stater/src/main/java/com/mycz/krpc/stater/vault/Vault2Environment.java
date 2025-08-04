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

        System.out.println("*** Vault初始化 - 开始");

        String vaultUrl = environment.getProperty("krpc.vault.url");
        String vaultToken = environment.getProperty("krpc.vault.token");
        String[] secretPaths = getSecretPaths(environment);

        if (!StringUtils.hasText(vaultUrl) || !StringUtils.hasText(vaultToken)) {
            System.out.println("*** Vault初始化 - url或token缺失");
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
                System.out.printf("*** Vault初始化 - 加载路径%s%n", secretPath);
                try {
                    LogicalResponse response = vault.logical().read(secretPath);
                    if (response != null && response.getData() != null) {
                        Map<String, String> rawData = response.getData();
                        System.out.printf("*** Vault初始化 - 加载路径%s, %n", secretPath);
                        String pathPrefix = extractPathPrefix(secretPath);
                        rawData.forEach((key, value) -> {
                            String finalKey = StringUtils.hasText(pathPrefix) ? 
                                "vault." + pathPrefix + "." + key : "vault." + key;
                            allProperties.put(finalKey, value);
                        });
                        System.out.printf("*** Vault初始化 - 加载路径%s, 数量:%s %n", secretPath, rawData.size());
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            if (!allProperties.isEmpty()) {
                VaultSource vaultPropertySource = new VaultSource("vault-properties", allProperties);
                environment.getPropertySources().addFirst(vaultPropertySource);
                System.out.printf("*** Vault初始化完成 - 加载配置完成,数量%s%n", allProperties.size());
            }
            System.out.println("*** Vault初始化 - 完成");
        } catch (Exception e) {
            System.out.println("*** Vault初始化 - 异常");
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
