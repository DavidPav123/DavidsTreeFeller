package com.github.davidpav123.davidstreefeller;

import java.io.*;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;

public class Configuration extends File implements Cloneable {
    @Serial
    private static final long serialVersionUID = 115L;

    private final String header;
    private final HashMap<String, String> hm;
    private final HashMap<String, String> info;

    public Configuration(String file, String header) {
        super(file);
        hm = new HashMap<>();
        info = new HashMap<>();
        this.header = header;
    }

    public void setValue(String key, Object value) {
        hm.put(key, value.toString());
    }

    public String getString(String key, String defaultValue) {
        return hm.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String str = hm.getOrDefault(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            System.err.println("Error trying to get integer value from config file");
            System.err.println("(Value \"" + str + "\" could not be parsed to integer)");
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String str = hm.getOrDefault(key, String.valueOf(defaultValue));
        switch (str) {
            case "true", "yes" -> {
                return true;
            }
            case "false", "no" -> {
                return false;
            }
            default -> {
                System.err.println("Error trying to get boolean value from config file");
                System.err.println("(Value \"" + str + "\" could not be parsed to boolean)");
                return defaultValue;
            }
        }
    }

    public void setInfo(String key, String info) {
        this.info.put(key, info);
    }

    public void saveConfig() throws IOException {
        StringBuilder configTxt = new StringBuilder(header == null ? "" : "#\t" + header + "\n\n");
        Set<String> keys = hm.keySet();
        for (String key : keys) {
            String value = hm.get(key);
            String info = this.info.get(key);
            if (info != null) {
                configTxt.append("#").append(info).append("\n");
            }
            configTxt.append(key).append(": ").append(value).append("\n\n");
        }

        if (exists()) {
            delete();
        }
        try {
            getParentFile().mkdirs();
        } catch (NullPointerException ignored) {
        }
        createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(this));
        writer.write(configTxt.toString());
        writer.close();
    }

    public void reloadConfig() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this));
            String line;
            int cont = 0;
            while ((line = reader.readLine()) != null) {
                cont++;
                line = line.trim();
                if (!line.startsWith("#") && !line.trim().isEmpty()) {
                    StringTokenizer st = new StringTokenizer(line, ":");
                    if (st.countTokens() != 2) {
                        reader.close();
                        throw new IOException("Looks like the file content is not correct. Broken line " + cont + " (" + st.countTokens() + " tokens, should be 2)");
                    }
                    String key = st.nextToken().trim();
                    String value = st.nextToken().trim();
                    setValue(key, value);
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Configuration file not created yet. Skipping load.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
