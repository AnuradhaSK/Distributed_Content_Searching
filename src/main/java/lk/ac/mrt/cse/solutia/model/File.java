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
    String name;
    String content;
    String hash;
    ArrayList<String> fileNames;


    public void fileGenerate(){

        Scanner scanner = null;
        BufferedReader reader;
        try {
            fileNames= new ArrayList<String>();
            reader = new BufferedReader(new FileReader("/var/tmp/file_names"));
            String line = reader.readLine();
            while (line != null) {

                line = reader.readLine();
                fileNames.add(line);

            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        int number_of_files = getRandomNumberInRange(3,5);


        for (int i = 0; i < number_of_files; i++) {
            int file_index = getRandomNumberInRange(0,19);
            String file_name = fileNames.get(file_index);

            int file_size = file_name.length()%10;
            if (file_size<2){
                file_size= 2;
            }

            int FILE_SIZE = 1000 * 1000* file_size;
            String file_path = "/var/tmp/" + file_name ;

            java.io.File file_write = new java.io.File(file_path);


            try (BufferedWriter writer = Files.newBufferedWriter(file_write.toPath())) {
                while (file_write.length() < FILE_SIZE) {
                    writer.write(file_name);
                    writer.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Files are generated to /var/tmp folder");


    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
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
