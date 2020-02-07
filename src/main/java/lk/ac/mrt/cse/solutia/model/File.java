package lk.ac.mrt.cse.solutia.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class File {
    private String name;
    private String content;
    private String hash;

    public String getContent() {
        return content;
    }

    public String getHash() {
        return hash;
    }

    public String getName() {
        return name;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String generateHash(String filename){
        String hashValue = "";
        try
            {

                hashValue = toHexString(getSHA(filename));
            }
            catch (NoSuchAlgorithmException e) {
                System.out.println("Exception thrown for incorrect algorithm: " + e);
            }
        return hashValue;
    }
    public static byte[] getSHA(String input) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String toHexString(byte[] hash)
    {
        BigInteger number = new BigInteger(1, hash);

        StringBuilder hexString = new StringBuilder(number.toString(16));

        while (hexString.length() < 32)
        {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }

}
