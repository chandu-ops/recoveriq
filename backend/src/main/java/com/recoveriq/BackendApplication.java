package com.recoveriq;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) 
    {
        // Force a canonical timezone ID before any DB connection is opened.
        // Avoids JVM/OS-reported legacy aliases (e.g. "Asia/Calcutta") that
        // some Postgres tzdata builds don't recognize during the connection
        // handshake. Deliberately explicit rather than relying on VM args,
        // so this works identically on any machine (yours, a teammate's,
        // or a demo laptop) without extra setup.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(BackendApplication.class, args);
    }
}