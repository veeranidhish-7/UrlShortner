package com.portfolio.urlshortener.service;

import org.springframework.stereotype.Service;

@Service
public class Base62Service {
    
    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = BASE62_ALPHABET.length();

    public String encode(long value) {
        if (value == 0) {
            return String.valueOf(BASE62_ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(BASE62_ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        
        return sb.reverse().toString();
    }

    public long decode(String base62) {
        long value = 0;
        for (int i = 0; i < base62.length(); i++) {
            value = value * BASE + BASE62_ALPHABET.indexOf(base62.charAt(i));
        }
        return value;
    }
}
