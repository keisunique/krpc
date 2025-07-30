package com.mycz.krpc.stater.vault;

import org.springframework.core.env.PropertySource;

import java.util.Map;

public class VaultSource extends PropertySource<Map<String, String>> {

    public VaultSource(String name, Map<String, String> source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        return source.get(name);
    }
}
