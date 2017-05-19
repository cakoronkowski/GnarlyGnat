package data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stephen on 12/3/16.
 */
public class ClientConfig {
    public ClientConfig(
            boolean botmode,
            int sleep,
            int port,
            ClientInstanceType t
    ){
        inBotMode = botmode;
        botSleepInSeconds = sleep;
        botSleepInMillis = botSleepInSeconds * 1000;
        preferredPort = port;
        type = t;
    }
    public ClientConfig(String[] args) throws IllegalArgumentException {

        inBotMode = false;
        type = ClientInstanceType.PEER;


        if(args.length == 0)
            return;


        int bot = -1, port = -1, t = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i].toLowerCase().equals("--botmode")) {
                bot = i;
            } else if (args[i].equals("--type")) {
                t = i;
            } else if (args[i].equals("--port")) {
                port = i;
            }
        }

        if(bot == -1 && port == -1 && t == -1)
            return;

        if (bot != -1 && bot + 1 < args.length) {
            inBotMode = true;
            try {

                botSleepInSeconds = Integer.parseInt(args[bot + 1]);
                botSleepInMillis = botSleepInSeconds * 1000;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("invalid sleep int '" + args[bot + 1] + "'");
            }
        }

        if (port != -1 && port + 1 < args.length) {
            try {

                preferredPort = Integer.valueOf(args[port + 1]);

            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("invalid sleep int '" + args[bot + 1] + "'");
            }
        } else if(port != -1) {
            throw new IllegalArgumentException("port not specified");
        }


        if (t != -1 && t + 1 < args.length) {
            hasType = true;
            String typeString = args[t + 1].toLowerCase();
            if (typeString.equals("t")) {
                type = ClientInstanceType.TRACKER;
            } else if (typeString.equals("r")) {
                type = ClientInstanceType.REPOSITORY;
            } else if (typeString.equals("p")) {
                type = ClientInstanceType.PEER;
            } else {
                throw new IllegalArgumentException("Invalid instance type '" + args[t + 1] + "'");
            }
        } else if(t != -1 && t + 1 > args.length){
            throw new IllegalArgumentException("no input string");
        }

    }

    private boolean inBotMode;
    private int botSleepInSeconds;
    private int botSleepInMillis;
    private int preferredPort;
    private ClientInstanceType type;
    private boolean hasType;


    public boolean isInBotMode() {
        return inBotMode;
    }

    public int getBotSleepInSeconds() {
        return botSleepInSeconds;
    }

    public int getBotSleepInMillis() {
        return botSleepInMillis;
    }

    public int getPreferredPort() {
        return preferredPort;
    }

    public ClientInstanceType getType() {
        return type;
    }

    public boolean hasPreferredPort(){
        return preferredPort > 0;
    }

    public boolean hasType(){return hasType;}


    public enum ClientInstanceType {
        PEER,
        TRACKER,
        REPOSITORY;
        public static ClientInstanceType getTypeFromString(String s){
            if(s.toLowerCase().equals("peer"))
                return PEER;
            if(s.toLowerCase().equals("tracker"))
                return TRACKER;
            if(s.toLowerCase().equals("repository"))
                return REPOSITORY;
            return PEER;
        }
    }
}
