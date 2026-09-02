package com.basilcode.emsbackend.common.exception;


public class OtpException {

    public static class Invalid extends RuntimeException {
        public Invalid(String message) { super(message); }
    }

    public static class Expired extends RuntimeException {
        public Expired(String message) { super(message); }
    }

    public static class Locked extends RuntimeException {
        public Locked(String message) { super(message); }
    }

    public static class Cooldown extends RuntimeException {
        public Cooldown(String message) { super(message); }
    }
}