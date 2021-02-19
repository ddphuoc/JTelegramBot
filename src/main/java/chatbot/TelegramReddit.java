package chatbot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TelegramReddit {

    private static int collectTime = 700;
    private static Hashtable<Long, String> htbFriends;
    private static Hashtable<String, String> htbConfigs;
    private static TelegramBot bot;
    private static String friendListName = "friendList.txt";
    private static String confFile = "conf.properties";
    private static String pythonProgram = "";
    private static String pythonData = "";
    private static List<String> _lstMentions = new ArrayList<>();
    private static  List<String> _lstBests = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        htbConfigs = new Hashtable<>();
        htbFriends = new Hashtable<>();
        confFile = args.length > 0 ? args[0]: confFile;
        LoadConfigurations();
        LoadFriends();

        // Create your bot passing the token received from @BotFather
        bot = new TelegramBot("1654024366:AAGFFapMC0bp445FPqBbeT5oe7WtaUahFzg");

        // Register for updates
        bot.setUpdatesListener(updates -> {
            // ... process updates
            for(Update update: updates){
                // Send messages
                long chatId = update.message().chat().id();
                String msg = update.message().text();
                if(msg.toLowerCase().equals("/redstock")) {
                    if(!htbFriends.containsKey(chatId)) {
                        AddFriend(chatId, update.message().from().firstName());
                    }
                } else if(msg.toLowerCase().equals("/redlatest")) {
                    if(htbFriends.containsKey(chatId)) {
                        SendMessageStock(chatId);
                        SendMessageMention(chatId);
                    }
                }
            }
            // return id of last processed update or confirm them all
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, exception -> {
            System.out.println(exception.getMessage());
        });
        LoadBestStocks();
        LoadMentions();

        // Get Reddit stock everyday
        new Timer().schedule(new RedditTask(collectTime, pythonProgram, pythonData, (successful) -> {
            if(successful) {
                try {
                    LoadBestStocks();
                    LoadMentions();
                    SendMessageStock(-1);
                    SendMessageMention(-1);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }), 0, 1 * 59 * 1000);
        System.out.println("System is running...");
    }

    private static void LoadConfigurations(){
        try {
            Path path = Files.createTempFile(confFile, "");
            if(!Files.exists(path))
                return;
            // Load friend list
            BufferedReader bufferedReader = new BufferedReader(new FileReader(confFile));
            bufferedReader.lines().forEach(line -> {
                String [] w = line.split("=");
                if(w.length == 2) {
                    htbConfigs.put(w[0].trim(), w[1].trim());
                }
            });
            bufferedReader.close();
            if(htbConfigs.containsKey("friendListPath"))
                friendListName = htbConfigs.get("friendListPath");
            if(htbConfigs.containsKey("reddit.Schedule"))
                collectTime = Integer.valueOf(htbConfigs.get("reddit.Schedule"));
            if(htbConfigs.containsKey("reddit.python.program"))
                pythonProgram = htbConfigs.get("reddit.python.program");
            if(htbConfigs.containsKey("reddit.python.data"))
                pythonData = htbConfigs.get("reddit.python.data");
        } catch (Exception exception){

        }
    }

    private static void LoadFriends(){
        try {
            Path path = Files.createTempFile(friendListName, "");
            if(!Files.exists(path))
                return;
            // Load friend list
            BufferedReader bufferedReader = new BufferedReader(new FileReader(friendListName));
            bufferedReader.lines().forEach(line -> {
                String [] w = line.split("\t");
                if(w.length == 2) {
                    htbFriends.put(Long.valueOf(w[0].trim()), w[1].trim());
                }
            });
            bufferedReader.close();
        } catch (Exception exception){

        }
    }

    private static void AddFriend(long chatId, String userName) {
        try {
            if(!htbFriends.containsKey(chatId)) {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(friendListName, true));
                bufferedWriter.write(String.format("%s\t%s", chatId, userName));
                bufferedWriter.newLine();
                bufferedWriter.flush();
                bufferedWriter.close();
                htbFriends.put (chatId, userName);
                SendMessageStock(chatId);
                SendMessageMention(chatId);
                SendMessage(chatId, "Welcome " + userName + ", your id has been added !");
                SendMessage(chatId, "I will please to send you latest news today when you request: /redlatest");
                SendMessage(chatId, "Good luck !");
                System.out.println("Added new friend: " + userName);
            }
        } catch (Exception exception) {
            System.out.println("Add friend error: " + exception.getMessage());
        }
    }

    private static void SendMessage(long chatId, String message){
        try {
            SendResponse response = bot.execute(new SendMessage(chatId, message));
        } catch (Exception exception) {

        }
    }

    private static void SendMessageMention(long chatId) {
        if (_lstMentions.size() <= 0)
            return;
        String msg = "";
        for (String stock:_lstMentions)
        {
            msg += stock + "\n";
        }
        for(long id:htbFriends.keySet()) {
            if(chatId > 0 && id != chatId)
                continue;
            SendMessage(id, "Mention list");
            SendMessage(id, msg);
            SendMessage(id, "===== mention end =====");
        }
    }

    private static void SendMessageStock(long chatId) {
        if (_lstBests.size() <= 0)
            return;
        String msg = "";
        for (String stock:_lstBests)
        {
            msg += stock + "\n";
        }
        for(long id:htbFriends.keySet()) {
            if(chatId > 0 && id != chatId)
                continue;
            SendMessage(id, "Best stock list");
            SendMessage(id, msg);
            SendMessage(id, "===== stock end =====");
        }
    }

    // Stop send mentions list
    private static void LoadMentions() throws IOException {
//        Date date = Calendar.getInstance().getTime();
//        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        String file = dateFormat.format(date) + "_tick_df.csv";
//        Path path = Paths.get(pythonData, file);
//
//        if(!Files.exists(path))
//            return;
//
//        _lstMentions.clear();
//        // Load friend list
//        BufferedReader bufferedReader = new BufferedReader(new FileReader(path.toFile().getPath()));
//        bufferedReader.lines().forEach(line -> {
//            if(line.trim().length() > 0) {
//                _lstMentions.add(line);
//            }
//        });
//        bufferedReader.close();
    }

    private static void LoadBestStocks() throws IOException {
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String file = dateFormat.format(date) + "_df_best_25.csv";
        Path path = Paths.get(pythonData, file);

        if(!Files.exists(path))
            return;

        _lstBests.clear();
        // Load friend list
        BufferedReader bufferedReader = new BufferedReader(new FileReader(path.toFile().getPath()));
        bufferedReader.lines().forEach(line -> {
            if(line.trim().length() > 0) {
                _lstBests.add(line);
            }
        });
        bufferedReader.close();
    }
}
