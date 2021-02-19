package chatbot;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;
import java.util.function.Consumer;

public class RedditTask extends TimerTask {

    int scheduleTime = 700;
    String pythonSource = "";
    String pythonData = "";
    private Consumer<Boolean> consumer;

    public  RedditTask() {

    }

    public RedditTask(int scheduleTime, String pythonSource, String pythonData, Consumer<Boolean> consumer) {
        this.scheduleTime = scheduleTime;
        this.pythonSource = pythonSource;
        this.pythonData = pythonData;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("HHmm");
        int checkTime = Integer.parseInt(dateFormat.format(date));
        if(checkTime != scheduleTime) {
            consumer.accept(false);
            return;
        }
        System.out.println("Start Reddit at: "
                + LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledExecutionTime()),
                ZoneId.systemDefault()));
        try {
            RedditCall();
            consumer.accept(true);
        } catch (IOException | InterruptedException ioException) {
            System.out.println(ioException.getMessage());
            consumer.accept(false);
        }
        System.out.println("Complete Reddit at: "
                + LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledExecutionTime()),
                ZoneId.systemDefault()));
    }

    /// <summary>
    /// Running Scripts
    /// Go to src/ directory.
    /// Create a praw.ini file with the following
    /// [ClientSecrets]
    /// client_id =< your client id>
    /// client_secret=<your client secret>
    /// user_agent=<your user agent>
    /// Note that the title of this section, ClientSecrets, is important because ticker_counts.py will specifically look for that title in the praw.ini file.
    /// Install required modules using pip install -r requirements.txt
    /// Run ticker_counts.py first
    /// Now run yfinance_analysis.py
    /// You will be able to find your results in data/ directory.
    /// </summary>
    /// <returns></returns>
    private void RedditCall() throws IOException, InterruptedException {
        FileUtils.cleanDirectory(new File(pythonData));

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("/bin/bash", "-c", "python3 ticker_counts.py");
        processBuilder.directory(new File(pythonSource));
        Process process = processBuilder.start();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        bufferedReader.lines().forEach(System.out::println);
        int exitCode = process.waitFor();

        processBuilder = new ProcessBuilder();
        processBuilder.command("/bin/bash", "-c", "python3 yfinance_analysis.py");
        processBuilder.directory(new File(pythonSource));
        process = processBuilder.start();
        bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        bufferedReader.lines().forEach(System.out::println);
        exitCode = process.waitFor();
    }
}
