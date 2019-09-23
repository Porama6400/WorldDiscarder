package net.otlg.worlddiscarder;

import java.util.Random;

public class AuthManager {
    private final long timeValid = 10 * 60 * 1000; // 10 minutes
    private long expiry = System.currentTimeMillis();
    private Random random = new Random();
    private int passcode = -1;

    public boolean isSessionValid() {
        return System.currentTimeMillis() < expiry;
    }

    public void resetSession() {
        expiry = System.currentTimeMillis() + timeValid;
    }

    public String getCode() {
        passcode = random.nextInt(9000) + 1000;
        return "" + passcode;
    }

    public void resetCode(){
        passcode = -1;
    }

    public boolean checkCode(String code){
        int inputPasscode = Integer.parseInt(code);
        if(inputPasscode < 1000) return false;
        if(inputPasscode > 10000) return false;

        return inputPasscode == passcode;
    }
}
